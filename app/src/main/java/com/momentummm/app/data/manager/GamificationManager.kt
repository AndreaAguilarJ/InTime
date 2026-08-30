package com.momentummm.app.data.manager

import android.content.Context
import com.momentummm.app.R
import com.momentummm.app.data.dao.UserDao
import com.momentummm.app.data.entity.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GamificationManager - Maneja toda la lógica de gamificación para retención.
 * Implementa un sistema de niveles, XP, TimeCoins y rachas.
 * Integrado con SmartNotificationManager para notificaciones dinámicas.
 */
@Singleton
class GamificationManager @Inject constructor(
    private val userDao: UserDao,
    // Depende del gestor de protección, no de SmartBlockingManager, para no
    // crear un ciclo de inyección: aquél sólo necesita el DAO de configuración.
    private val streakProtectionManager: StreakProtectionManager,
    @ApplicationContext private val context: Context
) {
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("GamificationManager", "Coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    
    // Referencia al NotificationManager (lazy para evitar ciclos)
    private var notificationManager: SmartNotificationManager? = null
    
    fun setNotificationManager(manager: SmartNotificationManager) {
        this.notificationManager = manager
    }
    
    init {
        // Asegurar que existan UserSettings al iniciar
        scope.launch {
            try {
                ensureUserSettingsExist()
            } catch (e: Exception) {
                android.util.Log.e("GamificationManager", "Error initializing user settings", e)
            }
        }
    }
    
    /**
     * Crea UserSettings por defecto si no existen
     */
    private suspend fun ensureUserSettingsExist() {
        val existing = userDao.getUserSettingsSync()
        if (existing == null) {
            userDao.insertUserSettings(UserSettings(
                id = 1,
                birthDate = null,
                isOnboardingCompleted = false,
                hasSeenTutorial = false
            ))
        }
    }
    
    /**
     * Data class para eventos de gamificación (subir de nivel, etc.)
     */
    data class GamificationEvent(
        val type: EventType,
        val xpGained: Int = 0,
        val coinsGained: Int = 0,
        val newLevel: Int? = null,
        val streakDays: Int = 0,
        val message: String = ""
    )

    enum class EventType {
        XP_GAINED,
        LEVEL_UP,
        STREAK_CONTINUED,
        STREAK_BROKEN,
        PERFECT_DAY,
        SESSION_COMPLETED,
        ACHIEVEMENT_UNLOCKED
    }

    /**
     * Obtiene el estado de gamificación actual como Flow
     */
    fun getGamificationState(): Flow<GamificationState?> {
        return userDao.getUserSettings().map { settings ->
            settings?.let {
                GamificationState(
                    level = it.userLevel,
                    currentXp = it.currentXp,
                    totalXp = it.totalXp,
                    xpForNextLevel = UserSettings.getXpForLevel(it.userLevel),
                    xpToNextLevel = it.getXpToNextLevel().coerceAtLeast(0),
                    xpProgress = it.getLevelProgress(),
                    levelTitle = context.getString(it.getLevelTitleRes(), it.userLevel),
                    levelEmoji = it.getLevelEmoji(),
                    timeCoins = it.timeCoins,
                    currentStreak = it.currentStreak,
                    longestStreak = it.longestStreak,
                    totalFocusMinutes = it.totalFocusMinutes,
                    totalSessions = it.totalSessionsCompleted,
                    perfectDays = it.perfectDaysCount,
                    streakMultiplier = UserSettings.getStreakMultiplier(it.currentStreak)
                )
            }
        }
    }

    /**
     * Otorga XP por minutos de foco completados
     */
    suspend fun awardFocusMinuteXp(minutes: Int): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.XP_GAINED)
        
        val streakMultiplier = UserSettings.getStreakMultiplier(settings.currentStreak)
        val baseXp = minutes * UserSettings.XP_PER_FOCUS_MINUTE
        val baseCoins = minutes * UserSettings.COINS_PER_FOCUS_MINUTE
        
        val totalXp = (baseXp * streakMultiplier).toInt()
        val totalCoins = (baseCoins * streakMultiplier).toInt()
        
        userDao.addXp(totalXp)
        userDao.addTimeCoins(totalCoins)
        userDao.addFocusMinutes(minutes)
        
        android.util.Log.d("GamificationManager", "Otorgando XP - minutos: $minutes, baseXP: $baseXp, multiplicador: $streakMultiplier, totalXP: $totalXp, totalCoins: $totalCoins")
        
        // Verificar si subió de nivel
        val levelUpEvent = checkAndProcessLevelUp()
        
        return levelUpEvent ?: GamificationEvent(
            type = EventType.XP_GAINED,
            xpGained = totalXp,
            coinsGained = totalCoins,
            message = "+$totalXp XP | +$totalCoins 🪙"
        )
    }

    /**
     * Otorga bonus por completar una sesión de foco
     */
    suspend fun awardSessionCompletionBonus(): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.SESSION_COMPLETED)
        
        val streakMultiplier = UserSettings.getStreakMultiplier(settings.currentStreak)
        val xpBonus = (UserSettings.XP_SESSION_BONUS * streakMultiplier).toInt()
        val coinsBonus = (UserSettings.COINS_SESSION_BONUS * streakMultiplier).toInt()
        
        userDao.addXp(xpBonus)
        userDao.addTimeCoins(coinsBonus)
        userDao.incrementSessionsCompleted()
        
        // Verificar y actualizar racha diaria
        updateDailyStreak()
        
        // Verificar si subió de nivel
        val levelUpEvent = checkAndProcessLevelUp()
        
        return levelUpEvent ?: GamificationEvent(
            type = EventType.SESSION_COMPLETED,
            xpGained = xpBonus,
            coinsGained = coinsBonus,
            message = context.getString(R.string.gam_ev_session_done, xpBonus)
        )
    }

    /**
     * Verifica y actualiza la racha diaria
     */
    suspend fun updateDailyStreak(): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.STREAK_CONTINUED)
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val lastActive = settings.lastActiveDate
        
        if (lastActive == null) {
            // Primera vez usando la app
            val rows = userDao.incrementStreak(today)
            if (rows > 0) {
                // Racha real tras escribir (día 1). Se lee de la BD, no se asume.
                notifyStreakMilestoneIfNeeded()
                // XP por iniciar la racha → la barra de progreso deja de estar en 0.
                userDao.addXp(UserSettings.XP_PER_STREAK_DAY)
                userDao.addTimeCoins(UserSettings.COINS_PER_STREAK_DAY)
                checkAndProcessLevelUp()?.let { return it }
            }
            return GamificationEvent(
                type = EventType.STREAK_CONTINUED,
                xpGained = if (rows > 0) UserSettings.XP_PER_STREAK_DAY else 0,
                coinsGained = if (rows > 0) UserSettings.COINS_PER_STREAK_DAY else 0,
                streakDays = 1,
                message = context.getString(R.string.gam_ev_streak_started)
            )
        }
        
        val lastActiveCalendar = Calendar.getInstance().apply { 
            time = lastActive
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val daysDifference = ((todayCalendar.timeInMillis - lastActiveCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            daysDifference == 0 -> {
                // Mismo día, no hacer nada
                GamificationEvent(
                    type = EventType.STREAK_CONTINUED,
                    streakDays = settings.currentStreak,
                    message = context.getString(R.string.gam_ev_streak_current, settings.currentStreak)
                )
            }
            daysDifference == 1 -> {
                // Día consecutivo - incrementar racha
                val rows = userDao.incrementStreak(today)
                // Se relee de la BD en vez de asumir currentStreak+1, porque
                // updateDailyStreak se llama desde dos sitios y el cálculo
                // optimista podía desincronizarse.
                val newStreak = userDao.getCurrentStreak() ?: (settings.currentStreak + 1)
                if (rows > 0) {
                    notifyStreakMilestoneIfNeeded()
                    // XP por mantener la racha, escalado por el MISMO multiplicador
                    // que la tarjeta muestra (x1.25, x1.5…). Así la barra avanza
                    // con el uso diario real y el multiplicador visible sirve
                    // para algo.
                    val multiplier = UserSettings.getStreakMultiplier(newStreak)
                    val xpGain = (UserSettings.XP_PER_STREAK_DAY * multiplier).toInt()
                    val coinGain = (UserSettings.COINS_PER_STREAK_DAY * multiplier).toInt()
                    userDao.addXp(xpGain)
                    userDao.addTimeCoins(coinGain)
                    // Ganar XP puede subir de nivel: se procesa y, si ocurre, se
                    // devuelve ese evento (que también anima la barra al reiniciarse).
                    checkAndProcessLevelUp()?.let { return it }
                    return GamificationEvent(
                        type = EventType.STREAK_CONTINUED,
                        xpGained = xpGain,
                        coinsGained = coinGain,
                        streakDays = newStreak,
                        message = context.getString(R.string.gam_ev_streak_days, newStreak)
                    )
                }
                GamificationEvent(
                    type = EventType.STREAK_CONTINUED,
                    streakDays = newStreak,
                    message = context.getString(R.string.gam_ev_streak_days, newStreak)
                )
            }
            else -> {
                // Racha rota - penalización
                breakStreak()
            }
        }
    }

    /**
     * Emite la notificación de hito de racha con el número REAL de días, si el
     * día alcanzado es un hito y no es spam.
     *
     * Lee la racha de la base DESPUÉS de incrementarla (no asume el valor) y
     * usa `longestStreak` para saber si es un récord nuevo. El gate de "qué días
     * notificar" evita avisar cada día.
     */
    private suspend fun notifyStreakMilestoneIfNeeded() {
        val manager = notificationManager ?: return
        val settings = userDao.getUserSettingsSync() ?: return
        val streak = settings.currentStreak
        if (streak < 1) return
        if (!isStreakMilestone(streak)) return
        // Récord: currentStreak == longestStreak solo cuando esta racha igualó o
        // superó la mejor histórica (incrementStreak sube longestStreak a la par).
        val isNewRecord = streak >= settings.longestStreak && streak > 1
        manager.showStreakMilestoneNotification(streak, isNewRecord)
    }

    /**
     * Qué días de racha merecen una notificación. Días 1, 3 y 7, luego cada
     * semana cumplida. Notificar cada día sería spam.
     *
     * Delega en el companion para poder probar la lógica sin construir el
     * manager (que necesita DAO, contexto, etc.).
     */
    fun isStreakMilestone(day: Int): Boolean = Companion.isStreakMilestone(day)

    /**
     * Rompe la racha y aplica penalización (Loss Aversion)
     *
     * ANTES DE ROMPERLA se intenta consumir un día de gracia. Este es el punto
     * que faltaba: la Protección de rachas guardaba un cupo semanal que nadie
     * consultaba nunca, así que la promesa «días de gracia para que no pierdas
     * tu racha» no correspondía a ningún flujo implementado.
     */
    suspend fun breakStreak(): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.STREAK_BROKEN)
        val previousStreak = settings.currentStreak

        if (previousStreak > 0) {
            // Rescate por día de gracia.
            val outcome = try {
                streakProtectionManager.tryConsumeGraceDay()
            } catch (e: Exception) {
                android.util.Log.e("GamificationManager", "Error al consumir día de gracia", e)
                StreakProtectionManager.Outcome.NoGraceLeft
            }

            if (outcome is StreakProtectionManager.Outcome.GraceUsed) {
                // La racha se conserva: no sube (el usuario faltó) pero tampoco
                // se pierde, y no hay penalización de XP.
                userDao.preserveStreak(Date())
                notificationManager?.showGraceDayUsedNotification(outcome.remainingAfter)

                return GamificationEvent(
                    type = EventType.STREAK_CONTINUED,
                    streakDays = previousStreak,
                    message = context.getString(
                        R.string.gam_ev_grace_day_used,
                        previousStreak,
                        outcome.remainingAfter
                    )
                )
            }

            // Aplicar penalización de XP
            userDao.subtractXp(UserSettings.XP_STREAK_BREAK_PENALTY)
            userDao.resetStreak(Date())
            
            // Enviar notificación de racha rota
            notificationManager?.showStreakBrokenNotification(previousStreak)
            
            return GamificationEvent(
                type = EventType.STREAK_BROKEN,
                xpGained = -UserSettings.XP_STREAK_BREAK_PENALTY,
                streakDays = 0,
                message = context.getString(R.string.gam_ev_streak_lost, previousStreak, UserSettings.XP_STREAK_BREAK_PENALTY)
            )
        }

        // Sin racha que romper no hay penalización ni aviso, pero la fecha SÍ se
        // actualiza. Antes no se tocaba, y eso dejaba a `lastActiveDate` clavada
        // en el pasado: `updateDailyStreak()` volvía a calcular una diferencia
        // mayor que un día, entraba otra vez por aquí y no cambiaba nada. Quien
        // rompía su racha y tardaba en volver no podía empezar una nueva nunca.
        userDao.resetStreak(Date())

        return GamificationEvent(
            type = EventType.STREAK_BROKEN,
            streakDays = 0,
            message = context.getString(R.string.gam_ev_streak_restart)
        )
    }

    /**
     * Otorga bonus por día perfecto (sin romper límites)
     */
    suspend fun awardPerfectDayBonus(): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.PERFECT_DAY)
        
        val streakMultiplier = UserSettings.getStreakMultiplier(settings.currentStreak)
        val xpBonus = (UserSettings.XP_PERFECT_DAY_BONUS * streakMultiplier).toInt()
        val coinsBonus = (UserSettings.COINS_PERFECT_DAY_BONUS * streakMultiplier).toInt()
        
        userDao.addXp(xpBonus)
        userDao.addTimeCoins(coinsBonus)
        userDao.incrementPerfectDays()
        
        // Enviar notificación de día perfecto con el XP real otorgado
        notificationManager?.showPerfectDayNotification(xpBonus)
        
        // Verificar si subió de nivel
        val levelUpEvent = checkAndProcessLevelUp()
        
        return levelUpEvent ?: GamificationEvent(
            type = EventType.PERFECT_DAY,
            xpGained = xpBonus,
            coinsGained = coinsBonus,
            message = context.getString(R.string.gam_ev_perfect_day, xpBonus)
        )
    }

    /**
     * Verifica si el usuario subió de nivel y procesa
     */
    private suspend fun checkAndProcessLevelUp(): GamificationEvent? {
        val settings = userDao.getUserSettingsSync() ?: return null
        val currentLevel = settings.userLevel
        val currentXp = settings.currentXp
        val xpForNextLevel = UserSettings.getXpForLevel(currentLevel)
        
        if (currentXp >= xpForNextLevel) {
            val newLevel = currentLevel + 1
            userDao.updateLevel(newLevel)
            
            // Bonus de TimeCoins por subir de nivel
            val levelUpCoins = newLevel * 50
            userDao.addTimeCoins(levelUpCoins)
            
            // Obtener título del nivel y enviar notificación
            val updatedSettings = userDao.getUserSettingsSync()
            val levelTitle = updatedSettings
                ?.let { context.getString(it.getLevelTitleRes(), it.userLevel) }
                ?: context.getString(R.string.level_title_grandmaster, newLevel)
            notificationManager?.showLevelUpNotification(newLevel, levelTitle, levelUpCoins)
            
            return GamificationEvent(
                type = EventType.LEVEL_UP,
                newLevel = newLevel,
                coinsGained = levelUpCoins,
                message = context.getString(R.string.gam_ev_level_up, newLevel)
            )
        }
        
        return null
    }

    /**
     * Gasta TimeCoins (para desbloqueos, temas, etc.)
     */
    suspend fun spendCoins(amount: Int): Boolean {
        val currentCoins = userDao.getTimeCoins() ?: 0
        return if (currentCoins >= amount) {
            userDao.spendTimeCoins(amount)
            true
        } else {
            false
        }
    }

    /**
     * Obtiene la racha actual
     */
    suspend fun getCurrentStreak(): Int {
        return userDao.getCurrentStreak() ?: 0
    }

    /**
     * Obtiene el nivel actual
     */
    suspend fun getCurrentLevel(): Int {
        return userDao.getUserLevel() ?: 1
    }

    /**
     * Obtiene los TimeCoins actuales
     */
    suspend fun getTimeCoins(): Int {
        return userDao.getTimeCoins() ?: 0
    }
    
    /**
     * Resetea todo el progreso de gamificación
     */
    suspend fun resetProgress() {
        userDao.resetGamificationProgress()
    }
    
    /**
     * Restaura el progreso de gamificación desde la nube.
     *
     * BUG CORREGIDO: el botón "Restaurar desde la nube" de los ajustes hacía
     * exactamente esto:
     *
     *     isSyncing = true
     *     // TODO: Implementar restauración desde Appwrite
     *     delay(2000)
     *     isSyncing = false
     *
     * Es decir: mostraba un indicador de progreso durante dos segundos y lo
     * quitaba, imitando una restauración correcta. El usuario veía la animación
     * de éxito y su progreso seguía exactamente igual. Peor que no hacer nada,
     * porque parecía haber funcionado.
     *
     * @return true si se restauró algo.
     */
    suspend fun restoreFromCloud(
        appwriteUserRepository: com.momentummm.app.data.appwrite.repository.AppwriteUserRepository,
        userId: String?
    ): RestoreResult {
        if (userId.isNullOrBlank()) {
            return RestoreResult.NotLoggedIn
        }

        val result = appwriteUserRepository.getGamificationData(userId)
        val cloud = result.getOrElse {
            android.util.Log.e("GamificationManager", "Error restaurando gamificación", it)
            return RestoreResult.Error(it.message)
        } ?: return RestoreResult.NothingStored

        val current = userDao.getUserSettingsSync()
            ?: return RestoreResult.Error("No hay ajustes locales que actualizar")

        val lastActive = cloud.lastActiveDate?.let { iso ->
            runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    .parse(iso)
            }.getOrNull()
        } ?: current.lastActiveDate

        userDao.updateUserSettings(
            current.copy(
                userLevel = cloud.userLevel,
                currentXp = cloud.currentXp,
                totalXp = cloud.totalXp,
                timeCoins = cloud.timeCoins,
                currentStreak = cloud.currentStreak,
                longestStreak = cloud.longestStreak,
                lastActiveDate = lastActive,
                totalFocusMinutes = cloud.totalFocusMinutes,
                totalSessionsCompleted = cloud.totalSessionsCompleted,
                perfectDaysCount = cloud.perfectDaysCount,
                gamificationEnabled = cloud.gamificationEnabled,
                showXpNotifications = cloud.showXpNotifications,
                showStreakReminders = cloud.showStreakReminders
            )
        )
        return RestoreResult.Restored(cloud.userLevel, cloud.totalXp)
    }

    sealed interface RestoreResult {
        data class Restored(val level: Int, val totalXp: Int) : RestoreResult
        data object NothingStored : RestoreResult
        data object NotLoggedIn : RestoreResult
        data class Error(val message: String?) : RestoreResult
    }

    /**
     * Sincroniza los datos de gamificación a la nube.
     *
     * BUG CORREGIDO: el id de usuario estaba fijado a cadena vacía con un
     * `// TODO`. La llamada se hacía y parecía funcionar, pero escribía contra
     * un usuario inexistente: el progreso (nivel, XP, racha) nunca llegaba a la
     * cuenta del usuario, así que "sincronizar" no sincronizaba nada y al
     * reinstalar se perdía todo.
     *
     * @return true si se pudo sincronizar. false si no hay sesión: el
     *   llamador debe decirle al usuario que inicie sesión en lugar de dar por
     *   hecho que el guardado tuvo éxito.
     */
    suspend fun syncToCloud(
        appwriteUserRepository: com.momentummm.app.data.appwrite.repository.AppwriteUserRepository,
        userId: String?
    ): Boolean {
        val data = getGamificationDataForSync() ?: return false

        if (userId.isNullOrBlank()) {
            android.util.Log.w(
                "GamificationManager",
                "Sin sesión de usuario: no se puede sincronizar la gamificación"
            )
            return false
        }
        
        // Llamar al repositorio para sincronizar
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        val lastActiveDateStr = data.lastActiveDate?.let { dateFormat.format(it) }
        
        appwriteUserRepository.syncGamificationData(
            userId = userId,
            userLevel = data.userLevel,
            currentXp = data.currentXp,
            totalXp = data.totalXp,
            timeCoins = data.timeCoins,
            currentStreak = data.currentStreak,
            longestStreak = data.longestStreak,
            lastActiveDate = lastActiveDateStr,
            totalFocusMinutes = data.totalFocusMinutes,
            totalSessionsCompleted = data.totalSessionsCompleted,
            perfectDaysCount = data.perfectDaysCount,
            gamificationEnabled = data.gamificationEnabled,
            showXpNotifications = data.showXpNotifications,
            showStreakReminders = data.showStreakReminders
        )
        return true
    }
    
    /**
     * Obtiene los datos de gamificación actuales para sincronización
     */
    suspend fun getGamificationDataForSync(): GamificationSyncData? {
        val settings = userDao.getUserSettingsSync() ?: return null
        return GamificationSyncData(
            userLevel = settings.userLevel,
            currentXp = settings.currentXp,
            totalXp = settings.totalXp,
            timeCoins = settings.timeCoins,
            currentStreak = settings.currentStreak,
            longestStreak = settings.longestStreak,
            lastActiveDate = settings.lastActiveDate,
            totalFocusMinutes = settings.totalFocusMinutes,
            totalSessionsCompleted = settings.totalSessionsCompleted,
            perfectDaysCount = settings.perfectDaysCount,
            gamificationEnabled = settings.gamificationEnabled,
            showXpNotifications = settings.showXpNotifications,
            showStreakReminders = settings.showStreakReminders
        )
    }

    companion object {
        /**
         * Días de racha que merecen una notificación de hito: 1, 3, 7 y cada
         * múltiplo de 7. Función pura, probable en la JVM sin Android.
         */
        fun isStreakMilestone(day: Int): Boolean =
            day == 1 || day == 3 || (day >= 7 && day % 7 == 0)
    }
}

/**
 * Datos de gamificación para sincronización con Appwrite
 */
data class GamificationSyncData(
    val userLevel: Int,
    val currentXp: Int,
    val totalXp: Int,
    val timeCoins: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: Date?,
    val totalFocusMinutes: Int,
    val totalSessionsCompleted: Int,
    val perfectDaysCount: Int,
    val gamificationEnabled: Boolean,
    val showXpNotifications: Boolean,
    val showStreakReminders: Boolean
)

/**
 * Estado de gamificación para la UI
 */
data class GamificationState(
    val level: Int,
    val currentXp: Int,
    val totalXp: Int,
    val xpForNextLevel: Int,
    val xpToNextLevel: Int,
    val xpProgress: Float,
    val levelTitle: String,
    val levelEmoji: String,
    val timeCoins: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalFocusMinutes: Int,
    val totalSessions: Int,
    val perfectDays: Int,
    val streakMultiplier: Float
)

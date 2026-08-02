package com.momentummm.app.data.manager

import android.content.Context
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
                    xpProgress = it.getLevelProgress(),
                    levelTitle = it.getLevelTitle(),
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
            message = "¡Sesión completada! +$xpBonus XP"
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
            userDao.incrementStreak(today)
            return GamificationEvent(
                type = EventType.STREAK_CONTINUED,
                streakDays = 1,
                message = "🔥 ¡Racha iniciada!"
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
                    message = "🔥 Racha: ${settings.currentStreak} días"
                )
            }
            daysDifference == 1 -> {
                // Día consecutivo - incrementar racha
                userDao.incrementStreak(today)
                val newStreak = settings.currentStreak + 1
                GamificationEvent(
                    type = EventType.STREAK_CONTINUED,
                    streakDays = newStreak,
                    message = "🔥 ¡$newStreak días de racha!"
                )
            }
            else -> {
                // Racha rota - penalización
                breakStreak()
            }
        }
    }

    /**
     * Rompe la racha y aplica penalización (Loss Aversion)
     */
    suspend fun breakStreak(): GamificationEvent {
        val settings = userDao.getUserSettingsSync() ?: return GamificationEvent(EventType.STREAK_BROKEN)
        val previousStreak = settings.currentStreak
        
        if (previousStreak > 0) {
            // Aplicar penalización de XP
            userDao.subtractXp(UserSettings.XP_STREAK_BREAK_PENALTY)
            userDao.resetStreak(Date())
            
            // Enviar notificación de racha rota
            notificationManager?.showStreakBrokenNotification(previousStreak)
            
            return GamificationEvent(
                type = EventType.STREAK_BROKEN,
                xpGained = -UserSettings.XP_STREAK_BREAK_PENALTY,
                streakDays = 0,
                message = "💔 Racha de $previousStreak días perdida. -${UserSettings.XP_STREAK_BREAK_PENALTY} XP"
            )
        }
        
        return GamificationEvent(
            type = EventType.STREAK_BROKEN,
            streakDays = 0,
            message = "Inicia una nueva racha mañana"
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
        
        // Enviar notificación de día perfecto
        notificationManager?.showPerfectDayNotification()
        
        // Verificar si subió de nivel
        val levelUpEvent = checkAndProcessLevelUp()
        
        return levelUpEvent ?: GamificationEvent(
            type = EventType.PERFECT_DAY,
            xpGained = xpBonus,
            coinsGained = coinsBonus,
            message = "⭐ ¡Día perfecto! +$xpBonus XP"
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
            val levelTitle = updatedSettings?.getLevelTitle() ?: "Nivel $newLevel"
            notificationManager?.showLevelUpNotification(newLevel, levelTitle, levelUpCoins)
            
            return GamificationEvent(
                type = EventType.LEVEL_UP,
                newLevel = newLevel,
                coinsGained = levelUpCoins,
                message = "🎉 ¡Subiste a nivel $newLevel!"
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

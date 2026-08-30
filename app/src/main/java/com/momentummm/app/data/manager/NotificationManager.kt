package com.momentummm.app.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.momentummm.app.data.notificationPrefs
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

// El delegado del DataStore vive ahora en un único sitio compartido
// (com.momentummm.app.data.notificationPrefs) para no crear dos instancias sobre
// el mismo fichero, que hacía crashear la pantalla de notificaciones.

/**
 * Sistema de notificaciones inteligente y no intrusivo.
 * 
 * Características:
 * - Cooldown entre notificaciones del mismo tipo para evitar spam
 * - Agrupa notificaciones similares
 * - Respeta horarios silenciosos (noche)
 * - Prioriza notificaciones importantes
 * - Tracking de última notificación por app/tipo
 */
@Singleton
class SmartNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val goalsRepository: GoalsRepository,
    private val appLimitRepository: AppLimitRepository,
    private val quotesRepository: QuotesRepository,
    private val userRepository: UserRepository
) {

    companion object {
        // Canales de notificación
        const val CHANNEL_ID_LIMITS = "app_limits_channel"
        const val CHANNEL_ID_GOALS = "goals_achievements_channel"
        const val CHANNEL_ID_MOTIVATION = "daily_motivation_channel"
        const val CHANNEL_ID_WEEKLY_SUMMARY = "weekly_summary_channel"
        const val CHANNEL_ID_REMINDERS = "smart_reminders_channel"
        const val CHANNEL_ID_GAMIFICATION = "gamification_channel"
        const val CHANNEL_ID_WEBSITE_BLOCK = "website_block_channel"

        // IDs de notificaciones base (se suman hash del packageName para únicas por app)
        const val NOTIFICATION_ID_APP_LIMIT_WARNING = 2001
        const val NOTIFICATION_ID_APP_LIMIT_REACHED = 2002
        const val NOTIFICATION_ID_DAILY_MOTIVATION = 2003
        const val NOTIFICATION_ID_WEEKLY_SUMMARY = 2004
        const val NOTIFICATION_ID_GOAL_ACHIEVED = 2005
        const val NOTIFICATION_ID_MILESTONE = 2006
        const val NOTIFICATION_ID_SCREEN_TIME_REMINDER = 2007
        const val NOTIFICATION_ID_STREAK_WARNING = 2008
        const val NOTIFICATION_ID_GRACE_DAY_USED = 2009
        const val NOTIFICATION_ID_GRACE_DAY_LOW = 2010
        const val NOTIFICATION_ID_LEVEL_UP = 2011
        const val NOTIFICATION_ID_GAMIFICATION = 2012
        const val NOTIFICATION_ID_WEBSITE_BLOCKED = 2013
        const val NOTIFICATION_ID_APP_BLOCKED = 2014
        const val NOTIFICATION_ID_FOCUS_SESSION = 2015
        const val NOTIFICATION_ID_SYNC = 2016
        const val NOTIFICATION_ID_LIFE_WEEKS = 2017

        // =================== COOLDOWNS (en minutos) ===================
        // Cuánto tiempo debe pasar antes de volver a notificar lo mismo
        const val COOLDOWN_LIMIT_WARNING_MINUTES = 60L      // 1 hora entre advertencias de límite
        const val COOLDOWN_LIMIT_REACHED_MINUTES = 180L     // 3 horas entre "límite alcanzado"
        const val COOLDOWN_STREAK_WARNING_MINUTES = 120L    // 2 horas entre advertencias de racha
        const val COOLDOWN_ACHIEVEMENT_MINUTES = 60L        // 1 hora entre logros similares
        const val COOLDOWN_SCREEN_TIME_MINUTES = 180L       // 3 horas entre recordatorios
        const val COOLDOWN_GRACE_DAY_MINUTES = 1440L        // 24 horas (una vez al día)

        // Umbrales
        const val WARNING_THRESHOLD_PERCENT = 80 // Avisar cuando se usa el 80% del límite
        const val EXCESSIVE_USAGE_HOURS = 6 // Considerar uso excesivo después de 6 horas

        // Horario silencioso (no molestar)
        const val QUIET_HOURS_START = 22 // 10 PM
        const val QUIET_HOURS_END = 8    // 8 AM

        // Keys de preferencias de notificaciones habilitadas
        val APP_LIMITS_ENABLED = booleanPreferencesKey("app_limits_notifications")
        val DAILY_MOTIVATION_ENABLED = booleanPreferencesKey("daily_motivation")
        val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary")
        val ACHIEVEMENTS_ENABLED = booleanPreferencesKey("achievements_notifications")
        val SCREEN_TIME_REMINDERS_ENABLED = booleanPreferencesKey("screen_time_reminders")
        val STREAK_WARNINGS_ENABLED = booleanPreferencesKey("streak_warnings")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    }

    // CoroutineExceptionHandler para manejar excepciones sin crashear
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("SmartNotificationManager", "Coroutine exception", throwable)
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    
    // Cache en memoria de últimas notificaciones para evitar consultas frecuentes a DataStore
    // Usar ConcurrentHashMap para evitar ConcurrentModificationException
    private val lastNotificationCache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    init {
        createNotificationChannels()
        scheduleAllNotifications()
    }

    // =================== FUNCIONES DE COOLDOWN ===================
    
    /**
     * Genera una key única para el cooldown basada en tipo + identificador
     */
    private fun getCooldownKey(notificationType: String, identifier: String = ""): String {
        return "last_notif_${notificationType}_${identifier.hashCode()}"
    }
    
    /**
     * Verifica si podemos enviar una notificación según el cooldown
     */
    private suspend fun canSendNotification(
        notificationType: String,
        identifier: String = "",
        cooldownMinutes: Long
    ): Boolean {
        val key = getCooldownKey(notificationType, identifier)
        val now = System.currentTimeMillis()
        
        // Primero revisar cache en memoria
        lastNotificationCache[key]?.let { lastTime ->
            val elapsedMinutes = (now - lastTime) / (60 * 1000)
            if (elapsedMinutes < cooldownMinutes) {
                return false
            }
        }
        
        // Si no está en cache o expiró, revisar DataStore
        val prefKey = longPreferencesKey(key)
        val lastTime = context.notificationPrefs.data.first()[prefKey] ?: 0L
        val elapsedMinutes = (now - lastTime) / (60 * 1000)
        
        return elapsedMinutes >= cooldownMinutes
    }
    
    /**
     * Registra que se envió una notificación
     */
    private suspend fun recordNotificationSent(notificationType: String, identifier: String = "") {
        val key = getCooldownKey(notificationType, identifier)
        val now = System.currentTimeMillis()
        
        // Actualizar cache
        lastNotificationCache[key] = now
        
        // Persistir en DataStore
        val prefKey = longPreferencesKey(key)
        context.notificationPrefs.edit { prefs ->
            prefs[prefKey] = now
        }
    }

    // =================== VERIFICACIONES DE CONTEXTO ===================
    
    /**
     * Verifica si estamos en horario silencioso
     */
    private suspend fun isQuietHours(): Boolean {
        // Verificar si el usuario activó horario silencioso
        val quietHoursEnabled = context.notificationPrefs.data.first()[QUIET_HOURS_ENABLED] ?: false
        if (!quietHoursEnabled) return false
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_HOURS_START || hour < QUIET_HOURS_END
    }
    
    /**
     * Verifica si una notificación está habilitada por el usuario
     */
    private suspend fun isNotificationEnabled(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>): Boolean {
        return context.notificationPrefs.data.first()[key] ?: true
    }
    
    /**
     * Genera ID único de notificación por app para que no se sobrescriban
     */
    private fun getUniqueNotificationId(baseId: Int, packageName: String): Int {
        return baseId + (packageName.hashCode() and 0x0000FFFF)
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_LIMITS,
                    context.getString(R.string.notification_channel_limits_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_limits_desc)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_GOALS,
                    context.getString(R.string.notification_channel_goals_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_goals_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_MOTIVATION,
                    context.getString(R.string.notification_channel_motivation_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_motivation_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_WEEKLY_SUMMARY,
                    context.getString(R.string.notification_channel_weekly_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_weekly_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    context.getString(R.string.notification_channel_reminders_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_reminders_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_GAMIFICATION,
                    context.getString(R.string.notification_channel_gamification_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_gamification_desc)
                }
            )
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }
    
    /**
     * 1. Sistema inteligente de alertas de límites de apps
     * 
     * - Solo notifica una vez cuando llegas al 80% (no repite)
     * - Solo notifica una vez cuando alcanzas el límite (no repite por 3 horas)
     * - Respeta horario silencioso
     * - Cada app tiene su propio cooldown independiente
     */
    fun checkAppLimitsAndNotify() {
        scope.launch {
            try {
                if (!isNotificationEnabled(APP_LIMITS_ENABLED)) return@launch
                if (isQuietHours()) return@launch

                val enabledLimits = appLimitRepository.getAllEnabledLimits().first()
                val todayUsage = usageStatsRepository.getTodayUsageStats()

                enabledLimits.forEach { limit ->
                    val usage = todayUsage.find { it.packageName == limit.packageName }
                    if (usage != null) {
                        val usageMinutes = (usage.totalTimeInMillis / 60000).toInt()
                        val limitMinutes = limit.dailyLimitMinutes
                        
                        // Evitar división por cero
                        if (limitMinutes <= 0) return@forEach
                        
                        val usagePercent = (usageMinutes * 100) / limitMinutes

                        when {
                            // Límite alcanzado - cooldown de 3 horas por app
                            usageMinutes >= limitMinutes -> {
                                if (canSendNotification("limit_reached", limit.packageName, COOLDOWN_LIMIT_REACHED_MINUTES)) {
                                    showAppLimitReachedNotification(
                                        limit.packageName,
                                        limit.appName,
                                        usageMinutes,
                                        limitMinutes
                                    )
                                    recordNotificationSent("limit_reached", limit.packageName)
                                }
                            }
                            // Advertencia al 80% - cooldown de 1 hora por app
                            usagePercent >= WARNING_THRESHOLD_PERCENT && usagePercent < 100 -> {
                                if (canSendNotification("limit_warning", limit.packageName, COOLDOWN_LIMIT_WARNING_MINUTES)) {
                                    showAppLimitWarningNotification(
                                        limit.packageName,
                                        limit.appName,
                                        usageMinutes,
                                        limitMinutes,
                                        limitMinutes - usageMinutes
                                    )
                                    recordNotificationSent("limit_warning", limit.packageName)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAppLimitWarningNotification(
        packageName: String,
        appName: String,
        usageMinutes: Int,
        limitMinutes: Int,
        remainingMinutes: Int
    ) {
        val title = context.getString(R.string.notification_limit_warning_title, appName)
        val message = context.getString(
            R.string.notification_limit_warning_message,
            formatMinutes(usageMinutes),
            formatMinutes(limitMinutes),
            formatMinutes(remainingMinutes)
        )

        showNotification(
            channelId = CHANNEL_ID_LIMITS,
            notificationId = getUniqueNotificationId(NOTIFICATION_ID_APP_LIMIT_WARNING, packageName),
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_DEFAULT // Bajamos prioridad para ser menos intrusivos
        )
    }
    
    private fun showAppLimitReachedNotification(
        packageName: String,
        appName: String,
        usageMinutes: Int,
        limitMinutes: Int
    ) {
        val title = context.getString(R.string.notification_limit_reached_title, appName)
        val message = context.getString(R.string.notification_limit_reached_message, formatMinutes(limitMinutes))

        showNotification(
            channelId = CHANNEL_ID_LIMITS,
            notificationId = getUniqueNotificationId(NOTIFICATION_ID_APP_LIMIT_REACHED, packageName),
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = true // Que se pueda descartar fácilmente
        )
    }

    /**
     * 2. Resumen semanal de uso - Solo domingos a las 8 PM
     */
    fun sendWeeklySummary() {
        scope.launch {
            try {
                if (!isNotificationEnabled(WEEKLY_SUMMARY_ENABLED)) return@launch

                val weeklyUsage = usageStatsRepository.getWeeklyUsageStats()
                val totalMinutes = weeklyUsage.sumOf { it.totalTimeInMillis } / 60000
                val topApps = weeklyUsage.take(3)
                val goals = goalsRepository.getAllGoals().first()
                val completedGoals = goals.count { it.currentValue >= it.targetValue }

                val title = context.getString(R.string.notification_weekly_summary_title)
                val message = buildString {
                    append(context.getString(R.string.notification_weekly_summary_total_time, formatMinutes(totalMinutes.toInt())))
                    append("\n")
                    append(context.getString(R.string.notification_weekly_summary_top_apps))
                    append("\n")
                    topApps.forEachIndexed { index, app ->
                        append(context.getString(
                            R.string.notification_weekly_summary_app_item,
                            index + 1,
                            app.appName,
                            formatMinutes((app.totalTimeInMillis / 60000).toInt())
                        ))
                        append("\n")
                    }
                    if (completedGoals > 0) {
                        append("\n")
                        append(context.getString(R.string.notification_weekly_summary_goals, completedGoals))
                    }
                }

                showExpandableNotification(
                    channelId = CHANNEL_ID_WEEKLY_SUMMARY,
                    notificationId = NOTIFICATION_ID_WEEKLY_SUMMARY,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Enviar una notificación de prueba inmediata
     */
    fun sendTestNotification() {
        val title = context.getString(R.string.notification_test_title_sent)
        val message = context.getString(R.string.notification_test_message)
        
        showNotification(
            channelId = CHANNEL_ID_MOTIVATION,
            notificationId = 9999,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * 3. Motivación diaria con frases
     */
    fun sendDailyMotivation() {
        scope.launch {
            try {
                if (!isNotificationEnabled(DAILY_MOTIVATION_ENABLED)) return@launch

                val quote = quotesRepository.getRandomQuote()
                if (quote != null) {
                    val title = context.getString(R.string.notification_motivation_title)
                    val message = context.getString(R.string.notification_motivation_message, quote.text)

                    showNotification(
                        channelId = CHANNEL_ID_MOTIVATION,
                        notificationId = NOTIFICATION_ID_DAILY_MOTIVATION,
                        title = title,
                        message = message,
                        icon = R.drawable.ic_goal,
                        priority = NotificationCompat.PRIORITY_LOW
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 4. Logros por hitos alcanzados - con cooldowns para evitar spam
     */
    fun checkAndNotifyAchievements() {
        scope.launch {
            try {
                if (!isNotificationEnabled(ACHIEVEMENTS_ENABLED)) return@launch
                if (isQuietHours()) return@launch

                val goals = goalsRepository.getAllGoals().first()

                goals.forEach { goal ->
                    // Meta completada por primera vez - solo notificar una vez
                    if (goal.currentValue >= goal.targetValue && goal.completionCount == 1) {
                        showAchievementNotificationWithCooldown(
                            "first_goal_${goal.id}",
                            context.getString(R.string.notification_goal_achieved_title),
                            context.getString(R.string.notification_goal_achieved_message, goal.title)
                        )
                    }

                    // Rachas especiales - cooldown muy largo (7 días) para no repetir
                    when (goal.currentStreak) {
                        7 -> showMilestoneNotificationWithCooldown(
                            "streak_7_${goal.id}",
                            context.getString(R.string.notification_streak_7_title),
                            context.getString(R.string.notification_streak_7_message, goal.title)
                        )
                        30 -> showMilestoneNotificationWithCooldown(
                            "streak_30_${goal.id}",
                            context.getString(R.string.notification_streak_30_title),
                            context.getString(R.string.notification_streak_30_message, goal.title)
                        )
                        100 -> showMilestoneNotificationWithCooldown(
                            "streak_100_${goal.id}",
                            context.getString(R.string.notification_streak_100_title),
                            context.getString(R.string.notification_streak_100_message, goal.title)
                        )
                    }

                    // Múltiplos de 10 completados - con cooldown
                    if (goal.completionCount > 0 && goal.completionCount % 10 == 0) {
                        showMilestoneNotificationWithCooldown(
                            "completion_${goal.completionCount}_${goal.id}",
                            context.getString(R.string.notification_completion_title, goal.completionCount),
                            goal.title
                        )
                    }
                }

                // Logro por tiempo de pantalla reducido
                checkScreenTimeReduction()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun checkScreenTimeReduction() {
        try {
            val todayUsage = usageStatsRepository.getTodayUsageStats()
            val totalMinutes = todayUsage.sumOf { it.totalTimeInMillis } / 60000

            // Logro por mantener uso moderado (menos de 3 horas)
            // Solo notificar en la noche y con cooldown diario
            if (totalMinutes < 180) {
                val calendar = Calendar.getInstance()
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 20) {
                    showAchievementNotificationWithCooldown(
                        "balanced_day_$dayOfYear",
                        context.getString(R.string.notification_balanced_day_title),
                        context.getString(R.string.notification_balanced_day_message, formatMinutes(totalMinutes.toInt()))
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun showAchievementNotificationWithCooldown(
        identifier: String,
        title: String, 
        message: String
    ) {
        if (canSendNotification("achievement", identifier, COOLDOWN_ACHIEVEMENT_MINUTES)) {
            showNotification(
                channelId = CHANNEL_ID_GOALS,
                notificationId = getUniqueNotificationId(NOTIFICATION_ID_GOAL_ACHIEVED, identifier),
                title = title,
                message = message,
                icon = R.drawable.ic_goal,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            recordNotificationSent("achievement", identifier)
        }
    }

    private fun showAchievementNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_GOAL_ACHIEVED,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    private suspend fun showMilestoneNotificationWithCooldown(
        identifier: String,
        title: String, 
        message: String
    ) {
        // Los milestones solo se muestran una vez (cooldown muy largo)
        if (canSendNotification("milestone", identifier, 10080L)) { // 7 días
            showNotification(
                channelId = CHANNEL_ID_GOALS,
                notificationId = getUniqueNotificationId(NOTIFICATION_ID_MILESTONE, identifier),
                title = title,
                message = message,
                icon = R.drawable.ic_goal,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            recordNotificationSent("milestone", identifier)
        }
    }

    private fun showMilestoneNotification(title: String, message: String) {
        showNotification(
            channelId = CHANNEL_ID_GOALS,
            notificationId = NOTIFICATION_ID_MILESTONE,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    // ================== PROTECCIÓN DE RACHAS ==================
    
    /**
     * Notificación de advertencia de racha - cuando está cerca de romperla
     * Con cooldown de 2 horas por app para no ser spam
     */
    fun showStreakWarningNotification(
        packageName: String,
        appName: String,
        remainingMinutes: Int,
        currentStreak: Int
    ) {
        scope.launch {
            try {
                if (!isNotificationEnabled(STREAK_WARNINGS_ENABLED)) return@launch
                if (isQuietHours()) return@launch
                
                // Solo notificar si pasó el cooldown
                if (!canSendNotification("streak_warning", packageName, COOLDOWN_STREAK_WARNING_MINUTES)) {
                    return@launch
                }
                
                val title = context.resources.getQuantityString(R.plurals.notification_streak_warning_title, currentStreak, currentStreak)
                val message = context.getString(R.string.notification_streak_warning_message, remainingMinutes, appName)
                
                showNotification(
                    channelId = CHANNEL_ID_LIMITS,
                    notificationId = getUniqueNotificationId(NOTIFICATION_ID_STREAK_WARNING, packageName),
                    title = title,
                    message = message,
                    icon = R.drawable.ic_streak,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                
                recordNotificationSent("streak_warning", packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación cuando se usa un día de gracia - máximo una vez al día
     */
    fun showGraceDayUsedNotification(graceDaysRemaining: Int) {
        scope.launch {
            try {
                if (!isNotificationEnabled(STREAK_WARNINGS_ENABLED)) return@launch
                
                // Solo notificar una vez al día
                if (!canSendNotification("grace_day_used", "", COOLDOWN_GRACE_DAY_MINUTES)) {
                    return@launch
                }
                
                val title = context.getString(R.string.notification_grace_day_used_title)
                val message = if (graceDaysRemaining > 0) {
                    context.getString(R.string.notification_grace_day_used_message, graceDaysRemaining)
                } else {
                    context.getString(R.string.notification_grace_day_used_last)
                }
                
                showNotification(
                    channelId = CHANNEL_ID_GOALS,
                    notificationId = NOTIFICATION_ID_GRACE_DAY_USED,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal,
                    priority = NotificationCompat.PRIORITY_LOW // Baja prioridad, no urgente
                )
                
                recordNotificationSent("grace_day_used", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación cuando quedan pocos días de gracia - máximo una vez al día
     */
    fun showGraceDaysLowNotification(graceDaysRemaining: Int) {
        scope.launch {
            try {
                if (!isNotificationEnabled(STREAK_WARNINGS_ENABLED)) return@launch
                
                // Solo notificar una vez al día
                if (!canSendNotification("grace_day_low", "", COOLDOWN_GRACE_DAY_MINUTES)) {
                    return@launch
                }
                
                if (graceDaysRemaining <= 1) {
                    val title = if (graceDaysRemaining == 0) {
                        context.getString(R.string.notification_grace_day_low_zero_title)
                    } else {
                        context.getString(R.string.notification_grace_day_low_one_title)
                    }
                    val message = if (graceDaysRemaining == 0) {
                        context.getString(R.string.notification_grace_day_low_zero_message)
                    } else {
                        context.getString(R.string.notification_grace_day_low_one_message)
                    }
                    
                    showNotification(
                        channelId = CHANNEL_ID_LIMITS,
                        notificationId = NOTIFICATION_ID_GRACE_DAY_LOW,
                        title = title,
                        message = message,
                        icon = R.drawable.ic_streak,
                        priority = NotificationCompat.PRIORITY_DEFAULT
                    )
                    
                    recordNotificationSent("grace_day_low", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Recordatorio inteligente de tiempo de pantalla
     * - Solo entre 9 AM y 9 PM
     * - Solo si uso excede 6 horas
     * - Máximo una vez cada 3 horas
     */
    fun sendSmartScreenTimeReminder() {
        scope.launch {
            try {
                if (!isNotificationEnabled(SCREEN_TIME_REMINDERS_ENABLED)) return@launch
                if (isQuietHours()) return@launch
                
                // Solo notificar cada 3 horas como máximo
                if (!canSendNotification("screen_time", "", COOLDOWN_SCREEN_TIME_MINUTES)) {
                    return@launch
                }

                val todayUsage = usageStatsRepository.getTodayUsageStats()
                val totalMinutes = todayUsage.sumOf { it.totalTimeInMillis } / 60000

                if (totalMinutes > EXCESSIVE_USAGE_HOURS * 60) {
                    val title = context.getString(R.string.notification_screen_time_title)
                    val message = context.getString(R.string.notification_screen_time_message, formatMinutes(totalMinutes.toInt()))

                    showNotification(
                        channelId = CHANNEL_ID_REMINDERS,
                        notificationId = NOTIFICATION_ID_SCREEN_TIME_REMINDER,
                        title = title,
                        message = message,
                        icon = R.drawable.ic_reminder,
                        priority = NotificationCompat.PRIORITY_LOW // Baja prioridad
                    )
                    
                    recordNotificationSent("screen_time", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Programar todas las notificaciones periódicas
     * Los workers se encargan de llamar las funciones, que ya tienen su propio cooldown
     */
    private fun scheduleAllNotifications() {
        // Chequeo de límites cada 15 minutos (el cooldown interno evita spam)
        scheduleAppLimitsCheck()

        // Motivación diaria a las 9 AM
        scheduleDailyMotivation()

        // Resumen semanal los domingos a las 8 PM
        scheduleWeeklySummary()

        // Chequeo de logros cada 4 horas (menos frecuente, pero el cooldown interno maneja duplicados)
        scheduleAchievementsCheck()

        // Recordatorio de pantalla cada 3 horas (de 9 AM a 9 PM)
        scheduleScreenTimeReminder()
    }

    private fun scheduleAppLimitsCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Chequear cada 15 minutos, el cooldown interno evita notificaciones repetidas
        val workRequest = PeriodicWorkRequestBuilder<AppLimitsCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "app_limits_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleDailyMotivation() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailyMotivationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_motivation",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleWeeklySummary() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleAchievementsCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Cada 4 horas es suficiente para logros
        val workRequest = PeriodicWorkRequestBuilder<AchievementsCheckWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "achievements_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleScreenTimeReminder() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Cada 3 horas, pero el cooldown interno evita duplicados
        val workRequest = PeriodicWorkRequestBuilder<ScreenTimeReminderWorker>(3, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "screen_time_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    // Funciones auxiliares
    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setPriority(priority)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun showExpandableNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
    
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    // =================== NOTIFICACIONES DE GAMIFICACIÓN ===================
    
    /**
     * Notificación cuando el usuario sube de nivel
     */
    fun showLevelUpNotification(newLevel: Int, levelTitle: String, coinsBonus: Int) {
        val title = context.getString(R.string.notification_level_up_title, newLevel)
        val message = context.getString(R.string.notification_level_up_message, levelTitle, coinsBonus)
        
        showNotification(
            channelId = CHANNEL_ID_GAMIFICATION,
            notificationId = NOTIFICATION_ID_LEVEL_UP,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * Notificación de XP ganado (para eventos importantes)
     */
    fun showXpGainedNotification(xpAmount: Int, reason: String) {
        if (xpAmount < 50) return // Solo mostrar para ganancias significativas
        
        val title = context.getString(R.string.notification_xp_gained_title, xpAmount)
        val message = context.getString(R.string.notification_xp_gained_message, reason)
        
        showNotification(
            channelId = CHANNEL_ID_GAMIFICATION,
            notificationId = NOTIFICATION_ID_GAMIFICATION,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }
    
    /**
     * Notificación de HITO DE RACHA alcanzado — con el número REAL de días.
     *
     * Este era el hueco que el usuario reportó: al llegar a N días de racha no
     * salía ninguna notificación (updateDailyStreak devolvía el evento pero
     * nadie lo convertía en notificación, y los strings de racha iniciada /
     * continuada existían sin ningún llamador). Ahora se dispara desde
     * GamificationManager con el valor leído de la base tras incrementar, así
     * que el número mostrado es el real, no uno fijo.
     *
     * @param currentStreak días de racha REALES (>=1).
     * @param isNewRecord true si esta racha supera la mejor histórica.
     */
    fun showStreakMilestoneNotification(currentStreak: Int, isNewRecord: Boolean) {
        scope.launch {
            try {
                if (currentStreak < 1) return@launch
                if (!isNotificationEnabled(STREAK_WARNINGS_ENABLED)) return@launch
                // Una celebración no debe despertar al usuario de madrugada.
                if (isQuietHours()) return@launch
                // Evita duplicar el mismo hito el mismo día: updateDailyStreak se
                // invoca desde dos sitios en cada arranque (Dashboard y fin de
                // sesión de foco).
                if (!canSendNotification("streak_milestone", "day_$currentStreak", COOLDOWN_GRACE_DAY_MINUTES)) {
                    return@launch
                }

                val title = if (currentStreak == 1) {
                    context.getString(R.string.notification_streak_started_title)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.notification_streak_continued_title, currentStreak, currentStreak
                    )
                }
                val message = if (isNewRecord && currentStreak > 1) {
                    context.resources.getQuantityString(
                        R.plurals.notification_streak_record_message, currentStreak, currentStreak
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.notification_streak_milestone_message, currentStreak, currentStreak
                    )
                }

                showNotification(
                    channelId = CHANNEL_ID_GAMIFICATION,
                    notificationId = NOTIFICATION_ID_GAMIFICATION,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_streak,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                recordNotificationSent("streak_milestone", "day_$currentStreak")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Notificación de racha rota
     */
    fun showStreakBrokenNotification(previousStreak: Int) {        scope.launch {
            try {
                if (!isNotificationEnabled(STREAK_WARNINGS_ENABLED)) return@launch
                
                val title = context.getString(R.string.notification_streak_broken_title)
                val message = context.resources.getQuantityString(R.plurals.notification_streak_broken_message, previousStreak, previousStreak)
                
                showNotification(
                    channelId = CHANNEL_ID_GOALS,
                    notificationId = NOTIFICATION_ID_GAMIFICATION,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_streak,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación de día perfecto — con el XP REAL otorgado.
     *
     * Antes el texto llevaba "+500 XP" escrito a mano y la función no tenía
     * ningún llamador (código muerto). Ahora recibe el bonus real (que depende
     * del multiplicador de racha) y se dispara desde GamificationManager cuando
     * el día anterior se cerró sin bloquear ninguna app.
     */
    fun showPerfectDayNotification(xpBonus: Int) {
        scope.launch {
            try {
                if (!isNotificationEnabled(ACHIEVEMENTS_ENABLED)) return@launch
                if (isQuietHours()) return@launch
                if (!canSendNotification("perfect_day", "", COOLDOWN_GRACE_DAY_MINUTES)) return@launch
                
                val title = context.getString(R.string.notification_perfect_day_title)
                val message = context.getString(R.string.gam_ev_perfect_day, xpBonus)
                
                showNotification(
                    channelId = CHANNEL_ID_GAMIFICATION,
                    notificationId = NOTIFICATION_ID_GAMIFICATION + 1,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                recordNotificationSent("perfect_day", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación de logro desbloqueado
     */
    fun showAchievementUnlockedNotification(achievementName: String) {
        val title = context.getString(R.string.notification_achievement_unlocked_title)
        val message = context.getString(R.string.notification_achievement_unlocked_message, achievementName)
        
        showNotification(
            channelId = CHANNEL_ID_GAMIFICATION,
            notificationId = NOTIFICATION_ID_GAMIFICATION + achievementName.hashCode(),
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    // =================== NOTIFICACIONES DE BLOQUEO DE SITIOS WEB ===================
    
    /**
     * Notificación cuando se bloquea un sitio web
     */
    fun showWebsiteBlockedNotification(url: String) {
        scope.launch {
            try {
                if (!canSendNotification("website_blocked", url, 5L)) return@launch // 5 min cooldown
                
                val title = context.getString(R.string.notification_website_blocked_title)
                val message = context.getString(R.string.notification_website_blocked_message, url)
                
                showNotification(
                    channelId = CHANNEL_ID_LIMITS,
                    notificationId = NOTIFICATION_ID_WEBSITE_BLOCKED + url.hashCode(),
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                
                recordNotificationSent("website_blocked", url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación cuando se bloquea una categoría de sitios
     */
    fun showWebsiteCategoryBlockedNotification(category: String) {
        scope.launch {
            try {
                if (!canSendNotification("website_category_blocked", category, 10L)) return@launch
                
                val title = context.getString(R.string.notification_website_blocked_category_title)
                val message = context.getString(R.string.notification_website_blocked_category_message, category)
                
                showNotification(
                    channelId = CHANNEL_ID_LIMITS,
                    notificationId = NOTIFICATION_ID_WEBSITE_BLOCKED + category.hashCode(),
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                
                recordNotificationSent("website_category_blocked", category)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // =================== NOTIFICACIONES DE BLOQUEO DE APPS ===================
    
    /**
     * Notificación cuando se bloquea una app por límite
     */
    fun showAppBlockedNotification(packageName: String, appName: String, limitMinutes: Int) {
        scope.launch {
            try {
                if (!canSendNotification("app_blocked", packageName, 30L)) return@launch
                
                val title = context.getString(R.string.notification_app_blocked_title, appName)
                val message = context.getString(R.string.notification_app_blocked_limit_message, limitMinutes)
                
                showNotification(
                    channelId = CHANNEL_ID_LIMITS,
                    notificationId = NOTIFICATION_ID_APP_BLOCKED + packageName.hashCode(),
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                
                recordNotificationSent("app_blocked", packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación cuando se bloquea una app por modo enfoque
     */
    fun showAppBlockedByFocusModeNotification(appName: String) {
        scope.launch {
            try {
                if (!canSendNotification("focus_block", appName, 60L)) return@launch
                
                val title = context.getString(R.string.notification_app_blocked_focus_title)
                val message = context.getString(R.string.notification_app_blocked_focus_message, appName)
                
                showNotification(
                    channelId = CHANNEL_ID_LIMITS,
                    notificationId = NOTIFICATION_ID_APP_BLOCKED + 1000,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                
                recordNotificationSent("focus_block", appName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación cuando se bloquea una app por modo nuclear
     */
    fun showAppBlockedByNuclearModeNotification(appName: String, remainingDays: Int) {
        val title = context.getString(R.string.notification_app_blocked_nuclear_title)
        val message = context.getString(R.string.notification_app_blocked_nuclear_message, appName, remainingDays)
        
        showNotification(
            channelId = CHANNEL_ID_LIMITS,
            notificationId = NOTIFICATION_ID_APP_BLOCKED + 2000,
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * Notificación de modo sueño activo
     */
    fun showSleepModeNotification() {
        scope.launch {
            try {
                if (!canSendNotification("sleep_mode", "", 120L)) return@launch
                
                val title = context.getString(R.string.notification_app_blocked_sleep_title)
                val message = context.getString(R.string.notification_app_blocked_sleep_message)
                
                showNotification(
                    channelId = CHANNEL_ID_REMINDERS,
                    notificationId = NOTIFICATION_ID_APP_BLOCKED + 3000,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_LOW
                )
                
                recordNotificationSent("sleep_mode", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación de modo solo comunicación
     */
    fun showCommunicationOnlyModeNotification() {
        scope.launch {
            try {
                if (!canSendNotification("communication_only", "", 60L)) return@launch
                
                val title = context.getString(R.string.notification_app_blocked_communication_title)
                val message = context.getString(R.string.notification_app_blocked_communication_message)
                
                showNotification(
                    channelId = CHANNEL_ID_REMINDERS,
                    notificationId = NOTIFICATION_ID_APP_BLOCKED + 4000,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_reminder,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                
                recordNotificationSent("communication_only", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // =================== NOTIFICACIONES DE SESIÓN DE ENFOQUE ===================
    
    /**
     * Notificación cuando inicia una sesión de enfoque
     */
    fun showFocusStartedNotification(durationMinutes: Int) {
        val title = context.getString(R.string.notification_focus_started_title)
        val message = context.getString(R.string.notification_focus_started_message, durationMinutes)
        
        showNotification(
            channelId = CHANNEL_ID_REMINDERS,
            notificationId = NOTIFICATION_ID_FOCUS_SESSION,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }
    
    /**
     * Notificación cuando se completa una sesión de enfoque
     */
    fun showFocusCompletedNotification(xpEarned: Int, coinsEarned: Int) {
        val title = context.getString(R.string.notification_focus_completed_title)
        val message = context.getString(R.string.notification_focus_completed_message, xpEarned, coinsEarned)
        
        showNotification(
            channelId = CHANNEL_ID_GAMIFICATION,
            notificationId = NOTIFICATION_ID_FOCUS_SESSION + 1,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * Notificación de tiempo de descanso
     */
    fun showBreakTimeNotification(breakMinutes: Int) {
        val title = context.getString(R.string.notification_focus_break_title)
        val message = context.getString(R.string.notification_focus_break_message, breakMinutes)
        
        showNotification(
            channelId = CHANNEL_ID_REMINDERS,
            notificationId = NOTIFICATION_ID_FOCUS_SESSION + 2,
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    // =================== NOTIFICACIONES DE SINCRONIZACIÓN ===================
    
    /**
     * Notificación de sincronización completada
     */
    fun showSyncCompletedNotification() {
        val title = context.getString(R.string.notification_sync_completed_title)
        val message = context.getString(R.string.notification_sync_completed_message)
        
        showNotification(
            channelId = CHANNEL_ID_REMINDERS,
            notificationId = NOTIFICATION_ID_SYNC,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }
    
    /**
     * Notificación de error de sincronización
     */
    fun showSyncFailedNotification() {
        val title = context.getString(R.string.notification_sync_failed_title)
        val message = context.getString(R.string.notification_sync_failed_message)
        
        showNotification(
            channelId = CHANNEL_ID_REMINDERS,
            notificationId = NOTIFICATION_ID_SYNC,
            title = title,
            message = message,
            icon = R.drawable.ic_reminder,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }
    
    // =================== NOTIFICACIONES DE LIFE WEEKS ===================
    
    /**
     * Notificación de cumpleaños
     */
    fun showBirthdayNotification(weeksLived: Int) {
        val title = context.getString(R.string.notification_birthday_title)
        val message = context.getString(R.string.notification_birthday_message, weeksLived)
        
        showNotification(
            channelId = CHANNEL_ID_MOTIVATION,
            notificationId = NOTIFICATION_ID_LIFE_WEEKS,
            title = title,
            message = message,
            icon = R.drawable.ic_goal,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }
    
    /**
     * Notificación de reflexión semanal
     */
    fun showWeeklyReflectionNotification(weekNumber: Int) {
        scope.launch {
            try {
                if (!isNotificationEnabled(WEEKLY_SUMMARY_ENABLED)) return@launch
                
                val title = context.getString(R.string.notification_weekly_reflection_title)
                val message = context.getString(R.string.notification_weekly_reflection_message, weekNumber)
                
                showNotification(
                    channelId = CHANNEL_ID_MOTIVATION,
                    notificationId = NOTIFICATION_ID_LIFE_WEEKS + 1,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal,
                    priority = NotificationCompat.PRIORITY_LOW
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Notificación de hito de vida (cada 100 semanas)
     */
    fun showLifeMilestoneNotification(weeksLived: Int) {
        scope.launch {
            try {
                if (!canSendNotification("life_milestone", weeksLived.toString(), 10080L)) return@launch
                
                val title = context.getString(R.string.notification_life_milestone_title)
                val message = context.getString(R.string.notification_life_milestone_message, weeksLived)
                
                showNotification(
                    channelId = CHANNEL_ID_MOTIVATION,
                    notificationId = NOTIFICATION_ID_LIFE_WEEKS + 2,
                    title = title,
                    message = message,
                    icon = R.drawable.ic_goal,
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                
                recordNotificationSent("life_milestone", weeksLived.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // =================== CONFIGURACIÓN DE PREFERENCIAS ===================
    
    /**
     * Habilita o deshabilita un tipo de notificación
     */
    suspend fun setNotificationEnabled(
        key: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
        enabled: Boolean
    ) {
        context.notificationPrefs.edit { prefs ->
            prefs[key] = enabled
        }
    }
    
    /**
     * Habilita o deshabilita el horario silencioso (10 PM - 8 AM)
     */
    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.notificationPrefs.edit { prefs ->
            prefs[QUIET_HOURS_ENABLED] = enabled
        }
    }
    
    /**
     * Obtiene si el horario silencioso está habilitado
     */
    suspend fun isQuietHoursEnabled(): Boolean {
        return context.notificationPrefs.data.first()[QUIET_HOURS_ENABLED] ?: false
    }
    
    /**
     * Limpia el cache de cooldowns al inicio de un nuevo día
     * Esto permite que las notificaciones diarias vuelvan a aparecer
     */
    fun resetDailyCooldowns() {
        scope.launch {
            try {
                val keysToKeep = listOf("milestone", "grace_day") // Estos tienen cooldowns más largos
                lastNotificationCache.keys.removeAll { key ->
                    keysToKeep.none { key.contains(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Obtiene el estado de todas las preferencias de notificación
     */
    suspend fun getNotificationPreferences(): Map<String, Boolean> {
        val prefs = context.notificationPrefs.data.first()
        return mapOf(
            "app_limits" to (prefs[APP_LIMITS_ENABLED] ?: true),
            "daily_motivation" to (prefs[DAILY_MOTIVATION_ENABLED] ?: true),
            "weekly_summary" to (prefs[WEEKLY_SUMMARY_ENABLED] ?: true),
            "achievements" to (prefs[ACHIEVEMENTS_ENABLED] ?: true),
            "screen_time_reminders" to (prefs[SCREEN_TIME_REMINDERS_ENABLED] ?: true),
            "streak_warnings" to (prefs[STREAK_WARNINGS_ENABLED] ?: true),
            "quiet_hours" to (prefs[QUIET_HOURS_ENABLED] ?: false)
        )
    }
}

// Workers para notificaciones programadas
class AppLimitsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.checkAppLimitsAndNotify()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class DailyMotivationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.sendDailyMotivation()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.sendWeeklySummary()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class AchievementsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                .smartNotificationManager
            notificationManager.checkAndNotifyAchievements()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

class ScreenTimeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Solo enviar recordatorios entre 9 AM y 9 PM
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            if (hour in 9..20) {
                val notificationManager = (applicationContext as com.momentummm.app.MomentumApplication)
                    .smartNotificationManager
                notificationManager.sendSmartScreenTimeReminder()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
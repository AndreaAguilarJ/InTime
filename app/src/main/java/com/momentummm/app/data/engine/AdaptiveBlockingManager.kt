package com.momentummm.app.data.engine

import android.content.Context
import android.util.Log
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  █████╗ ██████╗  █████╗ ██████╗ ████████╗██╗██╗   ██╗███████╗
// ██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚══██╔══╝██║██║   ██║██╔════╝
// ███████║██║  ██║███████║██████╔╝   ██║   ██║██║   ██║█████╗  
// ██╔══██║██║  ██║██╔══██║██╔═══╝    ██║   ██║╚██╗ ██╔╝██╔══╝  
// ██║  ██║██████╔╝██║  ██║██║        ██║   ██║ ╚████╔╝ ███████╗
// ╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝        ╚═╝   ╚═╝  ╚═══╝  ╚══════╝
// ADAPTIVE BLOCKING MANAGER - Bloqueo que aprende y se adapta
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Perfil de modo enfoque con configuración completa.
 */
@Entity(
    tableName = "focus_profiles",
    indices = [Index(value = ["name"], unique = true)]
)
data class FocusProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,                        // "Trabajo", "Estudio", "Descanso", "Ejercicio"
    val icon: String = "🎯",                 // Emoji del perfil
    val color: Long = 0xFF6200EE,            // Color del perfil
    val isActive: Boolean = false,
    val isDefault: Boolean = false,          // Perfiles que vienen con la app
    
    // ═══ CONFIGURACIÓN DE BLOQUEO ═══
    val blockAllApps: Boolean = false,       // Bloquear todas las apps
    val blockedApps: String = "",            // Paquetes separados por coma
    val allowedApps: String = "",            // Apps permitidas (whitelist del perfil)
    val blockWebsites: Boolean = false,
    val blockedCategories: String = "",      // Categorías bloqueadas
    
    // ═══ IN-APP BLOCKING ═══
    val blockShortVideos: Boolean = true,    // Reels, Shorts, TikTok
    val blockExplore: Boolean = true,        // Feeds de exploración
    val blockFeeds: Boolean = false,         // Feeds principales
    val blockStories: Boolean = false,
    val blockLiveStreams: Boolean = false,
    val blockShopping: Boolean = false,
    val blockGaming: Boolean = true,
    val allowMessaging: Boolean = true,      // Siempre permitir DMs
    
    // ═══ RESTRICCIONES DE TIEMPO ═══
    val hasTimeLimit: Boolean = false,
    val timeLimitMinutes: Int = 60,          // Límite total durante este modo
    val perAppLimitMinutes: Int = 15,        // Límite por app durante este modo
    
    // ═══ PROGRAMACIÓN AUTOMÁTICA ═══
    val autoActivate: Boolean = false,
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 17,
    val scheduleEndMinute: Int = 0,
    val scheduleDaysOfWeek: String = "1,2,3,4,5",
    
    // ═══ ACTIVACIÓN POR CONTEXTO ═══
    val activateByWifi: Boolean = false,
    val activationWifiSsid: String = "",
    val activateByLocation: Boolean = false,
    val activationLatitude: Double = 0.0,
    val activationLongitude: Double = 0.0,
    val activationRadiusMeters: Int = 200,
    
    // ═══ GRADUAL BLOCKING ═══
    val useGradualBlocking: Boolean = true,  // Bloqueo progresivo
    val gradualStartPercent: Int = 70,       // Empezar advertencias al 70% del límite
    val gradualStages: Int = 3,              // 3 etapas de bloqueo
    
    // ═══ PENALIZACIÓN Y RECOMPENSAS ═══
    val strictMode: Boolean = false,         // No permitir desbloqueo
    val punishmentMultiplier: Float = 1.0f,  // Multiplicador si intenta desbloquear
    val rewardStreakDays: Int = 0,           // Días consecutivos cumpliendo
    
    // Timestamps
    val lastActivatedAt: Long? = null,
    val totalTimeActive: Long = 0,           // Tiempo total en este modo (ms)
    val timesActivated: Int = 0,
    val timesCompleted: Int = 0,             // Veces que completó el período sin desbloquear
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getBlockedAppsList(): List<String> =
        if (blockedApps.isBlank()) emptyList()
        else blockedApps.split(",").map { it.trim() }.filter { it.isNotBlank() }
    
    fun getAllowedAppsList(): List<String> =
        if (allowedApps.isBlank()) emptyList()
        else allowedApps.split(",").map { it.trim() }.filter { it.isNotBlank() }
    
    fun getBlockedCategoriesList(): List<String> =
        if (blockedCategories.isBlank()) emptyList()
        else blockedCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
    
    fun getScheduleDays(): List<Int> =
        scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    
    fun isScheduleActive(): Boolean {
        if (!autoActivate) return false
        val now = Calendar.getInstance()
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val adjustedDay = if (dow == Calendar.SUNDAY) 7 else dow - 1
        if (adjustedDay !in getScheduleDays()) return false
        
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = scheduleStartHour * 60 + scheduleStartMinute
        val endMinutes = scheduleEndHour * 60 + scheduleEndMinute
        
        return currentMinutes in startMinutes until endMinutes
    }
    
    /**
     * Obtiene el límite de tiempo por app en este perfil.
     */
    fun getTimeLimitForApp(packageName: String): Int? {
        if (!hasTimeLimit) return null
        // Si la app está en la lista de bloqueadas, aplicar el límite por app
        if (packageName in getBlockedAppsList()) {
            return perAppLimitMinutes
        }
        // Si se bloquean todas las apps y esta no está en la whitelist
        if (blockAllApps && packageName !in getAllowedAppsList()) {
            return perAppLimitMinutes
        }
        return null
    }
}

/**
 * Registro de evento de bloqueo para analytics.
 */
@Entity(
    tableName = "blocking_events",
    indices = [
        Index(value = ["packageName", "timestamp"]),
        Index(value = ["eventType"]),
        Index(value = ["timestamp"])
    ]
)
data class BlockingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: BlockingEventType,
    val reason: String = "",                 // Razón del bloqueo
    val blockMethod: String = "",            // Método usado (overlay, back, home)
    val userReaction: String = "",           // Aceptó, intentó desbloquear, etc.
    val durationSeconds: Int = 0,            // Tiempo que duró el bloqueo
    val detectionConfidence: Float = 0f,     // Confianza de la detección
    val contentType: String = "",            // Tipo de contenido bloqueado
    val focusProfileId: Int? = null,         // Perfil activo al momento
    val ruleId: String? = null,              // Regla que disparó el bloqueo
    val wasOverridden: Boolean = false,      // Si el usuario lo anuló
    val overrideMethod: String? = null       // Cómo anuló (shame, pay, etc.)
)

enum class BlockingEventType {
    APP_BLOCKED,                 // App bloqueada por límite
    APP_BLOCK_ATTEMPTED,         // Intento de abrir app bloqueada
    FEATURE_BLOCKED,             // Función in-app bloqueada
    WEBSITE_BLOCKED,             // Sitio web bloqueado
    GRADUAL_WARNING_SHOWN,       // Advertencia gradual mostrada
    GRADUAL_SLOWDOWN_APPLIED,    // Ralentización aplicada
    BLOCK_OVERRIDDEN,            // Bloqueo anulado por el usuario
    LIMIT_ADJUSTED,              // Límite ajustado adaptativamente
    FOCUS_MODE_STARTED,          // Modo enfoque iniciado
    FOCUS_MODE_ENDED,            // Modo enfoque terminado
    FOCUS_MODE_BROKEN,           // Modo enfoque roto (desbloqueó)
    NUCLEAR_MODE_ACTIVATED,      // Modo nuclear activado
    STREAK_BROKEN,               // Racha rota
    STREAK_MILESTONE,            // Hito de racha alcanzado
    SMART_LIMIT_APPLIED,         // Límite inteligente aplicado
    PREDICTION_GENERATED,        // Predicción generada
    INTERVENTION_SHOWN           // Intervención mostrada (mensaje motivacional, etc.)
}

/**
 * Estrategia de bloqueo gradual - en vez de bloquear de golpe, 
 * aplica restricciones incrementales.
 */
data class GradualBlockingStage(
    val stageNumber: Int,
    val usagePercent: Int,       // Porcentaje de uso que dispara esta etapa
    val action: GradualAction,
    val message: String,
    val severity: Float          // 0.0 - 1.0
)

enum class GradualAction {
    GENTLE_REMINDER,             // Notificación suave
    WARNING_OVERLAY,             // Overlay de advertencia (no bloquea)
    BLOCK_INFINITE_SCROLL,       // Bloquear solo scroll infinito/shorts/reels
    SLOW_DOWN_APP,               // Agregar delays artificiales (anti-dopamina)
    GRAYSCALE_CONTENT,           // Convertir contenido a escala de grises
    BLOCK_NEW_CONTENT,           // Solo permitir contenido ya visto
    SOFT_BLOCK,                  // Bloqueo suave (se puede desbloquear fácil)
    HARD_BLOCK,                  // Bloqueo duro (requiere shame/pago)
    NUCLEAR_BLOCK                // Bloqueo total sin opción de desbloqueo
}

/**
 * Intervención inteligente basada en el contexto actual.
 */
data class SmartIntervention(
    val type: InterventionType,
    val message: String,
    val severity: Float,             // 0.0 - 1.0
    val actionSuggestion: String,    // Qué debería hacer el usuario
    val relatedPattern: PatternType?,
    val estimatedImpactMinutes: Int  // Minutos que podría ahorrar
)

enum class InterventionType {
    MORNING_NUDGE,           // "Empieza tu día sin pantallas"
    BEDTIME_REMINDER,        // "Es hora de dormir"
    BREAK_SUGGESTION,        // "Llevas X min seguidos, toma un descanso"
    PATTERN_AWARENESS,       // "Noté que abres X cuando estás aburrido"
    STREAK_MOTIVATION,       // "¡Llevas X días cumpliendo!"
    PROGRESS_CELEBRATION,    // "Redujiste tu uso un X% esta semana"
    REPLACEMENT_ACTIVITY,    // "En vez de X, ¿qué tal Y?"
    ACCOUNTABILITY_CHECK,    // "¿Realmente necesitas abrir esta app ahora?"
    SOCIAL_COMPARISON,       // "Los usuarios promedio usan X minutos menos"
    FUTURE_SELF,             // "Tu yo del futuro te agradecerá"
    COST_AWARENESS,          // "Esta semana pasaste X horas, equivalente a..."
    GOAL_REMINDER            // "Recuerda: tu meta es Y"
}

@Dao
interface FocusProfileDao {
    @Query("SELECT * FROM focus_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<FocusProfile>>
    
    @Query("SELECT * FROM focus_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): FocusProfile?
    
    @Query("SELECT * FROM focus_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileFlow(): Flow<FocusProfile?>
    
    @Query("SELECT * FROM focus_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): FocusProfile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FocusProfile)
    
    @Update
    suspend fun updateProfile(profile: FocusProfile)
    
    @Delete
    suspend fun deleteProfile(profile: FocusProfile)
    
    @Query("UPDATE focus_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()
    
    @Query("UPDATE focus_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: Int)
    
    @Query("""
        UPDATE focus_profiles 
        SET timesActivated = timesActivated + 1,
            lastActivatedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun recordActivation(id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE focus_profiles SET timesCompleted = timesCompleted + 1 WHERE id = :id")
    suspend fun recordCompletion(id: Int)
    
    @Query("""
        UPDATE focus_profiles 
        SET rewardStreakDays = rewardStreakDays + 1
        WHERE id = :id
    """)
    suspend fun incrementStreak(id: Int)
    
    @Query("UPDATE focus_profiles SET rewardStreakDays = 0 WHERE id = :id")
    suspend fun resetStreak(id: Int)
}

@Dao
interface BlockingEventDao {
    @Insert
    suspend fun insertEvent(event: BlockingEvent)
    
    @Insert
    suspend fun insertEvents(events: List<BlockingEvent>)
    
    @Query("SELECT * FROM blocking_events WHERE packageName = :pkg AND timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsForPackage(pkg: String, since: Long): List<BlockingEvent>
    
    @Query("SELECT * FROM blocking_events WHERE eventType = :type AND timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsByType(type: BlockingEventType, since: Long): List<BlockingEvent>
    
    @Query("SELECT * FROM blocking_events WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(since: Long, limit: Int = 100): List<BlockingEvent>
    
    @Query("""
        SELECT packageName, COUNT(*) as blockCount 
        FROM blocking_events 
        WHERE eventType IN ('APP_BLOCKED', 'FEATURE_BLOCKED') AND timestamp >= :since
        GROUP BY packageName 
        ORDER BY blockCount DESC
    """)
    suspend fun getMostBlockedApps(since: Long): List<AppBlockCount>
    
    @Query("""
        SELECT COUNT(*) FROM blocking_events 
        WHERE eventType = 'BLOCK_OVERRIDDEN' AND timestamp >= :since
    """)
    suspend fun getOverrideCount(since: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM blocking_events 
        WHERE packageName = :pkg AND eventType = 'BLOCK_OVERRIDDEN' AND timestamp >= :since
    """)
    suspend fun getOverrideCountForApp(pkg: String, since: Long): Int
    
    @Query("DELETE FROM blocking_events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long)
    
    @Query("""
        SELECT AVG(durationSeconds) FROM blocking_events 
        WHERE packageName = :pkg AND eventType = 'APP_BLOCKED' AND timestamp >= :since
    """)
    suspend fun getAvgBlockDuration(pkg: String, since: Long): Float?
}

data class AppBlockCount(
    val packageName: String,
    val blockCount: Int
)

// ═══════════════════════════════════════════════════════════════════════════
// ADAPTIVE BLOCKING MANAGER - El cerebro adaptativo
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class AdaptiveBlockingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patternEngine: UsagePatternEngine,
    private val detectionEngine: AdvancedDetectionEngine,
    private val focusProfileDao: FocusProfileDao,
    private val blockingEventDao: BlockingEventDao,
    private val patternDao: UsagePatternDao
) {
    private val TAG = "AdaptiveBlocking"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══ ESTADOS OBSERVABLES ═══
    
    private val _activeFocusProfile = MutableStateFlow<FocusProfile?>(null)
    val activeFocusProfile: StateFlow<FocusProfile?> = _activeFocusProfile.asStateFlow()
    
    private val _currentGradualStage = MutableStateFlow<Map<String, GradualBlockingStage>>(emptyMap())
    val currentGradualStage: StateFlow<Map<String, GradualBlockingStage>> = _currentGradualStage.asStateFlow()
    
    private val _pendingInterventions = MutableStateFlow<List<SmartIntervention>>(emptyList())
    val pendingInterventions: StateFlow<List<SmartIntervention>> = _pendingInterventions.asStateFlow()
    
    private val _dailyStats = MutableStateFlow(DailyBlockingStats())
    val dailyStats: StateFlow<DailyBlockingStats> = _dailyStats.asStateFlow()
    
    // Tracking de sesiones activas
    private val activeSessions = mutableMapOf<String, Long>() // pkg -> startTimestamp
    
    init {
        scope.launch {
            // Monitorear perfil activo
            focusProfileDao.getActiveProfileFlow().collect { profile ->
                _activeFocusProfile.value = profile
            }
        }
        
        scope.launch {
            initializeDefaultProfiles()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PERFILES DE ENFOQUE
    // ═══════════════════════════════════════════════════════════════
    
    private suspend fun initializeDefaultProfiles() {
        val existing = focusProfileDao.getAllProfiles().first()
        if (existing.isNotEmpty()) return
        
        val defaults = listOf(
            FocusProfile(
                name = "Trabajo",
                icon = "💼",
                color = 0xFF1976D2,
                isDefault = true,
                blockShortVideos = true,
                blockExplore = true,
                blockFeeds = true,
                blockStories = true,
                blockLiveStreams = true,
                blockGaming = true,
                allowMessaging = true,
                hasTimeLimit = true,
                timeLimitMinutes = 30,
                perAppLimitMinutes = 10,
                autoActivate = true,
                scheduleStartHour = 9,
                scheduleEndHour = 18,
                scheduleDaysOfWeek = "1,2,3,4,5",
                useGradualBlocking = true,
                gradualStartPercent = 60
            ),
            FocusProfile(
                name = "Estudio",
                icon = "📚",
                color = 0xFF388E3C,
                isDefault = true,
                blockShortVideos = true,
                blockExplore = true,
                blockFeeds = true,
                blockStories = true,
                blockLiveStreams = true,
                blockShopping = true,
                blockGaming = true,
                allowMessaging = false,
                hasTimeLimit = true,
                timeLimitMinutes = 15,
                perAppLimitMinutes = 5,
                useGradualBlocking = true,
                strictMode = true
            ),
            FocusProfile(
                name = "Descanso",
                icon = "🌙",
                color = 0xFF7B1FA2,
                isDefault = true,
                blockShortVideos = false,
                blockExplore = false,
                blockFeeds = false,
                blockStories = false,
                allowMessaging = true,
                hasTimeLimit = true,
                timeLimitMinutes = 120,
                perAppLimitMinutes = 30,
                useGradualBlocking = true,
                gradualStartPercent = 80
            ),
            FocusProfile(
                name = "Ejercicio",
                icon = "🏃",
                color = 0xFFE64A19,
                isDefault = true,
                blockAllApps = true,
                allowedApps = "com.spotify.music,com.google.android.apps.fitness",
                allowMessaging = false,
                strictMode = true
            ),
            FocusProfile(
                name = "Familia",
                icon = "👨‍👩‍👧‍👦",
                color = 0xFFF57C00,
                isDefault = true,
                blockShortVideos = true,
                blockExplore = true,
                blockFeeds = true,
                blockGaming = true,
                allowMessaging = true,
                hasTimeLimit = true,
                timeLimitMinutes = 20,
                useGradualBlocking = true
            ),
            FocusProfile(
                name = "Creatividad",
                icon = "🎨",
                color = 0xFFAD1457,
                isDefault = true,
                blockShortVideos = true,
                blockExplore = false,
                blockFeeds = true,
                allowMessaging = true,
                hasTimeLimit = true,
                timeLimitMinutes = 45,
                perAppLimitMinutes = 15
            ),
            FocusProfile(
                name = "Dormido",
                icon = "😴",
                color = 0xFF283593,
                isDefault = true,
                blockAllApps = true,
                allowedApps = "com.google.android.dialer,com.android.phone",
                strictMode = true,
                autoActivate = true,
                scheduleStartHour = 23,
                scheduleEndHour = 7,
                scheduleDaysOfWeek = "1,2,3,4,5,6,7"
            )
        )
        
        defaults.forEach { focusProfileDao.insertProfile(it) }
        Log.d(TAG, "Initialized ${defaults.size} default focus profiles")
    }
    
    // ═══════════════════════════════════════════════════════════════
    // BLOQUEO GRADUAL PROGRESIVO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene la estrategia de bloqueo gradual para una app basándose
     * en su porcentaje de uso actual.
     */
    fun getGradualBlockingStage(
        packageName: String,
        usagePercent: Int,
        profile: FocusProfile? = _activeFocusProfile.value
    ): GradualBlockingStage? {
        if (profile == null || !profile.useGradualBlocking) return null
        
        val stages = buildGradualStages(profile)
        return stages.lastOrNull { usagePercent >= it.usagePercent }
    }
    
    private fun buildGradualStages(profile: FocusProfile): List<GradualBlockingStage> {
        val startPercent = profile.gradualStartPercent
        val stageCount = profile.gradualStages.coerceIn(2, 5)
        val percentPerStage = (100 - startPercent) / stageCount
        
        return buildList {
            // Etapa 1: Recordatorio suave
            add(GradualBlockingStage(
                stageNumber = 1,
                usagePercent = startPercent,
                action = GradualAction.GENTLE_REMINDER,
                message = "Ya usaste el ${startPercent}% de tu tiempo. Considera tomar un descanso.",
                severity = 0.2f
            ))
            
            // Etapa 2: Advertencia con overlay
            if (stageCount >= 2) {
                val percent = startPercent + percentPerStage
                add(GradualBlockingStage(
                    stageNumber = 2,
                    usagePercent = percent,
                    action = GradualAction.BLOCK_INFINITE_SCROLL,
                    message = "Has usado el ${percent}%. Se bloqueó el scroll infinito y videos cortos.",
                    severity = 0.4f
                ))
            }
            
            // Etapa 3: Ralentización
            if (stageCount >= 3) {
                val percent = startPercent + percentPerStage * 2
                add(GradualBlockingStage(
                    stageNumber = 3,
                    usagePercent = percent,
                    action = GradualAction.SLOW_DOWN_APP,
                    message = "Has usado el ${percent}%. La app se ralentizará para desincentivar el uso.",
                    severity = 0.6f
                ))
            }
            
            // Etapa 4: Bloqueo suave
            if (stageCount >= 4) {
                val percent = startPercent + percentPerStage * 3
                add(GradualBlockingStage(
                    stageNumber = 4,
                    usagePercent = percent,
                    action = GradualAction.SOFT_BLOCK,
                    message = "Has excedido el ${percent}% de tu límite.",
                    severity = 0.8f
                ))
            }
            
            // Etapa final: Bloqueo duro
            add(GradualBlockingStage(
                stageNumber = stageCount,
                usagePercent = 100,
                action = if (profile.strictMode) GradualAction.NUCLEAR_BLOCK else GradualAction.HARD_BLOCK,
                message = "Has alcanzado tu límite. App bloqueada.",
                severity = 1.0f
            ))
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // INTERVENCIONES INTELIGENTES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Genera intervenciones inteligentes basadas en el contexto actual
     * y los patrones de uso detectados.
     */
    suspend fun generateInterventions(packageName: String, currentUsageMinutes: Int): List<SmartIntervention> {
        val interventions = mutableListOf<SmartIntervention>()
        val patterns = patternEngine.detectedPatterns.value[packageName] ?: emptyList()
        val score = patternEngine.addictionScores.value[packageName]
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        // 1. Intervención matutina
        if (currentHour in 6..8 && patterns.any { it.patternType == PatternType.MORNING_SCROLL }) {
            interventions.add(SmartIntervention(
                type = InterventionType.MORNING_NUDGE,
                message = "🌅 Empieza tu día con intención, no con scroll. ¿Qué tal si desayunas, meditas o haces ejercicio primero?",
                severity = 0.6f,
                actionSuggestion = "Cierra la app y establece tu intención del día",
                relatedPattern = PatternType.MORNING_SCROLL,
                estimatedImpactMinutes = 15
            ))
        }
        
        // 2. Intervención nocturna
        if (currentHour >= 22 && patterns.any { it.patternType == PatternType.LATE_NIGHT_BINGE }) {
            interventions.add(SmartIntervention(
                type = InterventionType.BEDTIME_REMINDER,
                message = "🌙 Es tarde. El uso de pantallas antes de dormir afecta tu sueño. Mañana te sentirás mejor si duermes ahora.",
                severity = 0.8f,
                actionSuggestion = "Activa el modo avión y lee un libro",
                relatedPattern = PatternType.LATE_NIGHT_BINGE,
                estimatedImpactMinutes = 30
            ))
        }
        
        // 3. Pausa por sesión larga
        if (currentUsageMinutes >= 20) {
            interventions.add(SmartIntervention(
                type = InterventionType.BREAK_SUGGESTION,
                message = "⏰ Llevas ${currentUsageMinutes} minutos seguidos. Tu cerebro necesita un descanso. La regla 20-20-20: cada 20 min, mira algo a 20 pies de distancia por 20 segundos.",
                severity = 0.5f,
                actionSuggestion = "Levántate, estírate, toma agua",
                relatedPattern = null,
                estimatedImpactMinutes = currentUsageMinutes / 3
            ))
        }
        
        // 4. Consciencia de patrón
        if (patterns.any { it.patternType == PatternType.BOREDOM_TRIGGER }) {
            interventions.add(SmartIntervention(
                type = InterventionType.PATTERN_AWARENESS,
                message = "🧠 He notado que abres esta app muchas veces por periodos cortos. Esto indica un patrón de aburrimiento. ¿Realmente necesitas revisarla ahora?",
                severity = 0.4f,
                actionSuggestion = "Pregúntate: ¿Qué estoy buscando realmente?",
                relatedPattern = PatternType.BOREDOM_TRIGGER,
                estimatedImpactMinutes = 10
            ))
        }
        
        // 5. Check de responsabilidad
        if (currentUsageMinutes > 5 && score != null && score.overallScore > 50) {
            interventions.add(SmartIntervention(
                type = InterventionType.ACCOUNTABILITY_CHECK,
                message = "🤔 ¿Abriste esta app con un propósito específico? Si ya lo completaste, es buen momento para cerrarla.",
                severity = 0.3f,
                actionSuggestion = "Define tu propósito antes de continuar",
                relatedPattern = null,
                estimatedImpactMinutes = currentUsageMinutes / 2
            ))
        }
        
        // 6. Costo del tiempo
        if (currentUsageMinutes >= 30) {
            val weeklyHours = (currentUsageMinutes * 7 / 60f)
            interventions.add(SmartIntervention(
                type = InterventionType.COST_AWARENESS,
                message = "📊 A este ritmo, pasarás ${String.format("%.1f", weeklyHours)} horas esta semana aquí. Eso es como ${getCostComparison(currentUsageMinutes * 7)}.",
                severity = 0.5f,
                actionSuggestion = "Visualiza qué podrías hacer con ese tiempo",
                relatedPattern = null,
                estimatedImpactMinutes = 15
            ))
        }
        
        // 7. Actividad de reemplazo
        if (currentUsageMinutes >= 15) {
            val activity = getReplacementActivity(currentHour)
            interventions.add(SmartIntervention(
                type = InterventionType.REPLACEMENT_ACTIVITY,
                message = activity.first,
                severity = 0.3f,
                actionSuggestion = activity.second,
                relatedPattern = null,
                estimatedImpactMinutes = 20
            ))
        }
        
        // 8. Comparación social
        if (score != null && score.overallScore > 40) {
            interventions.add(SmartIntervention(
                type = InterventionType.SOCIAL_COMPARISON,
                message = "📱 Los usuarios que más mejoran su productividad usan esta app un promedio de 20 minutos al día. Tú llevas $currentUsageMinutes minutos hoy.",
                severity = 0.4f,
                actionSuggestion = "Establece un objetivo personal de reducción",
                relatedPattern = null,
                estimatedImpactMinutes = currentUsageMinutes - 20
            ))
        }
        
        _pendingInterventions.value = interventions
        return interventions
    }
    
    private fun getCostComparison(weeklyMinutes: Int): String {
        return when {
            weeklyMinutes >= 1200 -> "leer 4 libros completos"
            weeklyMinutes >= 600 -> "aprender un nuevo idioma"
            weeklyMinutes >= 300 -> "completar un curso online"
            weeklyMinutes >= 120 -> "hacer ejercicio toda la semana"
            weeklyMinutes >= 60 -> "cocinar una cena gourmet"
            else -> "dar un paseo largo"
        }
    }
    
    private fun getReplacementActivity(currentHour: Int): Pair<String, String> {
        return when (currentHour) {
            in 6..9 -> "🏃 ¿Qué tal empezar el día con ejercicio o meditación?" to "Salir a caminar 15 minutos"
            in 10..12 -> "💡 Es tu hora más productiva. ¿Tienes algo pendiente?" to "Trabaja en tu tarea más importante"
            in 12..14 -> "🍽️ Es hora del almuerzo. Come sin pantallas." to "Disfruta tu comida con atención plena"
            in 14..17 -> "🎯 Aún queda tarde productiva. ¿Qué quieres lograr hoy?" to "Completa una tarea pendiente"
            in 17..20 -> "🌳 Sal a tomar aire fresco o haz algo creativo." to "Sal a caminar o llama a un amigo"
            in 20..22 -> "📚 ¿Qué tal leer algo interesante?" to "Lee un capítulo de un libro"
            else -> "😴 Es hora de descansar. Tu sueño es más importante." to "Prepárate para dormir"
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GESTIÓN DE PERFILES
    // ═══════════════════════════════════════════════════════════════
    
    suspend fun activateProfile(profileId: Int) {
        focusProfileDao.deactivateAllProfiles()
        focusProfileDao.activateProfile(profileId)
        focusProfileDao.recordActivation(profileId)
        
        blockingEventDao.insertEvent(BlockingEvent(
            packageName = "system",
            eventType = BlockingEventType.FOCUS_MODE_STARTED,
            reason = "Profile $profileId activated",
            focusProfileId = profileId
        ))
        
        Log.d(TAG, "Focus profile $profileId activated")
    }
    
    suspend fun deactivateCurrentProfile() {
        val current = _activeFocusProfile.value ?: return
        focusProfileDao.deactivateAllProfiles()
        focusProfileDao.recordCompletion(current.id)
        
        blockingEventDao.insertEvent(BlockingEvent(
            packageName = "system",
            eventType = BlockingEventType.FOCUS_MODE_ENDED,
            reason = "Profile ${current.id} deactivated",
            focusProfileId = current.id
        ))
        
        Log.d(TAG, "Focus profile ${current.id} deactivated")
    }
    
    // ═══ ALIASES/ADAPTERS para callers externos ═══
    
    /** Alias público para que los callers accedan al perfil activo. */
    val activeProfile: StateFlow<FocusProfile?> get() = _activeFocusProfile
    
    /** Registra un evento de bloqueo con firma simplificada. */
    suspend fun trackEvent(packageName: String, eventType: BlockingEventType, details: String = "") {
        recordBlockingEvent(packageName, eventType, reason = details)
    }
    
    /** Obtiene la etapa gradual actual basándose en el stage map. */
    fun getCurrentGradualStage(packageName: String): GradualBlockingStage? {
        return _currentGradualStage.value[packageName]
    }
    
    /** Obtiene una intervención inteligente para la app (la más severa pendiente). */
    suspend fun getSmartIntervention(packageName: String): SmartIntervention? {
        val interventions = generateInterventions(packageName, 10)
        return interventions.maxByOrNull { it.severity }
    }
    
    /** Alias para getDailyStats(). */
    suspend fun getDailyBlockingStats(): DailyBlockingStats = getDailyStats()
    
    /** Verifica si un tipo de contenido debe bloquearse (delega a shouldBlockContent). */
    fun shouldBlockContentType(packageName: String, contentType: AdvancedDetectionEngine.ContentType): Boolean {
        return shouldBlockContent(contentType)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // VERIFICACIONES DE BLOQUEO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Verifica si una app debería estar bloqueada según el perfil activo.
     */
    fun shouldBlockApp(packageName: String): Boolean {
        val profile = _activeFocusProfile.value ?: return false
        
        // Siempre permitir apps en whitelist del perfil
        if (packageName in profile.getAllowedAppsList()) return false
        
        // Bloquear todo
        if (profile.blockAllApps) return true
        
        // Bloquear apps específicas
        if (packageName in profile.getBlockedAppsList()) return true
        
        return false
    }
    
    /**
     * Verifica si un tipo de contenido debería estar bloqueado según el perfil activo.
     */
    fun shouldBlockContent(contentType: AdvancedDetectionEngine.ContentType): Boolean {
        val profile = _activeFocusProfile.value ?: return false
        
        return when (contentType) {
            AdvancedDetectionEngine.ContentType.SHORT_VIDEO -> profile.blockShortVideos
            AdvancedDetectionEngine.ContentType.EXPLORE_FEED -> profile.blockExplore
            AdvancedDetectionEngine.ContentType.SEARCH_RESULTS -> profile.blockExplore
            AdvancedDetectionEngine.ContentType.STORIES -> profile.blockStories
            AdvancedDetectionEngine.ContentType.MAIN_FEED -> profile.blockFeeds
            AdvancedDetectionEngine.ContentType.LIVE_STREAM -> profile.blockLiveStreams
            AdvancedDetectionEngine.ContentType.MESSAGING -> !profile.allowMessaging
            AdvancedDetectionEngine.ContentType.SHOPPING -> profile.blockShopping
            AdvancedDetectionEngine.ContentType.GAMING -> profile.blockGaming
            AdvancedDetectionEngine.ContentType.PROFILE -> false
            AdvancedDetectionEngine.ContentType.SETTINGS -> false
            AdvancedDetectionEngine.ContentType.NOTIFICATIONS -> false
            AdvancedDetectionEngine.ContentType.UNKNOWN -> false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // REGISTRO DE EVENTOS Y ANALYTICS
    // ═══════════════════════════════════════════════════════════════
    
    suspend fun recordBlockingEvent(
        packageName: String,
        eventType: BlockingEventType,
        reason: String = "",
        confidence: Float = 0f,
        contentType: String = "",
        ruleId: String? = null
    ) {
        val profile = _activeFocusProfile.value
        blockingEventDao.insertEvent(BlockingEvent(
            packageName = packageName,
            eventType = eventType,
            reason = reason,
            detectionConfidence = confidence,
            contentType = contentType,
            focusProfileId = profile?.id,
            ruleId = ruleId
        ))
    }
    
    suspend fun recordOverride(packageName: String, method: String) {
        blockingEventDao.insertEvent(BlockingEvent(
            packageName = packageName,
            eventType = BlockingEventType.BLOCK_OVERRIDDEN,
            wasOverridden = true,
            overrideMethod = method,
            focusProfileId = _activeFocusProfile.value?.id
        ))
    }
    
    suspend fun getDailyStats(): DailyBlockingStats {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val events = blockingEventDao.getRecentEvents(todayStart, 500)
        val blockedCount = events.count { 
            it.eventType in listOf(
                BlockingEventType.APP_BLOCKED, 
                BlockingEventType.FEATURE_BLOCKED,
                BlockingEventType.WEBSITE_BLOCKED
            )
        }
        val overrideCount = events.count { it.eventType == BlockingEventType.BLOCK_OVERRIDDEN }
        val uniqueApps = events.filter { 
            it.eventType in listOf(BlockingEventType.APP_BLOCKED, BlockingEventType.FEATURE_BLOCKED) 
        }.map { it.packageName }.distinct().size
        
        val mostBlocked = blockingEventDao.getMostBlockedApps(todayStart)
        
        val stats = DailyBlockingStats(
            totalBlocks = blockedCount,
            totalOverrides = overrideCount,
            uniqueAppsBlocked = uniqueApps,
            resistanceRate = if (blockedCount > 0) 1f - (overrideCount.toFloat() / blockedCount) else 1f,
            mostBlockedApp = mostBlocked.firstOrNull()?.packageName,
            focusProfileCompletions = events.count { it.eventType == BlockingEventType.FOCUS_MODE_ENDED },
            focusProfileBreaks = events.count { it.eventType == BlockingEventType.FOCUS_MODE_BROKEN },
            estimatedTimeSaved = blockedCount * 8 // Promedio de 8 min ahorrados por bloqueo
        )
        
        _dailyStats.value = stats
        return stats
    }
    
    /**
     * Verifica automáticamente si algún perfil debería activarse por horario.
     */
    suspend fun checkAutoActivation() {
        val profiles = focusProfileDao.getAllProfiles().first()
        val currentActive = _activeFocusProfile.value
        
        for (profile in profiles) {
            if (profile.autoActivate && profile.isScheduleActive()) {
                if (currentActive?.id != profile.id) {
                    activateProfile(profile.id)
                    Log.d(TAG, "Auto-activated profile: ${profile.name}")
                }
                return
            }
        }
        
        // Si ningún perfil programado está activo, desactivar el actual si fue auto-activado
        if (currentActive?.autoActivate == true && !currentActive.isScheduleActive()) {
            deactivateCurrentProfile()
            Log.d(TAG, "Auto-deactivated profile: ${currentActive.name}")
        }
    }
}

data class DailyBlockingStats(
    val totalBlocks: Int = 0,
    val totalOverrides: Int = 0,
    val uniqueAppsBlocked: Int = 0,
    val resistanceRate: Float = 1f,          // 0-1, qué % de bloqueos fueron respetados
    val mostBlockedApp: String? = null,
    val focusProfileCompletions: Int = 0,
    val focusProfileBreaks: Int = 0,
    val estimatedTimeSaved: Int = 0          // Minutos estimados ahorrados
)

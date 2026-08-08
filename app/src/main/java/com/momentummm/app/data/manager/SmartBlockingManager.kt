package com.momentummm.app.data.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.momentummm.app.data.dao.ContextBlockRuleDao
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.engine.AdaptiveBlockingManager
import com.momentummm.app.data.engine.BlockingEventType
import com.momentummm.app.data.engine.UsageAnalyticsEngine
import com.momentummm.app.data.engine.UsagePatternEngine
import com.momentummm.app.data.engine.RiskLevel
import com.momentummm.app.data.entity.BlockType
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.data.usage.DailyUsageCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SMART BLOCKING MANAGER V2 - Orquestador Central Mejorado
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Manager central para todas las características de bloqueo inteligente:
 * - Ventana de sueño
 * - Ayuno intermitente digital
 * - Modo nuclear
 * - Bloqueo por contexto
 * - Protección de rachas
 * - Timer flotante
 * - Modo solo comunicación
 * 
 * NUEVO V2:
 * - Integración con UsagePatternEngine (predicciones ML-like)
 * - Integración con AdaptiveBlockingManager (perfiles de enfoque + bloqueo gradual)
 * - Integración con UsageAnalyticsEngine (insights y reportes)
 * - Límites inteligentes basados en patrones de uso
 * - Auto-activación de perfiles de enfoque
 * - Análisis periódico de patrones
 */
@Singleton
class SmartBlockingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configDao: SmartBlockingConfigDao,
    private val contextRuleDao: ContextBlockRuleDao,
    private val inAppBlockRuleDao: InAppBlockRuleDao,
    private val patternEngine: UsagePatternEngine,
    private val adaptiveBlockingManager: AdaptiveBlockingManager,
    private val analyticsEngine: UsageAnalyticsEngine
) {
    private val TAG = "SmartBlockingManager"
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e(TAG, "Coroutine exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    
    // Estados observables
    private val _config = MutableStateFlow(SmartBlockingConfig.DEFAULT)
    val config: StateFlow<SmartBlockingConfig> = _config.asStateFlow()
    
    private val _isInSleepMode = MutableStateFlow(false)
    val isInSleepMode: StateFlow<Boolean> = _isInSleepMode.asStateFlow()
    
    private val _isInFastingMode = MutableStateFlow(false)
    val isInFastingMode: StateFlow<Boolean> = _isInFastingMode.asStateFlow()
    
    private val _isNuclearModeActive = MutableStateFlow(false)
    val isNuclearModeActive: StateFlow<Boolean> = _isNuclearModeActive.asStateFlow()
    
    private val _nuclearModeProgress = MutableStateFlow(0f) // 0.0 - 1.0
    val nuclearModeProgress: StateFlow<Float> = _nuclearModeProgress.asStateFlow()
    
    private val _activeContextRules = MutableStateFlow<List<ContextBlockRule>>(emptyList())
    val activeContextRules: StateFlow<List<ContextBlockRule>> = _activeContextRules.asStateFlow()
    
    // Context rules flow
    val contextRules: Flow<List<ContextBlockRule>> = contextRuleDao.getAllRules()
    
    // ================== TRACKING DE APPS BLOQUEADAS HOY ==================
    // CRITICAL FIX: Ahora usa SharedPreferences para PERSISTIR el estado
    // Antes usaba solo MutableStateFlow que se perdía al reiniciar el servicio
    
    private val blockedAppsPrefs: SharedPreferences = 
        context.getSharedPreferences("blocked_apps_today", Context.MODE_PRIVATE)

    /**
     * Clave nueva: guarda el INSTANTE en que empezó el día vigente en lugar del
     * antiguo `blocked_day` (día del año). Al cambiar de clave, la primera
     * ejecución tras actualizar no encuentra valor y limpia el estado, que es
     * justo lo que interesa.
     */
    private val KEY_BLOCKED_DAY_START = "blocked_day_start_millis"
    
    private val _blockedAppsToday = MutableStateFlow<Map<String, Long>>(emptyMap())
    val blockedAppsToday: StateFlow<Map<String, Long>> = _blockedAppsToday.asStateFlow()
    
    // Guarda la última vez que se mostró la pantalla de bloqueo para cada app
    // Usar ConcurrentHashMap para evitar ConcurrentModificationException
    private val lastBlockScreenShownTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val BLOCK_SCREEN_COOLDOWN = 2000L // 2 segundos mínimo entre pantallas de bloqueo (reducido de 3s)
    
    // Inicio del "día" vigente, para resetear al cruzar el límite del día.
    // Se usa la misma referencia que DailyUsageCalculator (hora de inicio de
    // día configurable), no medianoche.
    @Volatile
    private var currentBlockDayStart: Long = 0L
    
    init {
        // CRITICAL FIX: Cargar apps bloqueadas desde storage persistente al iniciar
        loadBlockedAppsFromDisk()
        
        scope.launch {
            // Inicializar configuración
            initializeConfig()
            
            // Observar cambios en la configuración
            configDao.getConfig().collect { dbConfig ->
                val currentConfig = dbConfig ?: SmartBlockingConfig.DEFAULT
                _config.value = currentConfig
                updateModeStates(currentConfig)
            }
        }
    }
    
    /**
     * Carga las apps bloqueadas desde SharedPreferences.
     * Esto asegura que si el servicio se reinicia, no pierde el estado de bloqueo.
     *
     * BUG CORREGIDO: el "día" se identificaba con `Calendar.DAY_OF_YEAR`, es
     * decir medianoche, mientras que el uso diario se calcula desde la hora de
     * inicio de día configurable del usuario ([DailyUsageCalculator]). Con un
     * inicio de día a las 4:00 las dos fechas discrepaban durante horas: el uso
     * se reseteaba a las 4:00 pero las apps seguían marcadas como bloqueadas
     * hasta medianoche, así que el usuario se quedaba sin acceso un día entero
     * con el contador a cero.
     */
    private fun loadBlockedAppsFromDisk() {
        try {
            val savedDay = blockedAppsPrefs.getLong(KEY_BLOCKED_DAY_START, -1L)
            val today = currentDayStart()

            if (savedDay == today) {
                // Cargar apps bloqueadas del día actual
                val blockedSet = blockedAppsPrefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
                val blockedMap = mutableMapOf<String, Long>()
                for (pkg in blockedSet) {
                    val timestamp = blockedAppsPrefs.getLong("blocked_time_$pkg", System.currentTimeMillis())
                    blockedMap[pkg] = timestamp
                }
                _blockedAppsToday.value = blockedMap
                Log.d(TAG, "Loaded ${blockedMap.size} blocked apps from disk for today")
            } else {
                // Es un nuevo día, limpiar
                clearBlockedAppsFromDisk()
                Log.d(TAG, "New day detected, cleared blocked apps from disk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading blocked apps from disk", e)
        }
    }
    
    /**
     * Guarda las apps bloqueadas en SharedPreferences.
     */
    private fun saveBlockedAppsToDisk() {
        try {
            val today = currentDayStart()
            val currentBlocked = _blockedAppsToday.value
            
            blockedAppsPrefs.edit().apply {
                putLong(KEY_BLOCKED_DAY_START, today)
                putStringSet("blocked_packages", currentBlocked.keys.toSet())
                for ((pkg, timestamp) in currentBlocked) {
                    putLong("blocked_time_$pkg", timestamp)
                }
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving blocked apps to disk", e)
        }
    }

    /**
     * Instante en que empezó el "día" vigente, según la hora de inicio de día
     * del usuario. Es la MISMA referencia que usa [DailyUsageCalculator] para
     * sumar el uso, así que el reseteo del bloqueo y el del contador ocurren a
     * la vez.
     */
    private fun currentDayStart(): Long =
        runCatching { DailyUsageCalculator.dayStartMillis(context) }
            .getOrElse {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
    
    /**
     * Limpia las apps bloqueadas del disco.
     */
    private fun clearBlockedAppsFromDisk() {
        try {
            blockedAppsPrefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing blocked apps from disk", e)
        }
    }
    
    private suspend fun initializeConfig() {
        val existing = configDao.getConfigSync()
        if (existing == null) {
            configDao.insertConfig(SmartBlockingConfig.DEFAULT)
            Log.d(TAG, "Initialized default SmartBlockingConfig")
        }
    }
    
    private fun updateModeStates(config: SmartBlockingConfig) {
        _isInSleepMode.value = config.isInSleepHours()
        _isInFastingMode.value = config.isInFastingHours()
        _isNuclearModeActive.value = config.isNuclearModeActive()
        
        if (config.isNuclearModeActive()) {
            val totalSeconds = config.nuclearModeUnlockWaitMinutes * 60
            val currentSeconds = config.nuclearModeCurrentWaitSeconds
            // Protección contra división por cero
            _nuclearModeProgress.value = if (totalSeconds > 0) {
                currentSeconds.toFloat() / totalSeconds.toFloat()
            } else 0f
        }
    }
    
    /**
     * Actualiza el estado de los modos (llamar periódicamente)
     * V2: También ejecuta análisis de patrones y auto-activación de perfiles
     */
    fun refreshModeStates() {
        scope.launch {
            // Se usa la config YA cacheada en _config (el colector de init la
            // mantiene fresca vía configDao.getConfig()) en lugar de volver a leer
            // de disco en cada tick del monitor. updateModeStates solo recalcula
            // ventanas de tiempo (sueño/ayuno/nuclear) contra el reloj, así que el
            // valor cacheado es suficiente y se ahorra una lectura Room por ciclo.
            val currentConfig = _config.value
            updateModeStates(currentConfig)
            
            // Verificar si cambió el día para resetear apps bloqueadas
            val today = currentDayStart()
            if (currentBlockDayStart == 0L) {
                // Primera pasada: sólo se toma la referencia. El estado ya se
                // cargó (y depuró) desde disco en el init.
                currentBlockDayStart = today
            } else if (today != currentBlockDayStart) {
                resetBlockedAppsForNewDay()
                currentBlockDayStart = today
                DailyUsageCalculator.invalidate()
                
                // === NUEVO V2: Análisis diario de patrones ===
                scope.launch {
                    try {
                        patternEngine.analyzeAllPatterns()
                        analyticsEngine.generateInsights()
                        Log.d(TAG, "✅ Análisis diario de patrones completado")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en análisis diario", e)
                    }
                }
            }
            
            // Actualizar reglas de contexto activas
            val rules = contextRuleDao.getEnabledRulesSync()
            _activeContextRules.value = rules.filter { 
                when (it.contextType) {
                    "SCHEDULE" -> it.isActiveBySchedule()
                    else -> false
                }
            }
            
            // Resetear días de gracia si es nueva semana
            checkAndResetGraceDays(currentConfig)
            
            // === NUEVO V2: Auto-activación de perfiles de enfoque ===
            try {
                adaptiveBlockingManager.checkAutoActivation()
            } catch (e: Exception) {
                Log.e(TAG, "Error en auto-activación de perfiles", e)
            }
        }
    }
    
    // ================== NUEVO V2: MÉTODOS DE ENGINES ==================
    
    /**
     * Registra uso de una app (llamar desde AppMonitoringService)
     */
    suspend fun recordAppUsage(packageName: String, durationMinutes: Int) {
        patternEngine.recordUsage(packageName, durationMinutes)
    }
    
    /**
     * Verifica si la predicción indica que el usuario excederá su límite
     */
    suspend fun willExceedLimit(packageName: String, currentUsageMinutes: Int, limitMinutes: Int): Boolean {
        val (willExceed, _) = patternEngine.predictWillExceedToday(packageName, currentUsageMinutes, limitMinutes)
        return willExceed
    }
    
    /**
     * Obtiene las horas de mayor riesgo para una app (hora, score de riesgo)
     */
    suspend fun getPeakRiskHours(packageName: String): List<Pair<Int, Float>> {
        return patternEngine.getPeakRiskHours(packageName)
    }
    
    /**
     * Obtiene la intervención inteligente actual (si hay alguna)
     * CRITICAL FIX: Ahora acepta el uso real en vez de hardcodear 10 minutos.
     * Antes, intervenciones que requerían >10 min nunca se activaban.
     */
    suspend fun getSmartIntervention(packageName: String, currentUsageMinutes: Int = 10): com.momentummm.app.data.engine.SmartIntervention? {
        val interventions = adaptiveBlockingManager.generateInterventions(packageName, currentUsageMinutes)
        return interventions.maxByOrNull { it.severity }
    }
    
    /**
     * Obtiene las stats de bloqueo del día
     */
    suspend fun getDailyBlockingStats(): com.momentummm.app.data.engine.DailyBlockingStats {
        return adaptiveBlockingManager.getDailyStats()
    }
    
    /**
     * Genera el reporte semanal completo
     */
    suspend fun generateWeeklyReport(): com.momentummm.app.data.engine.UsageAnalyticsEngine.WeeklyReport {
        return analyticsEngine.generateWeeklyReport()
    }
    
    /**
     * Obtiene los insights actuales
     */
    suspend fun getInsights(): List<com.momentummm.app.data.engine.UsageAnalyticsEngine.UsageInsight> {
        return analyticsEngine.generateInsights()
    }
    
    /**
     * Obtiene el score de bienestar digital
     */
    fun getDigitalWellbeingScore(): Float {
        return analyticsEngine.digitalWellbeingScore.value
    }
    
    /**
     * Registra un evento de bloqueo para analytics
     */
    suspend fun trackBlockingEvent(
        packageName: String, 
        eventType: BlockingEventType,
        details: String = ""
    ) {
        adaptiveBlockingManager.recordBlockingEvent(packageName, eventType, reason = details)
    }
    
    // ================== GESTIÓN DE APPS BLOQUEADAS ==================
    
    /**
     * Marca una app como bloqueada hoy (alcanzó su límite)
     * CRITICAL FIX: Ahora persiste en SharedPreferences
     */
    fun markAppAsBlocked(packageName: String) {
        _blockedAppsToday.update { currentMap ->
            currentMap + (packageName to System.currentTimeMillis())
        }
        saveBlockedAppsToDisk() // Persistir inmediatamente
        Log.d(TAG, "App marcada como bloqueada hoy: $packageName (persistido en disco)")
    }
    
    /**
     * Verifica si una app está bloqueada hoy (ya alcanzó su límite)
     */
    fun isAppBlockedToday(packageName: String): Boolean {
        return _blockedAppsToday.value.containsKey(packageName)
    }
    
    /**
     * Verifica si se puede mostrar la pantalla de bloqueo (evita spam de pantallas)
     * Retorna true si ya pasó el cooldown desde la última vez que se mostró
     */
    fun canShowBlockScreen(packageName: String): Boolean {
        val lastShown = lastBlockScreenShownTime[packageName] ?: 0L
        val now = System.currentTimeMillis()
        return (now - lastShown) > BLOCK_SCREEN_COOLDOWN
    }
    
    /**
     * Registra que se mostró la pantalla de bloqueo para una app
     */
    fun registerBlockScreenShown(packageName: String) {
        lastBlockScreenShownTime[packageName] = System.currentTimeMillis()
    }
    
    /**
     * Desbloquea una app temporalmente (por pago o shame share)
     * CRITICAL FIX: Ahora persiste en SharedPreferences
     */
    fun temporarilyUnblockApp(packageName: String) {
        _blockedAppsToday.update { currentMap ->
            currentMap - packageName
        }
        lastBlockScreenShownTime.remove(packageName)
        saveBlockedAppsToDisk() // Persistir el cambio
        Log.d(TAG, "App desbloqueada temporalmente: $packageName (persistido en disco)")
    }
    
    /**
     * Resetea todas las apps bloqueadas (para nuevo día)
     * CRITICAL FIX: También limpia el disco
     */
    private fun resetBlockedAppsForNewDay() {
        _blockedAppsToday.value = emptyMap()
        lastBlockScreenShownTime.clear()
        clearBlockedAppsFromDisk()
        Log.d(TAG, "Apps bloqueadas reseteadas para nuevo día (disco limpiado)")
    }
    
    private suspend fun checkAndResetGraceDays(config: SmartBlockingConfig) {
        val lastReset = config.lastGraceDayResetDate
        val now = Calendar.getInstance()
        
        if (lastReset == null || shouldResetGraceDays(lastReset, now)) {
            configDao.updateGraceDaysUsed(0, now.time)
            Log.d(TAG, "Reset grace days for new week")
        }
    }
    
    private fun shouldResetGraceDays(lastReset: Date, now: Calendar): Boolean {
        val lastResetCal = Calendar.getInstance().apply { time = lastReset }
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val lastResetWeek = lastResetCal.get(Calendar.WEEK_OF_YEAR)
        return currentWeek != lastResetWeek || now.get(Calendar.YEAR) != lastResetCal.get(Calendar.YEAR)
    }
    
    // ================== VENTANA DE SUEÑO ==================
    
    suspend fun setSleepModeEnabled(enabled: Boolean) {
        configDao.setSleepModeEnabled(enabled)
        Log.d(TAG, "Sleep mode enabled: $enabled")
    }
    
    suspend fun setSleepSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        configDao.setSleepStartTime(startHour, startMinute)
        configDao.setSleepEndTime(endHour, endMinute)
        Log.d(TAG, "Sleep schedule set: $startHour:$startMinute - $endHour:$endMinute")
    }
    
    fun shouldIgnoreUsageTracking(): Boolean {
        val currentConfig = _config.value
        return currentConfig.sleepModeEnabled && 
               currentConfig.sleepModeIgnoreTracking && 
               currentConfig.isInSleepHours()
    }
    
    // ================== AYUNO INTERMITENTE DIGITAL ==================
    
    suspend fun setDigitalFastingEnabled(enabled: Boolean) {
        configDao.setDigitalFastingEnabled(enabled)
        Log.d(TAG, "Digital fasting enabled: $enabled")
    }
    
    suspend fun setFastingSchedule(
        startHour: Int, 
        startMinute: Int, 
        endHour: Int, 
        endMinute: Int,
        dailyLimitMinutes: Int,
        daysOfWeek: List<Int>
    ) {
        configDao.setFastingStartTime(startHour, startMinute)
        configDao.setFastingEndTime(endHour, endMinute)
        configDao.setFastingDailyLimit(dailyLimitMinutes)
        configDao.setFastingDays(daysOfWeek.joinToString(","))
        Log.d(TAG, "Fasting schedule updated")
    }
    
    /**
     * Obtiene el límite efectivo considerando ayuno, reglas de contexto Y patrones de uso
     */
    suspend fun getEffectiveDailyLimit(packageName: String, originalLimitMinutes: Int): Int {
        val currentConfig = _config.value
        var effectiveLimit = originalLimitMinutes
        
        // === NUEVO V2: Límite inteligente basado en patrones ===
        val smartLimit = patternEngine.calculateSmartLimit(packageName, originalLimitMinutes)
        if (smartLimit < effectiveLimit) {
            effectiveLimit = smartLimit
            Log.d(TAG, "⚡ Límite inteligente aplicado para $packageName: $smartLimit (original: $originalLimitMinutes)")
        }
        
        // === NUEVO V2: Límite del perfil de enfoque activo ===
        val focusProfile = adaptiveBlockingManager.activeFocusProfile.value
        if (focusProfile != null) {
            val profileLimit = focusProfile.getTimeLimitForApp(packageName)
            if (profileLimit != null && profileLimit < effectiveLimit) {
                effectiveLimit = profileLimit
                Log.d(TAG, "🎯 Límite de perfil '${focusProfile.name}' para $packageName: $profileLimit")
            }
        }
        
        // Aplicar límite de ayuno si está activo
        if (currentConfig.isInFastingHours()) {
            effectiveLimit = minOf(effectiveLimit, currentConfig.fastingDailyLimitMinutes)
        }
        
        // Aplicar reglas de contexto activas
        for (rule in _activeContextRules.value) {
            if (rule.blockCompletely) {
                return 0 // Bloqueo total
            }
            if (rule.overrideDailyLimit) {
                if (rule.applyToAllLimitedApps || rule.getAffectedAppsList().contains(packageName)) {
                    effectiveLimit = minOf(effectiveLimit, rule.contextDailyLimitMinutes)
                }
            }
        }
        
        return effectiveLimit
    }
    
    // ================== MODO NUCLEAR ==================
    
    suspend fun activateNuclearMode(
        durationDays: Int, 
        targetApps: List<String>,
        unlockWaitMinutes: Int = 30
    ) {
        val startDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.DAY_OF_YEAR, durationDays)
        val endDate = calendar.time
        
        val currentConfig = configDao.getConfigSync() ?: SmartBlockingConfig.DEFAULT
        val updatedConfig = currentConfig.copy(
            nuclearModeEnabled = true,
            nuclearModeStartDate = startDate,
            nuclearModeEndDate = endDate,
            nuclearModeDurationDays = durationDays,
            nuclearModeApps = targetApps.joinToString(","),
            nuclearModeUnlockWaitMinutes = unlockWaitMinutes,
            nuclearModeCurrentWaitSeconds = 0,
            updatedAt = System.currentTimeMillis()
        )
        configDao.updateConfig(updatedConfig)
        
        Log.d(TAG, "Nuclear mode activated for $durationDays days, ${targetApps.size} apps")
    }
    
    suspend fun deactivateNuclearMode() {
        configDao.setNuclearMode(false, null, null, 0, "")
        Log.d(TAG, "Nuclear mode deactivated")
    }
    
    /**
     * Actualiza el progreso del timer de desbloqueo nuclear
     * Solo debe llamarse cuando la app está en primer plano
     */
    suspend fun updateNuclearWaitProgress(secondsToAdd: Int): Boolean {
        val currentConfig = configDao.getConfigSync() ?: return false
        
        if (!currentConfig.isNuclearModeActive()) return false
        if (!currentConfig.nuclearModeRequiresAppOpen) return false
        
        val newSeconds = currentConfig.nuclearModeCurrentWaitSeconds + secondsToAdd
        val requiredSeconds = currentConfig.nuclearModeUnlockWaitMinutes * 60
        
        configDao.updateNuclearWaitProgress(newSeconds)
        
        // Verificar si se completó el tiempo de espera
        if (newSeconds >= requiredSeconds) {
            Log.d(TAG, "Nuclear mode unlock wait completed!")
            return true
        }
        
        return false
    }
    
    fun isAppInNuclearMode(packageName: String): Boolean {
        val currentConfig = _config.value
        if (!currentConfig.isNuclearModeActive()) return false
        return currentConfig.getNuclearModeAppsList().contains(packageName)
    }
    
    fun getNuclearModeRemainingDays(): Int {
        val currentConfig = _config.value
        val endDate = currentConfig.nuclearModeEndDate ?: return 0
        val now = Date()
        val diff = endDate.time - now.time
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
    
    // ================== PROTECCIÓN DE RACHAS ==================
    
    suspend fun setStreakProtectionEnabled(enabled: Boolean) {
        configDao.setStreakProtectionEnabled(enabled)
    }
    
    suspend fun setGraceDaysPerWeek(days: Int) {
        configDao.setGraceDaysPerWeek(days.coerceIn(0, 3))
    }
    
    suspend fun useGraceDay(): Boolean {
        val currentConfig = configDao.getConfigSync() ?: return false
        
        if (!currentConfig.streakProtectionEnabled) return false
        if (!currentConfig.hasGraceDaysAvailable()) return false
        
        configDao.updateGraceDaysUsed(
            currentConfig.graceDaysUsedThisWeek + 1,
            currentConfig.lastGraceDayResetDate ?: Date()
        )
        
        Log.d(TAG, "Grace day used. Remaining: ${currentConfig.graceDaysPerWeek - currentConfig.graceDaysUsedThisWeek - 1}")
        return true
    }
    
    fun getGraceDaysRemaining(): Int {
        val currentConfig = _config.value
        return (currentConfig.graceDaysPerWeek - currentConfig.graceDaysUsedThisWeek).coerceAtLeast(0)
    }
    
    /**
     * Verifica si el usuario está cerca de romper su racha
     */
    fun shouldWarnAboutStreakBreak(currentUsageMinutes: Int, limitMinutes: Int): Boolean {
        val currentConfig = _config.value
        if (!currentConfig.warningBeforeStreakBreak) return false
        
        val remainingMinutes = limitMinutes - currentUsageMinutes
        return remainingMinutes in 1..currentConfig.warningMinutesBeforeLimit
    }
    
    // ================== TIMER FLOTANTE ==================
    
    suspend fun setFloatingTimerEnabled(enabled: Boolean) {
        configDao.setFloatingTimerEnabled(enabled)
    }
    
    suspend fun setFloatingTimerOpacity(opacity: Float) {
        configDao.setFloatingTimerOpacity(opacity.coerceIn(0.3f, 1f))
    }
    
    suspend fun setFloatingTimerPosition(position: String) {
        configDao.setFloatingTimerPosition(position)
    }
    
    suspend fun setFloatingTimerSize(size: String) {
        configDao.setFloatingTimerSize(size)
    }
    
    fun isFloatingTimerEnabled(): Boolean = _config.value.floatingTimerEnabled
    
    // ================== MODO SOLO COMUNICACIÓN ==================
    
    suspend fun setCommunicationOnlyMode(enabled: Boolean, apps: List<String>) {
        configDao.setCommunicationOnlyMode(enabled, apps.joinToString(","))
        Log.d(TAG, "Communication only mode: $enabled for ${apps.size} apps")
        
        // Sincronizar con las reglas de bloqueo in-app
        syncCommunicationOnlyRules(enabled, apps)
    }
    
    /**
     * Sincroniza las reglas de bloqueo in-app según la configuración de modo solo comunicación
     */
    private suspend fun syncCommunicationOnlyRules(enabled: Boolean, apps: List<String>) {
        val currentConfig = _config.value
        
        // Mapeo de paquetes a sus ruleIds para bloqueo de funciones
        val appRulesMapping = mapOf(
            "com.instagram.android" to listOf("instagram_reels", "instagram_explore"),
            "com.google.android.youtube" to listOf("youtube_shorts", "youtube_search"),
            "com.facebook.katana" to listOf("facebook_reels"),
            "com.snapchat.android" to listOf("snapchat_discover"),
            "com.zhiliaoapp.musically" to listOf("tiktok_foryou"), // TikTok
            "com.ss.android.ugc.trill" to listOf("tiktok_foryou"), // TikTok (alternativo)
            "com.twitter.android" to listOf("x_explore"),
            "com.x.android" to listOf("x_explore") // X (nuevo nombre de Twitter)
        )
        
        for ((packageName, ruleIds) in appRulesMapping) {
            val shouldBlock = enabled && apps.contains(packageName)
            
            for (ruleId in ruleIds) {
                // Habilitar o deshabilitar las reglas según la configuración
                if (shouldBlock) {
                    // Aplicar configuración de qué bloquear
                    val shouldEnableRule = when {
                        ruleId.contains("reels") || ruleId.contains("shorts") -> currentConfig.communicationOnlyBlockReels
                        ruleId.contains("explore") || ruleId.contains("discover") || ruleId.contains("foryou") -> currentConfig.communicationOnlyBlockFeed
                        ruleId.contains("search") -> currentConfig.communicationOnlyBlockFeed
                        else -> true
                    }
                    
                    if (shouldEnableRule) {
                        inAppBlockRuleDao.updateRuleEnabled(ruleId, true)
                        Log.d(TAG, "Enabled in-app block rule: $ruleId")
                    }
                } else {
                    // Deshabilitar reglas cuando el modo está desactivado
                    inAppBlockRuleDao.updateRuleEnabled(ruleId, false)
                    Log.d(TAG, "Disabled in-app block rule: $ruleId")
                }
            }
        }
    }
    
    /**
     * Actualiza las opciones de bloqueo de contenido específico
     */
    suspend fun updateCommunicationOnlyOptions(
        blockFeed: Boolean? = null,
        blockStories: Boolean? = null,
        blockReels: Boolean? = null,
        allowDMs: Boolean? = null
    ) {
        val currentConfig = _config.value
        val apps = currentConfig.getCommunicationOnlyAppsList()
        
        // Actualizar opciones en la base de datos
        blockFeed?.let {
            configDao.updateConfig(currentConfig.copy(communicationOnlyBlockFeed = it))
        }
        blockStories?.let {
            configDao.updateConfig(currentConfig.copy(communicationOnlyBlockStories = it))
        }
        blockReels?.let {
            configDao.updateConfig(currentConfig.copy(communicationOnlyBlockReels = it))
        }
        allowDMs?.let {
            configDao.updateConfig(currentConfig.copy(communicationOnlyAllowDMs = it))
        }
        
        // Re-sincronizar reglas si el modo está activo
        if (currentConfig.communicationOnlyModeEnabled) {
            syncCommunicationOnlyRules(true, apps)
        }
    }
    
    fun isAppInCommunicationOnlyMode(packageName: String): Boolean {
        val currentConfig = _config.value
        if (!currentConfig.communicationOnlyModeEnabled) return false
        return currentConfig.getCommunicationOnlyAppsList().contains(packageName)
    }
    
    fun isCommunicationOnlyModeActive(): Boolean {
        return _config.value.communicationOnlyModeEnabled
    }
    
    fun getCommunicationOnlyAllowedApps(): List<String> {
        return _config.value.getCommunicationOnlyAppsList()
    }
    
    // ================== REGLAS DE CONTEXTO ==================
    
    suspend fun addContextRule(rule: ContextBlockRule): Long {
        return contextRuleDao.insertRule(rule)
    }
    
    suspend fun updateContextRule(rule: ContextBlockRule) {
        contextRuleDao.updateRule(rule)
    }
    
    suspend fun deleteContextRule(ruleId: Int) {
        contextRuleDao.deleteRuleById(ruleId)
    }
    
    suspend fun setContextRuleEnabled(ruleId: Int, enabled: Boolean) {
        contextRuleDao.setRuleEnabled(ruleId, enabled)
    }
    
    /**
     * Verifica si una app está bloqueada por alguna regla de contexto activa
     */
    fun isAppBlockedByContext(packageName: String): Boolean {
        for (rule in _activeContextRules.value) {
            if (rule.blockCompletely) {
                if (rule.applyToAllLimitedApps || rule.getAffectedAppsList().contains(packageName)) {
                    return true
                }
            }
        }
        return false
    }
}

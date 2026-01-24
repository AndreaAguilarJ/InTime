package com.momentummm.app.data.manager

import android.content.Context
import android.util.Log
import com.momentummm.app.data.dao.ContextBlockRuleDao
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.entity.BlockType
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager central para todas las características de bloqueo inteligente:
 * - Ventana de sueño
 * - Ayuno intermitente digital
 * - Modo nuclear
 * - Bloqueo por contexto
 * - Protección de rachas
 * - Timer flotante
 * - Modo solo comunicación
 */
@Singleton
class SmartBlockingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configDao: SmartBlockingConfigDao,
    private val contextRuleDao: ContextBlockRuleDao,
    private val inAppBlockRuleDao: InAppBlockRuleDao
) {
    private val TAG = "SmartBlockingManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
    
    init {
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
            _nuclearModeProgress.value = currentSeconds.toFloat() / totalSeconds.toFloat()
        }
    }
    
    /**
     * Actualiza el estado de los modos (llamar periódicamente)
     */
    fun refreshModeStates() {
        scope.launch {
            val currentConfig = configDao.getConfigSync() ?: return@launch
            updateModeStates(currentConfig)
            
            // Actualizar reglas de contexto activas
            val rules = contextRuleDao.getEnabledRulesSync()
            _activeContextRules.value = rules.filter { 
                when (it.contextType) {
                    "SCHEDULE" -> it.isActiveBySchedule()
                    // Para LOCATION y WIFI, se necesitaría verificar con servicios de ubicación
                    else -> false
                }
            }
            
            // Resetear días de gracia si es nueva semana
            checkAndResetGraceDays(currentConfig)
        }
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
     * Obtiene el límite efectivo considerando ayuno y reglas de contexto
     */
    fun getEffectiveDailyLimit(packageName: String, originalLimitMinutes: Int): Int {
        val currentConfig = _config.value
        var effectiveLimit = originalLimitMinutes
        
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

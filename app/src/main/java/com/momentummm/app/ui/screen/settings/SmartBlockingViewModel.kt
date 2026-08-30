package com.momentummm.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.data.manager.SmartBlockingManager
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.util.ContextSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartBlockingViewModel @Inject constructor(
    private val smartBlockingManager: SmartBlockingManager,
    private val configDao: SmartBlockingConfigDao,
    private val appLimitRepository: AppLimitRepository,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    val config: StateFlow<SmartBlockingConfig> = smartBlockingManager.config
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SmartBlockingConfig.DEFAULT
        )
    
    val contextRules: StateFlow<List<ContextBlockRule>> = smartBlockingManager.contextRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ================== TIMER FLOTANTE ==================
    
    fun setFloatingTimerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setFloatingTimerEnabled(enabled)
        }
    }
    
    fun setFloatingTimerOpacity(opacity: Float) {
        viewModelScope.launch {
            smartBlockingManager.setFloatingTimerOpacity(opacity)
        }
    }
    
    fun setFloatingTimerPosition(position: String) {
        viewModelScope.launch {
            smartBlockingManager.setFloatingTimerPosition(position)
        }
    }
    
    fun setFloatingTimerSize(size: String) {
        viewModelScope.launch {
            smartBlockingManager.setFloatingTimerSize(size)
        }
    }

    // ================== VENTANA DE SUEÑO ==================
    
    fun setSleepModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setSleepModeEnabled(enabled)
        }
    }
    
    fun setSleepSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        viewModelScope.launch {
            smartBlockingManager.setSleepSchedule(startHour, startMinute, endHour, endMinute)
        }
    }
    
    fun setSleepIgnoreTracking(ignore: Boolean) {
        viewModelScope.launch {
            // Se delega en el manager, que relee la fila vigente antes de
            // escribirla. Copiar `config.value` aquí podía revertir un cambio
            // concurrente que el StateFlow aún no reflejaba.
            smartBlockingManager.setSleepIgnoreTracking(ignore)
        }
    }

    fun setSleepBlockApps(block: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setSleepBlockApps(block)
        }
    }

    // ================== AYUNO DIGITAL ==================
    
    fun setDigitalFastingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setDigitalFastingEnabled(enabled)
        }
    }
    
    fun setFastingSchedule(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        limitMinutes: Int,
        daysOfWeek: List<Int>
    ) {
        viewModelScope.launch {
            smartBlockingManager.setFastingSchedule(
                startHour, startMinute, endHour, endMinute, limitMinutes, daysOfWeek
            )
        }
    }

    // ================== MODO NUCLEAR ==================
    
    fun activateNuclearMode(durationDays: Int, targetApps: List<String>, waitMinutes: Int) {
        viewModelScope.launch {
            smartBlockingManager.activateNuclearMode(durationDays, targetApps, waitMinutes)
        }
    }

    /**
     * Pide desactivar el modo nuclear. NO lo desactiva: abre la espera.
     *
     * El interruptor llamaba antes directamente a `deactivateNuclearMode()`, lo
     * que hacía falsa la promesa de irreversibilidad de la pantalla.
     */
    fun requestNuclearUnlock() {
        viewModelScope.launch {
            smartBlockingManager.requestNuclearUnlock()
        }
    }

    fun cancelNuclearUnlockRequest() {
        viewModelScope.launch {
            smartBlockingManager.cancelNuclearUnlockRequest()
        }
    }

    fun isNuclearUnlockAvailable(): Boolean = smartBlockingManager.isNuclearUnlockAvailable()

    fun deactivateNuclearMode() {
        viewModelScope.launch {
            smartBlockingManager.deactivateNuclearMode()
        }
    }
    
    fun getNuclearModeRemainingDays(): Int {
        return smartBlockingManager.getNuclearModeRemainingDays()
    }

    /**
     * Apps instaladas que el usuario puede elegir para el Modo nuclear.
     *
     * El diálogo confirmaba con `emptyList()`, así que el modo se activaba sin
     * ninguna app objetivo y no bloqueaba absolutamente nada.
     */
    val selectableApps: StateFlow<List<Pair<String, String>>> = flow {
        val apps = runCatching { appLimitRepository.getInstallableApps() }
            .getOrElse { emptyList() }
            .map { it.packageName to it.appName }
        emit(apps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ================== PROTECCIÓN DE RACHAS ==================
    
    fun setStreakProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setStreakProtectionEnabled(enabled)
        }
    }
    
    fun setGraceDaysPerWeek(days: Int) {
        viewModelScope.launch {
            smartBlockingManager.setGraceDaysPerWeek(days)
        }
    }
    
    fun setWarningBeforeStreakBreak(enabled: Boolean) {
        viewModelScope.launch {
            val currentConfig = config.value
            val updatedConfig = currentConfig.copy(
                warningBeforeStreakBreak = enabled,
                updatedAt = System.currentTimeMillis()
            )
            configDao.updateConfig(updatedConfig)
        }
    }
    
    fun setWarningMinutesBeforeLimit(minutes: Int) {
        viewModelScope.launch {
            val currentConfig = config.value
            val updatedConfig = currentConfig.copy(
                warningMinutesBeforeLimit = minutes,
                updatedAt = System.currentTimeMillis()
            )
            configDao.updateConfig(updatedConfig)
        }
    }

    // ================== BLOQUEO POR CONTEXTO ==================
    
    fun setContextBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentConfig = config.value
            val updatedConfig = currentConfig.copy(
                contextBlockingEnabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            configDao.updateConfig(updatedConfig)
        }
    }
    
    fun addContextRule(rule: ContextBlockRule) {
        viewModelScope.launch {
            smartBlockingManager.addContextRule(rule)
        }
    }

    /**
     * Red Wi-Fi y ubicación actuales, para poder crear reglas de contexto.
     *
     * Se leen al abrir el diálogo y no de forma continua: mantener una
     * suscripción a la ubicación desde una pantalla de ajustes gastaría batería
     * para un dato que sólo se usa en el instante de crear la regla.
     */
    fun readContextSnapshot(): Pair<String?, Pair<Double, Double>?> =
        ContextSnapshot.currentSsid(appContext) to ContextSnapshot.lastKnownLocation(appContext)

    fun hasLocationPermission(): Boolean = ContextSnapshot.hasLocationPermission(appContext)
    
    fun toggleContextRule(ruleId: Int, enabled: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.setContextRuleEnabled(ruleId, enabled)
        }
    }
    
    fun deleteContextRule(ruleId: Int) {
        viewModelScope.launch {
            smartBlockingManager.deleteContextRule(ruleId)
        }
    }

    // ================== MODO SOLO COMUNICACIÓN ==================
    
    fun setCommunicationOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            val currentApps = config.value.getCommunicationOnlyAppsList()
            smartBlockingManager.setCommunicationOnlyMode(enabled, currentApps)
        }
    }
    
    fun toggleCommunicationOnlyApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentApps = config.value.getCommunicationOnlyAppsList().toMutableList()
            if (enabled && packageName !in currentApps) {
                currentApps.add(packageName)
            } else if (!enabled) {
                currentApps.remove(packageName)
            }
            smartBlockingManager.setCommunicationOnlyMode(config.value.communicationOnlyModeEnabled, currentApps)
        }
    }
    
    fun setCommunicationOnlyAllowDMs(allow: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.updateCommunicationOnlyOptions(allowDMs = allow)
        }
    }
    
    fun setCommunicationOnlyBlockFeed(block: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.updateCommunicationOnlyOptions(blockFeed = block)
        }
    }
    
    fun setCommunicationOnlyBlockStories(block: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.updateCommunicationOnlyOptions(blockStories = block)
        }
    }
    
    fun setCommunicationOnlyBlockReels(block: Boolean) {
        viewModelScope.launch {
            smartBlockingManager.updateCommunicationOnlyOptions(blockReels = block)
        }
    }
}

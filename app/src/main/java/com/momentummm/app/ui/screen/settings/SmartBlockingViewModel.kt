package com.momentummm.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.data.manager.SmartBlockingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartBlockingViewModel @Inject constructor(
    private val smartBlockingManager: SmartBlockingManager,
    private val configDao: SmartBlockingConfigDao
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
            val currentConfig = config.value
            val updatedConfig = currentConfig.copy(
                sleepModeIgnoreTracking = ignore,
                updatedAt = System.currentTimeMillis()
            )
            configDao.updateConfig(updatedConfig)
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
    
    fun deactivateNuclearMode() {
        viewModelScope.launch {
            smartBlockingManager.deactivateNuclearMode()
        }
    }
    
    fun getNuclearModeRemainingDays(): Int {
        return smartBlockingManager.getNuclearModeRemainingDays()
    }

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

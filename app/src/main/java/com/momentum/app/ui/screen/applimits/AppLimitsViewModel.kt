package com.momentummm.app.ui.screen.applimits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.AppLimit
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.service.AppMonitoringService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class AppLimitsUiState(
    val isLoading: Boolean = true,
    val appLimits: List<AppLimit> = emptyList(),
    val availableApps: List<AppUsageInfo> = emptyList(),
    val remainingTimes: Map<String, Int> = emptyMap(),
    val isMonitoringActive: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLimitsUiState())
    val uiState: StateFlow<AppLimitsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        // Colección de límites en tiempo real
        viewModelScope.launch {
            try {
                appLimitRepository.getAllLimits().collect { limits ->
                    val remainingTimes = mutableMapOf<String, Int>()
                    for (limit in limits) {
                        val remaining = appLimitRepository.getRemainingTime(limit.packageName)
                        remainingTimes[limit.packageName] = remaining
                    }
                    _uiState.value = _uiState.value.copy(
                        appLimits = limits,
                        remainingTimes = remainingTimes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }

        // Cargar apps disponibles en paralelo
        viewModelScope.launch {
            try {
                val availableApps = appLimitRepository.getInstallableApps()
                _uiState.value = _uiState.value.copy(
                    availableApps = availableApps.filter { app ->
                        _uiState.value.appLimits.none { it.packageName == app.packageName }
                    }
                )
            } catch (e: SecurityException) {
                // Android 11+ sin <queries> adecuado puede causar visibilidad limitada; no cerrar app
                _uiState.value = _uiState.value.copy(availableApps = emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }

        // Verificar estado del monitoreo
        viewModelScope.launch {
            updateMonitoringStatus()
        }
    }

    fun addAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            try {
                appLimitRepository.addAppLimit(packageName, appName, limitMinutes)

                // Actualizar la lista de apps disponibles
                val currentAvailable = _uiState.value.availableApps
                _uiState.value = _uiState.value.copy(
                    availableApps = currentAvailable.filter { it.packageName != packageName }
                )

                // Reiniciar monitoreo si está activo
                if (_uiState.value.isMonitoringActive) {
                    restartMonitoring()
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateAppLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            try {
                appLimitRepository.updateAppLimit(packageName, limitMinutes)

                // Recalcular tiempo restante
                val remaining = appLimitRepository.getRemainingTime(packageName)
                val currentRemainingTimes = _uiState.value.remainingTimes.toMutableMap()
                currentRemainingTimes[packageName] = remaining

                _uiState.value = _uiState.value.copy(
                    remainingTimes = currentRemainingTimes
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleAppLimit(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                appLimitRepository.toggleAppLimit(packageName, enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun removeAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            try {
                appLimitRepository.removeAppLimit(appLimit)

                // Agregar la app de vuelta a disponibles
                val availableApp = AppUsageInfo(
                    packageName = appLimit.packageName,
                    appName = appLimit.appName,
                    totalTimeInMillis = 0L,
                    lastTimeUsed = 0L
                )

                val currentAvailable = _uiState.value.availableApps.toMutableList()
                currentAvailable.add(availableApp)
                currentAvailable.sortBy { it.appName }

                _uiState.value = _uiState.value.copy(
                    availableApps = currentAvailable
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleAppMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    startMonitoring()
                } else {
                    stopMonitoring()
                }

                _uiState.value = _uiState.value.copy(
                    isMonitoringActive = enabled
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun startMonitoring() {
        AppMonitoringService.startService(context)
    }

    private fun stopMonitoring() {
        AppMonitoringService.stopService(context)
    }

    private fun restartMonitoring() {
        if (_uiState.value.isMonitoringActive) {
            stopMonitoring()
            startMonitoring()
        }
    }

    private fun updateMonitoringStatus() {
        // Verificar si el servicio de monitoreo está activo
        // Esta implementación puede variar según cómo manejes el estado del servicio
        val isActive = isMonitoringServiceRunning()
        _uiState.value = _uiState.value.copy(isMonitoringActive = isActive)
    }

    private fun isMonitoringServiceRunning(): Boolean {
        // Implementación para verificar si el servicio está corriendo
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (AppMonitoringService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun refreshRemainingTimes() {
        viewModelScope.launch {
            val remainingTimes = mutableMapOf<String, Int>()
            _uiState.value.appLimits.forEach { limit ->
                val remaining = appLimitRepository.getRemainingTime(limit.packageName)
                remainingTimes[limit.packageName] = remaining
            }

            _uiState.value = _uiState.value.copy(
                remainingTimes = remainingTimes
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

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
    val suggestedApps: List<AppUsageInfo> = emptyList(),
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

    companion object {
        /**
         * Lista de apps comúnmente bloqueadas/adictivas
         * Solo las que el usuario tenga instaladas aparecerán en la sección de sugerencias
         */
        private val COMMON_ADDICTIVE_PACKAGES = listOf(
            "com.instagram.android",              // Instagram
            "com.zhiliaoapp.musically",          // TikTok (Internacional)
            "com.ss.android.ugc.trill",          // TikTok (Algunos mercados)
            "com.google.android.youtube",        // YouTube
            "com.facebook.katana",               // Facebook
            "com.snapchat.android",              // Snapchat
            "com.twitter.android",               // Twitter/X
            "com.netflix.mediaclient",           // Netflix
            "com.whatsapp",                      // WhatsApp
            "com.reddit.frontpage",              // Reddit
            "com.spotify.music",                 // Spotify
            "com.pinterest",                     // Pinterest
            "com.discord",                       // Discord
            "com.medium.reader",                 // Medium
            "com.tumblr"                         // Tumblr
        )
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
        }

        // Colección de límites en tiempo real
        viewModelScope.launch {
            appLimitRepository.getAllLimits()
                .catch { e -> 
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { limits ->
                    // Calcular remaining times en paralelo para mejor performance
                    val remainingTimes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        limits.associate { limit ->
                            limit.packageName to appLimitRepository.getRemainingTime(limit.packageName)
                        }
                    }
                    _uiState.update { it.copy(
                        appLimits = limits,
                        remainingTimes = remainingTimes,
                        isLoading = false
                    ) }
                }
        }

        // Cargar apps disponibles en paralelo con Dispatchers.IO
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val availableApps = appLimitRepository.getInstallableApps()
                val filteredAvailable = availableApps.filter { app ->
                    _uiState.value.appLimits.none { it.packageName == app.packageName }
                }
                
                // Filtrar apps sugeridas: solo las comunes que están instaladas
                // y que NO tienen límite activo
                val suggestedApps = filteredAvailable.filter { app ->
                    COMMON_ADDICTIVE_PACKAGES.contains(app.packageName)
                }.sortedBy { it.appName }
                
                _uiState.update { it.copy(
                    availableApps = filteredAvailable,
                    suggestedApps = suggestedApps
                ) }
            } catch (e: SecurityException) {
                // Android 11+ sin <queries> adecuado puede causar visibilidad limitada; no cerrar app
                _uiState.update { it.copy(
                    availableApps = emptyList(),
                    suggestedApps = emptyList()
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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

                // Actualizar la lista de apps disponibles y sugeridas usando update atómico
                _uiState.update { currentState ->
                    val filteredAvailable = currentState.availableApps.filter { it.packageName != packageName }
                    val updatedSuggestedApps = filteredAvailable.filter { app ->
                        COMMON_ADDICTIVE_PACKAGES.contains(app.packageName)
                    }.sortedBy { it.appName }
                    
                    currentState.copy(
                        availableApps = filteredAvailable,
                        suggestedApps = updatedSuggestedApps
                    )
                }

                // Reiniciar monitoreo si está activo
                if (_uiState.value.isMonitoringActive) {
                    restartMonitoring()
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateAppLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            try {
                appLimitRepository.updateAppLimit(packageName, limitMinutes)

                // Recalcular tiempo restante
                val remaining = appLimitRepository.getRemainingTime(packageName)
                _uiState.update { currentState ->
                    val updatedRemainingTimes = currentState.remainingTimes.toMutableMap()
                    updatedRemainingTimes[packageName] = remaining
                    currentState.copy(remainingTimes = updatedRemainingTimes)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleAppLimit(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                appLimitRepository.toggleAppLimit(packageName, enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeAppLimit(appLimit: AppLimit) {
        viewModelScope.launch {
            try {
                appLimitRepository.removeAppLimit(appLimit)

                // Agregar la app de vuelta a disponibles usando update atómico
                _uiState.update { currentState ->
                    val availableApp = AppUsageInfo(
                        packageName = appLimit.packageName,
                        appName = appLimit.appName,
                        totalTimeInMillis = 0L,
                        lastTimeUsed = 0L
                    )

                    val updatedAvailable = (currentState.availableApps + availableApp).sortedBy { it.appName }
                    val updatedSuggestedApps = updatedAvailable.filter { app ->
                        COMMON_ADDICTIVE_PACKAGES.contains(app.packageName)
                    }.sortedBy { it.appName }

                    currentState.copy(
                        availableApps = updatedAvailable,
                        suggestedApps = updatedSuggestedApps
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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

                _uiState.update { it.copy(isMonitoringActive = enabled) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
        _uiState.update { it.copy(isMonitoringActive = isActive) }
    }

    private fun isMonitoringServiceRunning(): Boolean {
        // Implementación para verificar si el servicio está corriendo
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                ?: return false
            @Suppress("DEPRECATION")
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE) ?: return false
            for (service in runningServices) {
                if (AppMonitoringService::class.java.name == service.service.className) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun refreshRemainingTimes() {
        viewModelScope.launch {
            val appLimits = _uiState.value.appLimits
            val remainingTimes = appLimits.associate { limit ->
                limit.packageName to appLimitRepository.getRemainingTime(limit.packageName)
            }
            _uiState.update { it.copy(remainingTimes = remainingTimes) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

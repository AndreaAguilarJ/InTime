package com.momentummm.app.ui.screen.applimits

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.AppWhitelist
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.data.repository.AppWhitelistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppWhitelistUiState(
    val whitelistedApps: List<AppWhitelist> = emptyList(),
    val availableApps: List<AppUsageInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppWhitelistViewModel @Inject constructor(
    private val appWhitelistRepository: AppWhitelistRepository,
    private val appLimitRepository: AppLimitRepository
) : ViewModel() {

    var uiState by mutableStateOf(AppWhitelistUiState())
        private set

    val whitelistedApps = appWhitelistRepository.getAllWhitelistedApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)

                // Cargar apps disponibles
                val availableApps = appLimitRepository.getInstallableApps()

                uiState = uiState.copy(
                    availableApps = availableApps,
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun addToWhitelist(packageName: String, appName: String, reason: String = "Emergencias") {
        viewModelScope.launch {
            try {
                appWhitelistRepository.addToWhitelist(packageName, appName, reason)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun removeFromWhitelist(app: AppWhitelist) {
        viewModelScope.launch {
            try {
                appWhitelistRepository.removeFromWhitelist(app)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun addDefaultEmergencyApps() {
        viewModelScope.launch {
            try {
                appWhitelistRepository.addDefaultEmergencyApps()
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}


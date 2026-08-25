package com.momentummm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.UserSettings
import com.momentummm.app.data.repository.UserRepository
import com.momentummm.app.util.LifeWeeksCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import com.momentummm.app.R

data class LifeWeeksUiState(
    val isLoading: Boolean = true,
    val userSettings: UserSettings? = null,
    val lifeWeeksData: LifeWeeksCalculator.LifeWeeksData? = null,
    val showColorPicker: Boolean = false,
    val hasBirthDate: Boolean = false,
    val errorMessage: String? = null
)

class LifeWeeksViewModel(
    private val userRepository: UserRepository,
    private val appContext: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeWeeksUiState())
    val uiState: StateFlow<LifeWeeksUiState> = _uiState.asStateFlow()

    init {
        loadUserSettings()
    }

    private fun loadUserSettings() {
        viewModelScope.launch {
            userRepository.getUserSettings()
                .catch { e -> 
                    e.printStackTrace()
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = appContext.getString(R.string.life_weeks_load_error)
                    ) }
                }
                .collect { settings ->
                    val birthDate = settings?.birthDate
                    val hasBirthDate = birthDate != null

                    val lifeWeeksData = if (birthDate != null) {
                        try {
                            LifeWeeksCalculator.calculateLifeWeeks(birthDate)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        userSettings = settings,
                        lifeWeeksData = lifeWeeksData,
                        hasBirthDate = hasBirthDate,
                        errorMessage = if (!hasBirthDate) "No se ha configurado la fecha de nacimiento" else null
                    ) }
                }
        }
    }

    fun updateColors(livedColor: String, futureColor: String, backgroundColor: String) {
        viewModelScope.launch {
            try {
                userRepository.updateColors(livedColor, futureColor, backgroundColor)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Propagar cancelación
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Error al actualizar colores: ${e.message}"
                ) }
            }
        }
    }

    fun showColorPicker() {
        _uiState.update { it.copy(showColorPicker = true) }
    }

    fun hideColorPicker() {
        _uiState.update { it.copy(showColorPicker = false) }
    }

    fun refreshData() {
        loadUserSettings()
    }
}

class LifeWeeksViewModelFactory(
    private val userRepository: UserRepository,
    private val appContext: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeWeeksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeWeeksViewModel(userRepository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
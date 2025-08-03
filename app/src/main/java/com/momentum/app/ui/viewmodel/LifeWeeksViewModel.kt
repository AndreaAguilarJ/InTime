package com.momentum.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.app.data.entity.UserSettings
import com.momentum.app.data.repository.UserRepository
import com.momentum.app.util.LifeWeeksCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class LifeWeeksUiState(
    val isLoading: Boolean = true,
    val userSettings: UserSettings? = null,
    val lifeWeeksData: LifeWeeksCalculator.LifeWeeksData? = null,
    val showColorPicker: Boolean = false
)

class LifeWeeksViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifeWeeksUiState())
    val uiState: StateFlow<LifeWeeksUiState> = _uiState.asStateFlow()

    init {
        loadUserSettings()
    }

    private fun loadUserSettings() {
        viewModelScope.launch {
            userRepository.getUserSettings().collect { settings ->
                val lifeWeeksData = settings?.birthDate?.let { birthDate ->
                    LifeWeeksCalculator.calculateLifeWeeks(birthDate)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userSettings = settings,
                    lifeWeeksData = lifeWeeksData
                )
            }
        }
    }

    fun updateColors(livedColor: String, futureColor: String, backgroundColor: String) {
        viewModelScope.launch {
            try {
                userRepository.updateColors(livedColor, futureColor, backgroundColor)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun showColorPicker() {
        _uiState.value = _uiState.value.copy(showColorPicker = true)
    }

    fun hideColorPicker() {
        _uiState.value = _uiState.value.copy(showColorPicker = false)
    }
}

class LifeWeeksViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeWeeksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeWeeksViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
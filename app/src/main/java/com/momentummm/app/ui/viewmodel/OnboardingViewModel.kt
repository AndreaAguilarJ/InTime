package com.momentummm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.UserSettings
import com.momentummm.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val isLoading: Boolean = false,
    val selectedBirthDate: Date? = null,
    val hasUsagePermission: Boolean = false,
    val isCompleted: Boolean = false
)

enum class OnboardingStep {
    WELCOME,
    PERMISSIONS,
    BIRTH_DATE,
    COMPLETED
}

class OnboardingViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.BIRTH_DATE
            OnboardingStep.BIRTH_DATE -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED -> OnboardingStep.COMPLETED
        }
        
        _uiState.value = _uiState.value.copy(currentStep = nextStep)
        
        if (nextStep == OnboardingStep.COMPLETED) {
            completeOnboarding()
        }
    }

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasUsagePermission = hasPermission)
    }

    fun setBirthDate(birthDate: Date) {
        _uiState.value = _uiState.value.copy(selectedBirthDate = birthDate)
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val birthDate = _uiState.value.selectedBirthDate
                if (birthDate != null) {
                    userRepository.setBirthDate(birthDate)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isCompleted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

class OnboardingViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
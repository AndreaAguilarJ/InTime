package com.momentummm.app.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.PasswordProtection
import com.momentummm.app.data.repository.PasswordProtectionRepository
import com.momentummm.app.data.repository.PasswordProtectionSettings
import com.momentummm.app.data.repository.ProtectedFeature
import com.momentummm.app.security.BiometricAuthStatus
import com.momentummm.app.security.BiometricPromptManager
import com.momentummm.app.security.BiometricResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordProtectionViewModel @Inject constructor(
    private val repository: PasswordProtectionRepository,
    private val biometricPromptManager: BiometricPromptManager
) : ViewModel() {

    val passwordProtection: StateFlow<PasswordProtection?> = repository.getPasswordProtection()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _remainingLockoutTime = MutableStateFlow(0L)
    val remainingLockoutTime: StateFlow<Long> = _remainingLockoutTime.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isPasswordSet = MutableStateFlow(false)
    val isPasswordSet: StateFlow<Boolean> = _isPasswordSet.asStateFlow()

    val biometricResults = biometricPromptManager.biometricResults

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            
            // Observar cambios en la protección de contraseña
            passwordProtection.collect { protection ->
                _isPasswordSet.value = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
            }
        }
    }

    /**
     * Configura una nueva contraseña con hashing SHA-256
     */
    fun setPassword(password: String, settings: PasswordProtectionSettings) {
        viewModelScope.launch {
            repository.setPassword(password, settings)
        }
    }

    /**
     * Actualiza las configuraciones de protección
     */
    fun updateProtections(settings: PasswordProtectionSettings) {
        viewModelScope.launch {
            repository.updateProtections(settings)
        }
    }

    /**
     * Verifica la contraseña ingresada
     */
    suspend fun verifyPassword(password: String): Boolean {
        return repository.verifyPassword(password)
    }

    /**
     * Desactiva la protección por contraseña
     */
    fun disablePasswordProtection(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.disablePasswordProtection(password)
            onResult(result)
        }
    }

    /**
     * Cambia la contraseña actual
     */
    fun changePassword(oldPassword: String, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(oldPassword, newPassword)
            onResult(result)
        }
    }

    /**
     * Verifica si una característica está protegida
     */
    suspend fun isFeatureProtected(feature: ProtectedFeature): Boolean {
        return repository.isFeatureProtected(feature)
    }

    /**
     * Actualiza el tiempo restante de bloqueo
     */
    fun updateRemainingLockoutTime() {
        viewModelScope.launch {
            _remainingLockoutTime.value = repository.getRemainingLockoutTime()
        }
    }

    /**
     * Verifica si la biometría está disponible en el dispositivo
     */
    fun getBiometricAuthStatus(): BiometricAuthStatus {
        return biometricPromptManager.canAuthenticate()
    }

    /**
     * Verifica si la biometría está disponible y lista para usar
     */
    fun isBiometricAvailable(): Boolean {
        return biometricPromptManager.isBiometricAvailable()
    }

    /**
     * Activa/desactiva la autenticación biométrica
     */
    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    /**
     * Activa o desactiva la protección general (toggle switch)
     */
    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleProtection(enabled)
        }
    }
}
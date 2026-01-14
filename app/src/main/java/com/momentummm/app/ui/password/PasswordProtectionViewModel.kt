package com.momentummm.app.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.PasswordProtection
import com.momentummm.app.data.repository.PasswordProtectionRepository
import com.momentummm.app.data.repository.PasswordProtectionSettings
import com.momentummm.app.data.repository.ProtectedFeature
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
    private val repository: PasswordProtectionRepository
) : ViewModel() {

    val passwordProtection: StateFlow<PasswordProtection?> = repository.getPasswordProtection()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _remainingLockoutTime = MutableStateFlow(0L)
    val remainingLockoutTime: StateFlow<Long> = _remainingLockoutTime.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
    }

    fun setPassword(password: String, settings: PasswordProtectionSettings) {
        viewModelScope.launch {
            repository.setPassword(password, settings)
        }
    }

    fun updateProtections(settings: PasswordProtectionSettings) {
        viewModelScope.launch {
            repository.updateProtections(settings)
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        return repository.verifyPassword(password)
    }

    fun disablePasswordProtection(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.disablePasswordProtection(password)
            onResult(result)
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(oldPassword, newPassword)
            onResult(result)
        }
    }

    suspend fun isFeatureProtected(feature: ProtectedFeature): Boolean {
        return repository.isFeatureProtected(feature)
    }

    fun updateRemainingLockoutTime() {
        viewModelScope.launch {
            _remainingLockoutTime.value = repository.getRemainingLockoutTime()
        }
    }
}


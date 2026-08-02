package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.manager.BillingManager
import com.momentummm.app.data.manager.GamificationManager
import com.momentummm.app.data.manager.SmartBlockingManager
import com.momentummm.app.data.repository.UserRepository
import com.momentummm.app.util.SocialShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para manejar la pantalla de app bloqueada y el sistema "Shame or Pay".
 * Gestiona el desbloqueo temporal con pagos o compartiendo en redes sociales.
 */
@HiltViewModel
class BlockingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val billingManager: BillingManager,
    private val gamificationManager: GamificationManager,
    private val socialShareHelper: SocialShareHelper,
    private val smartBlockingManager: SmartBlockingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockingUiState())
    val uiState: StateFlow<BlockingUiState> = _uiState.asStateFlow()

    // CRITICAL FIX: Usar ConcurrentHashMap para evitar ConcurrentModificationException
    // Antes era un HashMap normal accedido desde múltiples hilos (race condition)
    private val temporaryUnlocks = java.util.concurrent.ConcurrentHashMap<String, SocialShareHelper.TemporaryUnlock>()
    
    // Job para evitar múltiples shame shares simultáneos
    private var shameShareJob: Job? = null

    init {
        observeBillingState()
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            gamificationManager.getGamificationState()
                .catch { e -> e.printStackTrace() }
                .collect { state ->
                    _uiState.update { it.copy(
                        currentStreak = state?.currentStreak ?: 0,
                        timeCoins = state?.timeCoins ?: 0
                    ) }
                }
        }
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.purchaseState
                .catch { e -> 
                    _uiState.update { it.copy(
                        showError = true,
                        errorMessage = "Error en facturación: ${e.message}",
                        isProcessing = false
                    ) }
                }
                .collect { purchaseState ->
                    _uiState.update { currentState ->
                        when (purchaseState) {
                            BillingManager.PurchaseState.Purchased -> currentState.copy(
                                unlockState = UnlockState.UNLOCKED_PAYMENT,
                                isProcessing = false
                            )
                            BillingManager.PurchaseState.Failed -> currentState.copy(
                                showError = true,
                                errorMessage = "Error al procesar el pago",
                                isProcessing = false
                            )
                            BillingManager.PurchaseState.Cancelled -> currentState.copy(isProcessing = false)
                            BillingManager.PurchaseState.Purchasing -> currentState.copy(isProcessing = true)
                            else -> currentState
                        }
                    }
                }
        }
    }

    /**
     * Inicia el proceso de desbloqueo con pago ($0.99)
     */
    fun initiatePaymentUnlock(blockedPackage: String) {
        _uiState.update { it.copy(
            currentBlockedPackage = blockedPackage,
            selectedUnlockMethod = UnlockMethod.PAYMENT
        ) }
    }

    /**
     * Inicia el proceso de desbloqueo por vergüenza (compartir)
     */
    fun initiateShameUnlock(blockedPackage: String, blockedAppName: String) {
        _uiState.update { it.copy(
            currentBlockedPackage = blockedPackage,
            currentBlockedAppName = blockedAppName,
            selectedUnlockMethod = UnlockMethod.SHAME_SHARE,
            showShameConfirmation = true
        ) }
    }

    /**
     * Confirma y ejecuta el shame share
     */
    fun confirmShameShare() {
        // Evitar múltiples ejecuciones simultáneas
        if (_uiState.value.isProcessing) return
        shameShareJob?.cancel()
        
        val state = _uiState.value
        val appName = state.currentBlockedAppName
        val packageName = state.currentBlockedPackage
        
        if (appName.isEmpty() || packageName.isEmpty()) return

        shameShareJob = viewModelScope.launch {
            _uiState.update { it.copy(
                isProcessing = true,
                showShameCountdown = true,
                shameCountdown = 3
            ) }

            try {
                // Countdown de 3 segundos
                for (i in 3 downTo 1) {
                    _uiState.update { it.copy(shameCountdown = i) }
                    kotlinx.coroutines.delay(1000)
                }

                // Ejecutar el share
                val shareType = determineShameType(state.currentStreak)
                val success = socialShareHelper.shareShameImage(
                    context = context,
                    appName = appName,
                    shameType = shareType,
                    streakDays = state.currentStreak
                )

                if (success) {
                    // Registrar desbloqueo temporal
                    registerTemporaryUnlock(
                        packageName = packageName,
                        method = SocialShareHelper.UnlockMethod.SHAME_SHARE
                    )
                    
                    _uiState.update { it.copy(
                        unlockState = UnlockState.UNLOCKED_SHAME,
                        isProcessing = false,
                        showShameCountdown = false
                    ) }
                } else {
                    _uiState.update { it.copy(
                        showError = true,
                        errorMessage = "No se pudo abrir la app para compartir",
                        isProcessing = false,
                        showShameCountdown = false
                    ) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Propagar cancelación
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    showError = true,
                    errorMessage = e.message ?: "Error desconocido",
                    isProcessing = false,
                    showShameCountdown = false
                ) }
            }
        }
    }

    /**
     * Determina el tipo de vergüenza basado en el contexto
     */
    private fun determineShameType(streakDays: Int): SocialShareHelper.ShameType {
        return when {
            streakDays > 7 -> SocialShareHelper.ShameType.STREAK_LOST
            streakDays > 0 -> SocialShareHelper.ShameType.WEAK_MOMENT
            else -> SocialShareHelper.ShameType.DOPAMINE_FAIL
        }
    }

    /**
     * Registra un desbloqueo temporal (5 minutos)
     */
    private fun registerTemporaryUnlock(
        packageName: String,
        method: SocialShareHelper.UnlockMethod
    ) {
        val now = System.currentTimeMillis()
        val unlock = SocialShareHelper.TemporaryUnlock(
            packageName = packageName,
            unlockTime = now,
            expirationTime = now + SocialShareHelper.UNLOCK_DURATION_MS,
            unlockMethod = method
        )
        
        temporaryUnlocks[packageName] = unlock
        
        // Desbloquear temporalmente en el SmartBlockingManager
        smartBlockingManager.temporarilyUnblockApp(packageName)
        
        // Guardar en preferencias para persistencia
        viewModelScope.launch {
            UserPreferencesRepository.addTemporaryUnlock(
                context = context,
                packageName = packageName,
                expirationTime = unlock.expirationTime
            )
        }
    }

    /**
     * Verifica si una app tiene desbloqueo temporal activo
     */
    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val unlock = temporaryUnlocks[packageName] ?: return false
        val isValid = System.currentTimeMillis() < unlock.expirationTime
        
        if (!isValid) {
            temporaryUnlocks.remove(packageName)
        }
        
        return isValid
    }

    /**
     * Obtiene el tiempo restante de desbloqueo temporal
     */
    fun getRemainingUnlockTime(packageName: String): Long {
        val unlock = temporaryUnlocks[packageName] ?: return 0
        val remaining = unlock.expirationTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Cancela el proceso de desbloqueo
     */
    fun cancelUnlock() {
        shameShareJob?.cancel()
        _uiState.update { it.copy(
            showShameConfirmation = false,
            showShameCountdown = false,
            selectedUnlockMethod = null,
            isProcessing = false
        ) }
    }

    /**
     * Limpia el error mostrado
     */
    fun clearError() {
        _uiState.update { it.copy(
            showError = false,
            errorMessage = ""
        ) }
    }

    /**
     * Callback cuando el usuario regresa de compartir
     */
    fun onShareCompleted(success: Boolean) {
        if (success) {
            val packageName = _uiState.value.currentBlockedPackage
            if (packageName.isNotEmpty()) {
                registerTemporaryUnlock(packageName, SocialShareHelper.UnlockMethod.SHAME_SHARE)
                _uiState.update { it.copy(
                    unlockState = UnlockState.UNLOCKED_SHAME,
                    isProcessing = false
                ) }
            }
        } else {
            _uiState.update { it.copy(
                isProcessing = false,
                showError = true,
                errorMessage = "Debes compartir la imagen para desbloquear"
            ) }
        }
    }
}

/**
 * Estado de la UI para la pantalla de bloqueo
 */
data class BlockingUiState(
    val currentBlockedPackage: String = "",
    val currentBlockedAppName: String = "",
    val currentStreak: Int = 0,
    val timeCoins: Int = 0,
    val selectedUnlockMethod: UnlockMethod? = null,
    val unlockState: UnlockState = UnlockState.LOCKED,
    val isProcessing: Boolean = false,
    val showShameConfirmation: Boolean = false,
    val showShameCountdown: Boolean = false,
    val shameCountdown: Int = 3,
    val showError: Boolean = false,
    val errorMessage: String = ""
)

enum class UnlockMethod {
    PAYMENT,
    SHAME_SHARE
}

enum class UnlockState {
    LOCKED,
    UNLOCKED_PAYMENT,
    UNLOCKED_SHAME
}

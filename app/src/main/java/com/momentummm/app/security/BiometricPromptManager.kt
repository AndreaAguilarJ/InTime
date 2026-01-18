package com.momentummm.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricPromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resultChannel = Channel<BiometricResult>()
    val biometricResults = resultChannel.receiveAsFlow()

    /**
     * Verifica si el dispositivo tiene capacidades biométricas disponibles
     */
    fun canAuthenticate(): BiometricAuthStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAuthStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAuthStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAuthStatus.TEMPORARY_NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAuthStatus.AVAILABLE_BUT_NOT_ENROLLED
            else -> BiometricAuthStatus.NOT_AVAILABLE
        }
    }

    /**
     * Muestra el prompt biométrico para autenticación
     * @param activity La actividad desde donde se invoca
     * @param title Título del diálogo
     * @param subtitle Subtítulo opcional
     * @param description Descripción opcional
     * @param negativeButtonText Texto del botón negativo (por defecto "Cancelar")
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancelar"
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                resultChannel.trySend(BiometricResult.AuthenticationError(errString.toString()))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                resultChannel.trySend(BiometricResult.AuthenticationSuccess)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                resultChannel.trySend(BiometricResult.AuthenticationFailed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Verifica si la biometría está disponible y configurada
     */
    fun isBiometricAvailable(): Boolean {
        return canAuthenticate() == BiometricAuthStatus.READY
    }
}

/**
 * Estado de la disponibilidad de autenticación biométrica
 */
enum class BiometricAuthStatus {
    READY, // Listo para usar
    NOT_AVAILABLE, // No hay hardware disponible
    TEMPORARY_NOT_AVAILABLE, // Temporalmente no disponible
    AVAILABLE_BUT_NOT_ENROLLED // Disponible pero no hay huella/cara registrada
}

/**
 * Resultado de la autenticación biométrica
 */
sealed interface BiometricResult {
    data object AuthenticationSuccess : BiometricResult
    data object AuthenticationFailed : BiometricResult
    data class AuthenticationError(val error: String) : BiometricResult
}

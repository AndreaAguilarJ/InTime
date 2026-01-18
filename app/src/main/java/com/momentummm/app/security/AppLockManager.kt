package com.momentummm.app.security

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.momentummm.app.data.repository.PasswordProtectionRepository
import com.momentummm.app.data.repository.ProtectedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el bloqueo automático de la aplicación cuando pasa a segundo plano
 * y la protección por contraseña está habilitada.
 * 
 * Características:
 * - Bloqueo automático al pasar a segundo plano (después de un delay configurable)
 * - Soporte para Grace Period (tiempo antes de bloquear)
 * - Verificación de características protegidas específicas
 * - Integración con autenticación biométrica
 */
@Singleton
class AppLockManager @Inject constructor(
    private val passwordProtectionRepository: PasswordProtectionRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Estado de bloqueo de la app
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Controla si se debe mostrar la pantalla de bloqueo
    private val _shouldShowLockScreen = MutableStateFlow(false)
    val shouldShowLockScreen: StateFlow<Boolean> = _shouldShowLockScreen.asStateFlow()

    // Tiempo en que la app pasó a segundo plano
    private var backgroundTimestamp: Long = 0L
    
    // Grace period en milisegundos (tiempo antes de bloquear)
    private val gracePeriodMs: Long = 5000L // 5 segundos de gracia
    
    // Flags de control
    private var shouldLockOnResume = false
    private var isFirstLaunch = true
    private var isInitialized = false

    // Tiempo de la última autenticación exitosa
    private var lastAuthenticationTime: Long = 0L
    
    // Tiempo durante el cual la autenticación sigue siendo válida
    private val authValidityPeriodMs: Long = 30 * 60 * 1000L // 30 minutos

    init {
        // Observar el ciclo de vida de la aplicación
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        isInitialized = true
        Log.d(TAG, "AppLockManager initialized")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App moved to foreground")

        scope.launch {
            // Verificar si hay protección activa
            val protection = passwordProtectionRepository.getPasswordProtectionSync()
            val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()

            if (!isProtectionEnabled) {
                Log.d(TAG, "Password protection is not enabled, skipping lock")
                return@launch
            }

            // Si es el primer inicio, verificar si debe bloquearse inmediatamente
            if (isFirstLaunch) {
                isFirstLaunch = false
                // En el primer inicio, si la protección está activa, bloquear
                if (isProtectionEnabled && !isAuthenticationStillValid()) {
                    Log.d(TAG, "First launch with protection enabled, locking app")
                    lockApp()
                }
                return@launch
            }

            // Verificar si pasó el grace period
            val timeInBackground = System.currentTimeMillis() - backgroundTimestamp
            
            if (shouldLockOnResume && timeInBackground > gracePeriodMs) {
                // Verificar que la autenticación no siga siendo válida
                if (!isAuthenticationStillValid()) {
                    Log.d(TAG, "Grace period exceeded ($timeInBackground ms), locking app")
                    lockApp()
                } else {
                    Log.d(TAG, "Authentication still valid, not locking")
                }
            } else {
                Log.d(TAG, "Within grace period ($timeInBackground ms), not locking")
            }
            
            // Resetear la bandera
            shouldLockOnResume = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App moved to background")

        scope.launch {
            // Verificar si hay protección activa
            val protection = passwordProtectionRepository.getPasswordProtectionSync()
            val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()

            if (isProtectionEnabled) {
                // Marcar timestamp y flag para bloquear cuando vuelva
                backgroundTimestamp = System.currentTimeMillis()
                shouldLockOnResume = true
                Log.d(TAG, "Marked app for potential locking on next resume")
            }
        }
    }

    /**
     * Verifica si la autenticación reciente sigue siendo válida
     */
    private fun isAuthenticationStillValid(): Boolean {
        if (lastAuthenticationTime == 0L) return false
        val elapsed = System.currentTimeMillis() - lastAuthenticationTime
        return elapsed < authValidityPeriodMs
    }

    /**
     * Bloquea la aplicación mostrando la pantalla de bloqueo
     */
    private fun lockApp() {
        _isLocked.value = true
        _shouldShowLockScreen.value = true
        Log.d(TAG, "App locked")
    }

    /**
     * Desbloquea la aplicación después de una autenticación exitosa
     */
    fun unlockApp() {
        _isLocked.value = false
        _shouldShowLockScreen.value = false
        lastAuthenticationTime = System.currentTimeMillis()
        Log.d(TAG, "App unlocked, authentication valid until ${lastAuthenticationTime + authValidityPeriodMs}")
    }

    /**
     * Verifica si la aplicación está bloqueada actualmente
     */
    fun isAppLocked(): Boolean = _isLocked.value

    /**
     * Fuerza el bloqueo de la aplicación inmediatamente
     * Útil para características que requieren autenticación
     */
    fun forceLock() {
        scope.launch {
            val protection = passwordProtectionRepository.getPasswordProtectionSync()
            val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
            
            if (isProtectionEnabled) {
                lockApp()
            }
        }
    }

    /**
     * Verifica si una característica específica requiere autenticación
     * y si la app está actualmente desbloqueada
     */
    suspend fun requiresAuthenticationForFeature(feature: ProtectedFeature): Boolean {
        // Si la app ya está desbloqueada con autenticación válida, no requiere más
        if (isAuthenticationStillValid()) {
            return false
        }
        return passwordProtectionRepository.isFeatureProtected(feature)
    }

    /**
     * Verifica si se requiere autenticación para cualquier acción protegida
     */
    suspend fun requiresAuthentication(): Boolean {
        if (isAuthenticationStillValid()) {
            return false
        }
        val protection = passwordProtectionRepository.getPasswordProtectionSync()
        return protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
    }

    /**
     * Bloquea la app después de un delay específico
     * Útil para programar bloqueos diferidos
     */
    fun lockAfterDelay(delayMs: Long = gracePeriodMs) {
        scope.launch {
            delay(delayMs)
            val protection = passwordProtectionRepository.getPasswordProtectionSync()
            val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
            
            if (isProtectionEnabled && !isAuthenticationStillValid()) {
                lockApp()
            }
        }
    }

    /**
     * Invalida la sesión de autenticación actual
     * Forzará re-autenticación en la próxima verificación
     */
    fun invalidateSession() {
        lastAuthenticationTime = 0L
        Log.d(TAG, "Authentication session invalidated")
    }

    /**
     * Registra una autenticación exitosa externa (ej: biométrica)
     */
    fun registerSuccessfulAuthentication() {
        lastAuthenticationTime = System.currentTimeMillis()
        _isLocked.value = false
        _shouldShowLockScreen.value = false
        Log.d(TAG, "External authentication registered successfully")
    }

    companion object {
        private const val TAG = "AppLockManager"
    }
}

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

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.Main)
    
    // Flag para evitar operaciones después de destrucción
    @Volatile
    private var isDestroyed = false
    
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
    
    // Flags de control - CRITICAL FIX: Usar @Volatile para visibilidad entre hilos
    // Antes, onStop y onStart corrían en coroutines diferentes y podían no ver los cambios
    @Volatile
    private var shouldLockOnResume = false
    @Volatile
    private var isFirstLaunch = true
    @Volatile
    private var isInitialized = false

    // Tiempo de la última autenticación exitosa
    @Volatile
    private var lastAuthenticationTime: Long = 0L
    
    // Tiempo durante el cual la autenticación sigue siendo válida
    private val authValidityPeriodMs: Long = 30 * 60 * 1000L // 30 minutos

    init {
        // Observar el ciclo de vida de la aplicación de forma segura
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            isInitialized = true
            Log.d(TAG, "AppLockManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AppLockManager", e)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isDestroyed) return
        Log.d(TAG, "App moved to foreground")

        scope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error in onStart", e)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isDestroyed) return
        Log.d(TAG, "App moved to background")

        scope.launch {
            try {
                // Verificar si hay protección activa
                val protection = passwordProtectionRepository.getPasswordProtectionSync()
                val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()

                if (isProtectionEnabled) {
                    // Marcar timestamp y flag para bloquear cuando vuelva
                    backgroundTimestamp = System.currentTimeMillis()
                    shouldLockOnResume = true
                    Log.d(TAG, "Marked app for potential locking on next resume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onStop", e)
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
     * Desbloquea la aplicación después de una autenticación exitosa.
     * CRITICAL FIX: Se verifica que realmente hubo una autenticación reciente
     * para evitar que código arbitrario pueda llamar este método para bypass.
     */
    @Volatile
    private var authenticationInProgress = false
    
    /**
     * Marca que una autenticación está en proceso.
     *
     * La llama [com.momentummm.app.ui.password.LockScreen] mientras está en
     * pantalla. Antes NADIE la llamaba, así que [unlockApp] siempre salía por
     * la guarda y la app quedaba imposible de desbloquear.
     */
    fun beginAuthentication() {
        authenticationInProgress = true
    }

    /** Cierra la ventana de autenticación al desaparecer la pantalla de bloqueo. */
    fun endAuthentication() {
        authenticationInProgress = false
    }
    
    fun unlockApp() {
        // CRITICAL FIX: Solo permitir desbloqueo si una autenticación estaba en proceso
        if (!authenticationInProgress) {
            Log.w(TAG, "unlockApp() called without active authentication - potential bypass attempt")
            return
        }
        authenticationInProgress = false
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
        if (isDestroyed) return
        scope.launch {
            try {
                val protection = passwordProtectionRepository.getPasswordProtectionSync()
                val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
                
                if (isProtectionEnabled) {
                    lockApp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in forceLock", e)
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
        if (isDestroyed) return
        scope.launch {
            try {
                delay(delayMs)
                val protection = passwordProtectionRepository.getPasswordProtectionSync()
                val isProtectionEnabled = protection?.isEnabled == true && !protection.passwordHash.isNullOrEmpty()
                
                if (isProtectionEnabled && !isAuthenticationStillValid()) {
                    lockApp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in lockAfterDelay", e)
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
     * NOTA: aquí había `registerSuccessfulAuthentication()`, que ponía
     * `_isLocked = false` sin ninguna comprobación. No lo llamaba nadie —el
     * desbloqueo real pasa por [unlockApp]— y era exactamente el atajo que la
     * guarda `authenticationInProgress` pretende cerrar, así que se ha
     * eliminado en lugar de dejar dos puertas con distinto cerrojo.
     */

    companion object {
        private const val TAG = "AppLockManager"
    }
}

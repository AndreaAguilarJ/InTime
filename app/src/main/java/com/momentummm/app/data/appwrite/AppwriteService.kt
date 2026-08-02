package com.momentummm.app.data.appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import io.appwrite.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import android.util.Log

private const val TAG = "AppwriteService"
private const val NETWORK_TIMEOUT_MS = 30_000L

class AppwriteService(context: Context) {
    private val client = Client(context)
        .setEndpoint(AppwriteConfig.ENDPOINT)
        .setProject(AppwriteConfig.PROJECT_ID)
    
    val account = Account(client)
    val databases = Databases(client)
    val storage = Storage(client)
    val databaseId = AppwriteConfig.DATABASE_ID

    private val _currentUser = MutableStateFlow<User<*>?>(null)
    val currentUser: StateFlow<User<*>?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Indica que la verificación inicial de sesión terminó (éxito o fallo)
    private val _isAuthReady = MutableStateFlow(false)
    val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        _isAuthReady.value = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _currentUser.value = null
                _isLoggedIn.value = false
            } finally {
                _isAuthReady.value = true
            }
        }
    }
    
    suspend fun createAccount(email: String, password: String, name: String): Result<User<*>> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                account.create(
                    userId = "unique()",
                    email = email,
                    password = password,
                    name = name
                )
                // Crear sesión tras crear cuenta
                runCatching { account.createEmailPasswordSession(email, password) }
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
                _isAuthReady.value = true
                Result.success(user)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Create account timeout", e)
            _isAuthReady.value = true
            Result.failure(Exception("Connection timeout. Please check your internet connection."))
        } catch (e: Exception) {
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<User<*>> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                Log.d(TAG, "Attempting login for email: $email")
                account.createEmailPasswordSession(email, password)
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
                _isAuthReady.value = true
                Log.d(TAG, "Login successful for user: ${user.id}")
                Result.success(user)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Login timeout", e)
            _isAuthReady.value = true
            Result.failure(Exception("Connection timeout. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                account.deleteSession("current")
                _currentUser.value = null
                _isLoggedIn.value = false
                _isAuthReady.value = true
                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Logout timeout", e)
            // En caso de timeout, forzar logout local
            _currentUser.value = null
            _isLoggedIn.value = false
            _isAuthReady.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    /**
     * Cambia la contraseña de la cuenta.
     *
     * BUG CORREGIDO: la pantalla de ajustes de cuenta tenía una tarjeta
     * "Cambiar contraseña" con flecha de navegación cuyo `onClick` era un
     * `// TODO` vacío: se podía pulsar y no pasaba absolutamente nada.
     *
     * @param newPassword contraseña nueva (Appwrite exige 8 caracteres mínimo).
     * @param oldPassword contraseña actual; Appwrite la exige para cuentas
     *   creadas con email y contraseña.
     */
    suspend fun updatePassword(newPassword: String, oldPassword: String): Result<Unit> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                account.updatePassword(password = newPassword, oldPassword = oldPassword)
                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Update password timeout", e)
            Result.failure(Exception("Connection timeout. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Update password failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Da de baja la cuenta del usuario y cierra la sesión.
     *
     * BUG CORREGIDO: el diálogo "Eliminar cuenta" tenía el borrado comentado
     * (`// TODO: Implementar eliminación de cuenta`) pero cerraba el diálogo y
     * volvía atrás como si hubiera funcionado. El usuario confirmaba el borrado
     * de su cuenta, la app le decía que sí, y la cuenta seguía existiendo
     * intacta en el servidor.
     *
     * Nota sobre la API: el SDK de cliente de Appwrite **no** permite un
     * borrado duro (eso requiere una API key de servidor). La vía oficial desde
     * el cliente es `account.updateStatus()`, que bloquea la cuenta de forma
     * permanente e irreversible: el usuario ya no puede volver a entrar. Por
     * eso el texto de la interfaz debe hablar de desactivar, no prometer un
     * borrado inmediato de todos los datos.
     */
    suspend fun deactivateAccount(): Result<Unit> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                account.updateStatus()
            }
            // La sesión ya no sirve para nada: limpiar el estado local.
            runCatching { account.deleteSessions() }
            _currentUser.value = null
            _isLoggedIn.value = false
            _isAuthReady.value = true
            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Deactivate account timeout", e)
            Result.failure(Exception("Connection timeout. Please check your internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Deactivate account failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<User<*>> {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
                _isAuthReady.value = true
                Result.success(user)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Get current user timeout", e)
            _isAuthReady.value = true
            Result.failure(Exception("Connection timeout. Please check your internet connection."))
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
}
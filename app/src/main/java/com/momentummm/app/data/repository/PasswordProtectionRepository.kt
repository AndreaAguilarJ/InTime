package com.momentummm.app.data.repository

import android.util.Log
import com.momentummm.app.data.dao.PasswordProtectionDao
import com.momentummm.app.data.entity.PasswordProtection
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PasswordProtectionRepo"

@Singleton
class PasswordProtectionRepository @Inject constructor(
    private val passwordProtectionDao: PasswordProtectionDao
) {
    fun getPasswordProtection(): Flow<PasswordProtection?> = passwordProtectionDao.getPasswordProtection()

    suspend fun getPasswordProtectionSync(): PasswordProtection? = passwordProtectionDao.getPasswordProtectionSync()

    suspend fun initializeIfNeeded() {
        try {
            val existing = passwordProtectionDao.getPasswordProtectionSync()
            if (existing == null) {
                passwordProtectionDao.insert(PasswordProtection())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing password protection", e)
        }
    }

    suspend fun setPassword(password: String, protections: PasswordProtectionSettings) {
        try {
            val hash = hashPassword(password)
            val current = passwordProtectionDao.getPasswordProtectionSync() ?: PasswordProtection()

            passwordProtectionDao.update(
                current.copy(
                    passwordHash = hash,
                    isEnabled = true,
                    protectAppLimits = protections.protectAppLimits,
                    protectInAppBlocking = protections.protectInAppBlocking,
                    protectWebsiteBlocking = protections.protectWebsiteBlocking,
                    protectMinimalMode = protections.protectMinimalMode,
                    failedAttempts = 0,
                    lockoutUntil = 0
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting password", e)
        }
    }

    suspend fun updateProtections(protections: PasswordProtectionSettings) {
        try {
            val current = passwordProtectionDao.getPasswordProtectionSync() ?: return

            passwordProtectionDao.update(
                current.copy(
                    protectAppLimits = protections.protectAppLimits,
                    protectInAppBlocking = protections.protectInAppBlocking,
                    protectWebsiteBlocking = protections.protectWebsiteBlocking,
                    protectMinimalMode = protections.protectMinimalMode
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating protections", e)
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return false

            if (!protection.isEnabled || protection.passwordHash == null) {
                return true // No hay contraseña configurada
            }

            // Verificar si hay bloqueo temporal
            val currentTime = System.currentTimeMillis()
            if (protection.lockoutUntil > currentTime) {
                return false
            }

            val inputHash = hashPassword(password)
            val isCorrect = inputHash == protection.passwordHash

            if (!isCorrect) {
                // Incrementar intentos fallidos
                val newAttempts = protection.failedAttempts + 1
                val lockoutTime = if (newAttempts >= 5) {
                    // Bloquear por 5 minutos después de 5 intentos fallidos
                    currentTime + (5 * 60 * 1000)
                } else {
                    0L
                }

                passwordProtectionDao.update(
                    protection.copy(
                        failedAttempts = newAttempts,
                        lastFailedAttempt = currentTime,
                        lockoutUntil = lockoutTime
                    )
                )
            } else {
                // Resetear intentos fallidos en caso de éxito
                passwordProtectionDao.resetFailedAttempts()
            }

            isCorrect
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying password", e)
            false
        }
    }

    suspend fun disablePasswordProtection(password: String): Boolean {
        return try {
            if (!verifyPassword(password)) {
                return false
            }

            val current = passwordProtectionDao.getPasswordProtectionSync() ?: return false
            passwordProtectionDao.update(
                current.copy(
                    isEnabled = false,
                    passwordHash = null,
                    failedAttempts = 0,
                    lockoutUntil = 0
                )
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling password protection", e)
            false
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        return try {
            if (!verifyPassword(oldPassword)) {
                return false
            }

            val current = passwordProtectionDao.getPasswordProtectionSync() ?: return false
            val newHash = hashPassword(newPassword)

            passwordProtectionDao.update(
                current.copy(
                    passwordHash = newHash,
                    failedAttempts = 0,
                    lockoutUntil = 0
                )
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error changing password", e)
            false
        }
    }

    suspend fun isFeatureProtected(feature: ProtectedFeature): Boolean {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return false

            if (!protection.isEnabled) return false

            when (feature) {
                ProtectedFeature.APP_LIMITS -> protection.protectAppLimits
                ProtectedFeature.IN_APP_BLOCKING -> protection.protectInAppBlocking
                ProtectedFeature.WEBSITE_BLOCKING -> protection.protectWebsiteBlocking
                ProtectedFeature.MINIMAL_MODE -> protection.protectMinimalMode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking feature protection", e)
            false
        }
    }

    suspend fun getRemainingLockoutTime(): Long {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return 0L
            val currentTime = System.currentTimeMillis()

            if (protection.lockoutUntil > currentTime) {
                protection.lockoutUntil - currentTime
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remaining lockout time", e)
            0L
        }
    }

    /**
     * Activa o desactiva la protección (toggle switch sin cambiar la contraseña)
     */
    suspend fun toggleProtection(enabled: Boolean) {
        try {
            val current = passwordProtectionDao.getPasswordProtectionSync() ?: return
            // Solo permite activar si hay contraseña configurada
            if (enabled && current.passwordHash.isNullOrEmpty()) {
                return
            }
            passwordProtectionDao.update(current.copy(isEnabled = enabled))
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling protection", e)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

data class PasswordProtectionSettings(
    val protectAppLimits: Boolean = true,
    val protectInAppBlocking: Boolean = true,
    val protectWebsiteBlocking: Boolean = true,
    val protectMinimalMode: Boolean = true
)

enum class ProtectedFeature {
    APP_LIMITS,
    IN_APP_BLOCKING,
    WEBSITE_BLOCKING,
    MINIMAL_MODE
}

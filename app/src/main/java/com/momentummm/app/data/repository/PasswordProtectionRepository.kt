package com.momentummm.app.data.repository

import android.util.Log
import com.momentummm.app.data.dao.PasswordProtectionDao
import com.momentummm.app.data.entity.PasswordProtection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PasswordProtectionRepo"
private const val HASH_SCHEME = "pbkdf2_sha256"
private const val PBKDF2_ITERATIONS = 210_000
private const val PBKDF2_KEY_BITS = 256
private const val SALT_BYTES = 16

@Singleton
class PasswordProtectionRepository @Inject constructor(
    private val passwordProtectionDao: PasswordProtectionDao
) {
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun getPasswordProtection(): Flow<PasswordProtection?> =
        passwordProtectionDao.getPasswordProtection()

    suspend fun getPasswordProtectionSync(): PasswordProtection? =
        passwordProtectionDao.getPasswordProtectionSync()

    fun clearError() {
        _lastError.value = null
    }

    suspend fun initializeIfNeeded() {
        try {
            if (passwordProtectionDao.getPasswordProtectionSync() == null) {
                passwordProtectionDao.insert(PasswordProtection())
            }
            clearError()
        } catch (error: Exception) {
            recordError("inicializar la protección", error)
        }
    }

    suspend fun setPassword(password: String, protections: PasswordProtectionSettings) {
        try {
            require(password.isNotEmpty()) { "La contraseña no puede estar vacía" }
            val current = passwordProtectionDao.getPasswordProtectionSync() ?: PasswordProtection()
            passwordProtectionDao.insert(
                current.copy(
                    passwordHash = createPasswordHash(password),
                    isEnabled = true,
                    protectAppLimits = protections.protectAppLimits,
                    protectInAppBlocking = protections.protectInAppBlocking,
                    protectWebsiteBlocking = protections.protectWebsiteBlocking,
                    protectMinimalMode = protections.protectMinimalMode,
                    failedAttempts = 0,
                    lastFailedAttempt = 0,
                    lockoutUntil = 0
                )
            )
            clearError()
        } catch (error: Exception) {
            recordError("guardar la contraseña", error)
        }
    }

    suspend fun updateProtections(protections: PasswordProtectionSettings) {
        try {
            val current = passwordProtectionDao.getPasswordProtectionSync()
                ?: throw IllegalStateException("La protección no está inicializada")
            passwordProtectionDao.update(
                current.copy(
                    protectAppLimits = protections.protectAppLimits,
                    protectInAppBlocking = protections.protectInAppBlocking,
                    protectWebsiteBlocking = protections.protectWebsiteBlocking,
                    protectMinimalMode = protections.protectMinimalMode
                )
            )
            clearError()
        } catch (error: Exception) {
            recordError("actualizar las áreas protegidas", error)
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return false
            val storedHash = protection.passwordHash

            if (!protection.isEnabled || storedHash == null) {
                return true
            }

            val currentTime = System.currentTimeMillis()
            if (protection.lockoutUntil > currentTime) {
                _lastError.value = "Demasiados intentos. Inténtalo de nuevo más tarde."
                return false
            }

            val verification = verifyPasswordHash(password, storedHash)
            if (!verification.matches) {
                recordFailedAttempt(protection, currentTime)
                _lastError.value = "Contraseña incorrecta"
                false
            } else {
                val upgradedHash = if (verification.usesLegacyHash) {
                    createPasswordHash(password)
                } else {
                    storedHash
                }
                passwordProtectionDao.update(
                    protection.copy(
                        passwordHash = upgradedHash,
                        failedAttempts = 0,
                        lastFailedAttempt = 0,
                        lockoutUntil = 0
                    )
                )
                clearError()
                true
            }
        } catch (error: Exception) {
            recordError("verificar la contraseña", error)
            false
        }
    }

    suspend fun disablePasswordProtection(password: String): Boolean {
        return try {
            if (!verifyPassword(password)) return false
            val current = passwordProtectionDao.getPasswordProtectionSync()
                ?: throw IllegalStateException("La protección no está inicializada")
            passwordProtectionDao.update(
                current.copy(
                    isEnabled = false,
                    passwordHash = null,
                    failedAttempts = 0,
                    lastFailedAttempt = 0,
                    lockoutUntil = 0
                )
            )
            clearError()
            true
        } catch (error: Exception) {
            recordError("desactivar la protección", error)
            false
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        return try {
            require(newPassword.isNotEmpty()) { "La contraseña nueva no puede estar vacía" }
            if (!verifyPassword(oldPassword)) return false
            val current = passwordProtectionDao.getPasswordProtectionSync()
                ?: throw IllegalStateException("La protección no está inicializada")
            passwordProtectionDao.update(
                current.copy(
                    passwordHash = createPasswordHash(newPassword),
                    failedAttempts = 0,
                    lastFailedAttempt = 0,
                    lockoutUntil = 0
                )
            )
            clearError()
            true
        } catch (error: Exception) {
            recordError("cambiar la contraseña", error)
            false
        }
    }

    suspend fun isFeatureProtected(feature: ProtectedFeature): Boolean {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return false
            if (!protection.isEnabled) return false

            clearError()
            when (feature) {
                ProtectedFeature.APP_LIMITS -> protection.protectAppLimits
                ProtectedFeature.IN_APP_BLOCKING -> protection.protectInAppBlocking
                ProtectedFeature.WEBSITE_BLOCKING -> protection.protectWebsiteBlocking
                ProtectedFeature.MINIMAL_MODE -> protection.protectMinimalMode
            }
        } catch (error: Exception) {
            recordError("consultar la protección", error)
            false
        }
    }

    suspend fun getRemainingLockoutTime(): Long {
        return try {
            val protection = passwordProtectionDao.getPasswordProtectionSync() ?: return 0L
            clearError()
            (protection.lockoutUntil - System.currentTimeMillis()).coerceAtLeast(0L)
        } catch (error: Exception) {
            recordError("consultar el bloqueo temporal", error)
            0L
        }
    }

    suspend fun toggleProtection(enabled: Boolean) {
        try {
            val current = passwordProtectionDao.getPasswordProtectionSync()
                ?: throw IllegalStateException("La protección no está inicializada")
            if (enabled && current.passwordHash.isNullOrEmpty()) {
                _lastError.value = "Configura una contraseña antes de activar la protección."
                return
            }
            passwordProtectionDao.update(current.copy(isEnabled = enabled))
            clearError()
        } catch (error: Exception) {
            recordError("cambiar el estado de protección", error)
        }
    }

    private suspend fun recordFailedAttempt(protection: PasswordProtection, currentTime: Long) {
        val newAttempts = protection.failedAttempts + 1
        val lockoutTime = if (newAttempts >= 5) currentTime + 5 * 60 * 1000L else 0L
        passwordProtectionDao.update(
            protection.copy(
                failedAttempts = newAttempts,
                lastFailedAttempt = currentTime,
                lockoutUntil = lockoutTime
            )
        )
    }

    private fun createPasswordHash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = deriveKey(password, salt, PBKDF2_ITERATIONS)
        return listOf(
            HASH_SCHEME,
            PBKDF2_ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash)
        ).joinToString("$")
    }

    private fun verifyPasswordHash(password: String, storedHash: String): HashVerification {
        if (!storedHash.startsWith("$HASH_SCHEME$")) {
            return HashVerification(
                matches = MessageDigest.isEqual(legacySha256(password), decodeHex(storedHash)),
                usesLegacyHash = true
            )
        }

        val parts = storedHash.split('$')
        if (parts.size != 4 || parts[0] != HASH_SCHEME) return HashVerification(false, false)
        val iterations = parts[1].toIntOrNull()
            ?.takeIf { it in 100_000..1_000_000 }
            ?: return HashVerification(false, false)
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull()
            ?: return HashVerification(false, false)
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull()
            ?: return HashVerification(false, false)
        val actual = deriveKey(password, salt, iterations)
        return HashVerification(MessageDigest.isEqual(actual, expected), false)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            passwordChars.fill('\u0000')
        }
    }

    private fun legacySha256(password: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))

    private fun decodeHex(value: String): ByteArray {
        if (value.length != 64 || value.any { it.digitToIntOrNull(16) == null }) return ByteArray(0)
        return ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun recordError(operation: String, error: Exception) {
        Log.e(TAG, "No se pudo $operation", error)
        _lastError.value = error.message?.takeIf { it.isNotBlank() }
            ?: "No se pudo $operation. Inténtalo de nuevo."
    }

    private data class HashVerification(
        val matches: Boolean,
        val usesLegacyHash: Boolean
    )
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

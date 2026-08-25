package com.momentummm.app.data.repository

import com.momentummm.app.data.dao.PasswordProtectionDao
import com.momentummm.app.data.entity.PasswordProtection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordProtectionRepositoryTest {

    @Test
    fun `configurar contraseña guarda hash y nunca texto plano`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)

        repository.setPassword("correct-horse", PasswordProtectionSettings())

        val stored = requireNotNull(dao.current)
        assertTrue(stored.isEnabled)
        assertNotEquals("correct-horse", stored.passwordHash)
        // El esquema vigente es PBKDF2, no el SHA-256 hex heredado de 64 caracteres:
        // formato "pbkdf2_sha256$<iteraciones>$<sal base64>$<hash base64>".
        val hash = requireNotNull(stored.passwordHash)
        val parts = hash.split('$')
        assertEquals(4, parts.size)
        assertEquals("pbkdf2_sha256", parts[0])
        assertTrue(requireNotNull(parts[1].toIntOrNull()) >= 100_000)
        assertTrue(parts[2].isNotBlank())
        assertTrue(parts[3].isNotBlank())
        // La contraseña en claro no debe aparecer en ninguna parte del valor guardado.
        assertFalse(hash.contains("correct-horse"))
    }

    @Test
    fun `dos contraseñas iguales producen hashes distintos por la sal`() = runBlocking {
        val primero = InMemoryPasswordProtectionDao()
        val segundo = InMemoryPasswordProtectionDao()
        PasswordProtectionRepository(primero).setPassword("correct-horse", PasswordProtectionSettings())
        PasswordProtectionRepository(segundo).setPassword("correct-horse", PasswordProtectionSettings())

        assertNotEquals(primero.current?.passwordHash, segundo.current?.passwordHash)
        // Y ambas siguen verificando correctamente su propia contraseña.
        assertTrue(PasswordProtectionRepository(primero).verifyPassword("correct-horse"))
        assertTrue(PasswordProtectionRepository(segundo).verifyPassword("correct-horse"))
    }

    @Test
    fun `la contraseña correcta se acepta y limpia intentos fallidos`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)
        repository.setPassword("correct-horse", PasswordProtectionSettings())
        dao.current = requireNotNull(dao.current).copy(failedAttempts = 3)

        assertTrue(repository.verifyPassword("correct-horse"))
        assertEquals(0, dao.current?.failedAttempts)
    }

    @Test
    fun `una contraseña incorrecta incrementa los intentos`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)
        repository.setPassword("correct-horse", PasswordProtectionSettings())

        assertFalse(repository.verifyPassword("wrong"))
        assertEquals(1, dao.current?.failedAttempts)
        assertTrue(requireNotNull(dao.current).lastFailedAttempt > 0)
    }

    @Test
    fun `el quinto fallo activa un bloqueo temporal`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)
        repository.setPassword("correct-horse", PasswordProtectionSettings())
        dao.current = requireNotNull(dao.current).copy(failedAttempts = 4)
        val before = System.currentTimeMillis()

        assertFalse(repository.verifyPassword("wrong"))

        val stored = requireNotNull(dao.current)
        assertEquals(5, stored.failedAttempts)
        assertTrue(stored.lockoutUntil >= before + 5 * 60 * 1000)
    }

    @Test
    fun `durante el bloqueo incluso la contraseña correcta se rechaza`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)
        repository.setPassword("correct-horse", PasswordProtectionSettings())
        dao.current = requireNotNull(dao.current).copy(
            failedAttempts = 5,
            lockoutUntil = System.currentTimeMillis() + 60_000
        )

        assertFalse(repository.verifyPassword("correct-horse"))
        assertEquals(5, dao.current?.failedAttempts)
    }

    @Test
    fun `cambiar contraseña invalida la anterior y acepta la nueva`() = runBlocking {
        val dao = InMemoryPasswordProtectionDao()
        val repository = PasswordProtectionRepository(dao)
        repository.setPassword("old-password", PasswordProtectionSettings())

        assertTrue(repository.changePassword("old-password", "new-password"))
        assertFalse(repository.verifyPassword("old-password"))
        assertTrue(repository.verifyPassword("new-password"))
    }

    private class InMemoryPasswordProtectionDao(
        initial: PasswordProtection? = null
    ) : PasswordProtectionDao {
        private val state = MutableStateFlow(initial)

        var current: PasswordProtection?
            get() = state.value
            set(value) {
                state.value = value
            }

        override fun getPasswordProtection(): Flow<PasswordProtection?> = state

        override suspend fun getPasswordProtectionSync(): PasswordProtection? = current

        override suspend fun insert(passwordProtection: PasswordProtection) {
            current = passwordProtection
        }

        override suspend fun update(passwordProtection: PasswordProtection) {
            current = passwordProtection
        }

        override suspend fun updateFailedAttempts(attempts: Int, timestamp: Long) {
            current = current?.copy(failedAttempts = attempts, lastFailedAttempt = timestamp)
        }

        override suspend fun updateLockoutUntil(timestamp: Long) {
            current = current?.copy(lockoutUntil = timestamp)
        }

        override suspend fun resetFailedAttempts() {
            current = current?.copy(failedAttempts = 0)
        }
    }
}

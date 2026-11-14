package com.momentummm.app.data.account

import com.momentummm.app.R
import com.momentummm.app.util.ErrorHandler
import io.appwrite.exceptions.AppwriteException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * El 4 de noviembre de 2026 Google rechazó la 1.3.1 (17): el revisor no pudo
 * entrar con la cuenta de revisión y vio «An unexpected error occurred».
 * La causa era el servidor: el proyecto de Appwrite pasó a solo lectura y
 * respondía 402 `team_read_only` a todo, hasta a `GET /locale`. Como toda la
 * app estaba detrás del inicio de sesión, nadie podía usar Momentum.
 *
 * Estas pruebas fijan las reglas del modo sin cuenta.
 */
class AccountAccessTest {

    private val readOnly = AppwriteException(
        "This project is in readonly mode. Please contact the organization admin for details.",
        402,
        "team_read_only",
        null
    )

    @Test
    fun `un proyecto en solo lectura cuenta como servidor no disponible`() {
        assertEquals(AccountServer.UNAVAILABLE, AccountAccess.classify(readOnly))
    }

    @Test
    fun `un error 5xx cuenta como servidor no disponible`() {
        assertEquals(AccountServer.UNAVAILABLE, AccountAccess.classify(AppwriteException("down", 503, "general_server_error", null)))
    }

    @Test
    fun `un 401 sin sesion significa que el servidor atiende con normalidad`() {
        val noSession = AppwriteException("missing scope", 401, "general_unauthorized_scope", null)
        assertEquals(AccountServer.AVAILABLE, AccountAccess.classify(noSession))
    }

    @Test
    fun `sin red no se decide nada sobre el servidor`() {
        assertEquals(AccountServer.UNREACHABLE, AccountAccess.classify(IOException("Unable to resolve host")))
        assertEquals(AccountServer.UNREACHABLE, AccountAccess.classify(IllegalStateException("x")))
    }

    @Test
    fun `con el servidor no disponible se entra directamente sin cuenta`() {
        assertTrue(AccountAccess.shouldAutoEnterLocal(AccountServer.UNAVAILABLE, isLoggedIn = false))
        assertFalse(AccountAccess.shouldAutoEnterLocal(AccountServer.UNAVAILABLE, isLoggedIn = true))
        // Sin red o con el servidor sano se sigue mostrando la bienvenida.
        assertFalse(AccountAccess.shouldAutoEnterLocal(AccountServer.UNREACHABLE, isLoggedIn = false))
        assertFalse(AccountAccess.shouldAutoEnterLocal(AccountServer.AVAILABLE, isLoggedIn = false))
        assertFalse(AccountAccess.shouldAutoEnterLocal(AccountServer.UNKNOWN, isLoggedIn = false))
    }

    @Test
    fun `se entra en la app con sesion o en modo sin cuenta`() {
        assertTrue(AccountAccess.entersApp(isLoggedIn = true, localMode = false))
        assertTrue(AccountAccess.entersApp(isLoggedIn = false, localMode = true))
        assertFalse(AccountAccess.entersApp(isLoggedIn = false, localMode = false))
    }

    @Test
    fun `no se ofrece iniciar sesion si el servidor no atiende`() {
        assertFalse(AccountAccess.offersAccounts(AccountServer.UNAVAILABLE))
        assertTrue(AccountAccess.offersAccounts(AccountServer.AVAILABLE))
        assertTrue(AccountAccess.offersAccounts(AccountServer.UNREACHABLE))
        assertTrue(AccountAccess.offersAccounts(AccountServer.UNKNOWN))
    }

    @Test
    fun `el error de solo lectura no se muestra como error inesperado`() {
        assertEquals(R.string.err_accounts_unavailable, ErrorHandler.errorMessageRes(readOnly, networkAvailable = true))
        // El resto sigue igual que antes.
        val bad = AppwriteException("Invalid credentials", 401, "user_invalid_credentials", null)
        assertEquals(R.string.err_invalid_credentials, ErrorHandler.errorMessageRes(bad, networkAvailable = true))
        assertEquals(R.string.err_no_internet, ErrorHandler.errorMessageRes(readOnly, networkAvailable = false))
    }
}

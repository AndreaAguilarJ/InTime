package com.momentummm.app.util

import com.momentummm.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del mapeo puro de errores a recursos ([ErrorHandler.errorMessageRes]).
 *
 * Guarda una clase de fallo invisible al compilar: si alguien reordena las ramas
 * del `when` o cambia una palabra clave, el usuario vería el mensaje de error
 * equivocado en pantalla. Cada caso fija la palabra clave real que devuelve
 * Appwrite/Android y el recurso que le corresponde.
 */
class ErrorHandlerTest {

    @Test
    fun `sin conexion gana sobre cualquier otro error`() {
        // Aunque el mensaje diga "timeout", si no hay red debe primar "sin internet".
        assertEquals(
            R.string.err_no_internet,
            ErrorHandler.errorMessageRes("connection timeout", networkAvailable = false)
        )
    }

    @Test
    fun `credenciales invalidas`() {
        assertEquals(R.string.err_invalid_credentials, ErrorHandler.errorMessageRes("invalid credentials", true))
        assertEquals(R.string.err_invalid_credentials, ErrorHandler.errorMessageRes("error: invalid_credentials (401)", true))
    }

    @Test
    fun `usuario no encontrado`() {
        assertEquals(R.string.err_user_not_found, ErrorHandler.errorMessageRes("user_not_found", true))
        assertEquals(R.string.err_user_not_found, ErrorHandler.errorMessageRes("the user not found", true))
    }

    @Test
    fun `usuario ya existe`() {
        assertEquals(R.string.err_user_exists, ErrorHandler.errorMessageRes("user_already_exists", true))
        assertEquals(R.string.err_user_exists, ErrorHandler.errorMessageRes("a user with the same id already exists", true))
    }

    @Test
    fun `contrasena invalida requiere ambas palabras`() {
        assertEquals(R.string.err_password_invalid, ErrorHandler.errorMessageRes("password is invalid", true))
        // Solo "password" sin "invalid" no debe mapear a contraseña inválida.
        assertEquals(R.string.err_unexpected, ErrorHandler.errorMessageRes("password too short", true))
    }

    @Test
    fun `email invalido requiere ambas palabras`() {
        assertEquals(R.string.err_email_invalid, ErrorHandler.errorMessageRes("email format is invalid", true))
    }

    @Test
    fun `demasiados intentos`() {
        assertEquals(R.string.err_rate_limit, ErrorHandler.errorMessageRes("rate_limit exceeded", true))
        assertEquals(R.string.err_rate_limit, ErrorHandler.errorMessageRes("too many requests", true))
    }

    @Test
    fun `autenticacion, red y timeout`() {
        assertEquals(R.string.err_authentication, ErrorHandler.errorMessageRes("authentication failure", true))
        assertEquals(R.string.err_network, ErrorHandler.errorMessageRes("network error", true))
        assertEquals(R.string.err_timeout, ErrorHandler.errorMessageRes("operation timeout", true))
    }

    @Test
    fun `mensaje vacio o desconocido cae en error inesperado`() {
        assertEquals(R.string.err_unexpected, ErrorHandler.errorMessageRes("", true))
        assertEquals(R.string.err_unexpected, ErrorHandler.errorMessageRes("algo raro 500", true))
    }
}

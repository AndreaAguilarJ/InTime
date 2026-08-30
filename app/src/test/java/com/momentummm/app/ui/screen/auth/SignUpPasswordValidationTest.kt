package com.momentummm.app.ui.screen.auth

import java.lang.reflect.Method
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Caracteriza la validación privada que habilita el botón de registro. */
class SignUpPasswordValidationTest {

    @Test
    fun `siete caracteres no son suficientes`() {
        assertFalse(isValidPassword("1234567"))
    }

    @Test
    fun `ocho caracteres son aceptados`() {
        assertTrue(isValidPassword("12345678"))
    }

    @Test
    fun `la validacion cuenta caracteres unicode`() {
        assertTrue(isValidPassword("🔒🔒🔒🔒🔒🔒🔒🔒"))
    }

    private fun isValidPassword(password: String): Boolean =
        validator.invoke(null, password) as Boolean

    private companion object {
        val validator: Method = Class.forName(
            "com.momentummm.app.ui.screen.auth.AuthScreensKt"
        ).getDeclaredMethod("isValidPassword", String::class.java).apply {
            isAccessible = true
        }
    }
}

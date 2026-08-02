package com.momentummm.app.notification

import com.momentummm.app.data.entity.MessageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que los mensajes motivacionales se personalizan de verdad.
 *
 * El bug de partida: la notificación mostraba el texto de la tabla semilla tal
 * cual, idéntico para todos los usuarios, aunque la app se anunciara con
 * "mensajes personalizados". Aquí se comprueban las dos garantías que hacen
 * falta para que la personalización sea segura:
 *
 *  1. Con datos del usuario, aparecen en el texto.
 *  2. Sin datos del usuario, el mensaje sigue leyéndose bien (nunca queda un
 *     "Hola {nombre}" ni un "Hola ," delante de las narices del usuario).
 */
class MessageTemplatesTest {

    private val withName = MessageTemplates.UserContext(
        name = "Andrea",
        streak = 5,
        level = 3,
        timeCoins = 120,
        screenTimeMinutesToday = 95
    )

    private val anonymous = MessageTemplates.UserContext()

    // ─── Marcadores ───────────────────────────────────────────────────────

    @Test
    fun `el nombre sustituye al marcador`() {
        val result = MessageTemplates.applyPlaceholders("Hola {nombre}, ¿listo?", withName, 10)

        assertEquals("Hola Andrea, ¿listo?", result)
    }

    @Test
    fun `sin nombre el marcador desaparece sin dejar restos`() {
        val result = MessageTemplates.applyPlaceholders("Hola {nombre}, ¿listo?", anonymous, 10)

        assertEquals("Hola, ¿listo?", result)
        assertFalse("No debe quedar el marcador sin sustituir", result.contains("{"))
        assertFalse("No deben quedar espacios dobles", result.contains("  "))
    }

    @Test
    fun `sin nombre no queda una coma huerfana al principio`() {
        val result = MessageTemplates.applyPlaceholders("{nombre}, hoy es tu día", anonymous, 10)

        assertEquals("hoy es tu día", result)
    }

    @Test
    fun `la racha el nivel y las monedas se sustituyen por datos reales`() {
        val result = MessageTemplates.applyPlaceholders(
            "Racha {racha}, nivel {nivel}, monedas {monedas}",
            withName,
            10
        )

        assertEquals("Racha 5, nivel 3, monedas 120", result)
    }

    @Test
    fun `el tiempo de pantalla se formatea en horas y minutos`() {
        val result = MessageTemplates.applyPlaceholders("Hoy: {pantalla}", withName, 10)

        assertEquals("Hoy: 1h 35min", result)
    }

    @Test
    fun `los marcadores en ingles tambien funcionan`() {
        val result = MessageTemplates.applyPlaceholders(
            "Hi {name}, streak {streak}, level {level}",
            withName,
            10
        )

        assertEquals("Hi Andrea, streak 5, level 3", result)
    }

    @Test
    fun `los marcadores no distinguen mayusculas`() {
        val result = MessageTemplates.applyPlaceholders("Hola {NOMBRE}", withName, 10)

        assertEquals("Hola Andrea", result)
    }

    @Test
    fun `un mensaje sin marcadores se entrega intacto`() {
        val original = "Tu tiempo es tu vida. Recupéralo un minuto a la vez."

        assertEquals(original, MessageTemplates.applyPlaceholders(original, withName, 10))
        assertEquals(original, MessageTemplates.applyPlaceholders(original, anonymous, 10))
    }

    @Test
    fun `un nombre con caracteres especiales de regex no rompe la sustitucion`() {
        // Un nombre con $ o \ reventaría una sustitución hecha a lo bruto.
        val tricky = MessageTemplates.UserContext(name = "A\$B\\C")

        val result = MessageTemplates.applyPlaceholders("Hola {nombre}", tricky, 10)

        assertEquals("Hola A\$B\\C", result)
    }

    // ─── Saludo según la hora ─────────────────────────────────────────────

    @Test
    fun `el saludo depende de la hora del dia`() {
        assertEquals("Buenos días", MessageTemplates.greeting(8))
        assertEquals("Buenas tardes", MessageTemplates.greeting(15))
        assertEquals("Buenas noches", MessageTemplates.greeting(23))
        assertEquals("Buenas noches", MessageTemplates.greeting(3))
    }

    @Test
    fun `el marcador de saludo usa la hora recibida`() {
        assertEquals(
            "Buenas tardes, Andrea",
            MessageTemplates.applyPlaceholders("{saludo}, {nombre}", withName, 16)
        )
    }

    // ─── Título ───────────────────────────────────────────────────────────

    @Test
    fun `el titulo se dirige al usuario por su nombre`() {
        assertEquals(
            "☀️ Buenos días, Andrea",
            MessageTemplates.buildTitle("☀️ Buenos días", withName)
        )
    }

    @Test
    fun `sin nombre el titulo se queda como estaba`() {
        assertEquals(
            "☀️ Buenos días",
            MessageTemplates.buildTitle("☀️ Buenos días", anonymous)
        )
    }

    @Test
    fun `el nombre no se repite si el titulo ya lo incluye`() {
        val title = "Buenos días, Andrea"

        assertEquals(title, MessageTemplates.buildTitle(title, withName))
    }

    @Test
    fun `un nombre en blanco se trata como ausente`() {
        val blank = MessageTemplates.UserContext(name = "   ")

        assertEquals("Motivación", MessageTemplates.buildTitle("Motivación", blank))
    }

    // ─── Línea de contexto ────────────────────────────────────────────────

    @Test
    fun `la racha se menciona cuando merece la pena`() {
        val line = MessageTemplates.buildContextLine(withName, MessageCategory.MOTIVATION)

        assertTrue("Debe mencionar la racha", line?.contains("5 días") == true)
    }

    @Test
    fun `una racha de un solo dia no se menciona`() {
        val oneDay = MessageTemplates.UserContext(streak = 1)

        assertNull(MessageTemplates.buildContextLine(oneDay, MessageCategory.MOTIVATION))
    }

    @Test
    fun `por la noche se informa del tiempo de pantalla`() {
        val noStreak = MessageTemplates.UserContext(screenTimeMinutesToday = 240)

        val line = MessageTemplates.buildContextLine(noStreak, MessageCategory.EVENING)

        assertTrue("Debe informar del tiempo de pantalla", line?.contains("4h") == true)
    }

    @Test
    fun `sin datos no se inventa una linea de contexto`() {
        assertNull(MessageTemplates.buildContextLine(anonymous, MessageCategory.FOCUS))
    }

    // ─── Formato de duración ──────────────────────────────────────────────

    @Test
    fun `las duraciones se formatean de forma legible`() {
        assertEquals("0 min", MessageTemplates.formatMinutes(0))
        assertEquals("0 min", MessageTemplates.formatMinutes(-10))
        assertEquals("45 min", MessageTemplates.formatMinutes(45))
        assertEquals("2h", MessageTemplates.formatMinutes(120))
        assertEquals("2h 5min", MessageTemplates.formatMinutes(125))
    }
}

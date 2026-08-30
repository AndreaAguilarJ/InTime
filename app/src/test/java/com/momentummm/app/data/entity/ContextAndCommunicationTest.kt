package com.momentummm.app.data.entity

import com.momentummm.app.util.ContextSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del Bloqueo por contexto y del Modo solo comunicación.
 *
 * Las dos funciones tenían fallos de semántica más que de cálculo: reglas de
 * ubicación y Wi-Fi que el motor descartaba, una lista vacía interpretada como
 * «bloquea todo», y un SSID censurado por el sistema que se habría guardado
 * como si fuera un nombre de red válido.
 */
class ContextAndCommunicationTest {

    // ── Saneado del SSID ─────────────────────────────────────────────────

    @Test
    fun `el ssid llega entre comillas y se limpia`() {
        assertEquals("MiRed", ContextSnapshot.sanitizeSsid("\"MiRed\""))
    }

    @Test
    fun `un ssid desconocido no es un nombre de red valido`() {
        // Sin permiso de ubicación Android devuelve este literal. Guardarlo
        // crearía una regla que no puede coincidir con nada.
        assertNull(ContextSnapshot.sanitizeSsid("<unknown ssid>"))
    }

    @Test
    fun `un ssid vacio o en blanco se descarta`() {
        assertNull(ContextSnapshot.sanitizeSsid(""))
        assertNull(ContextSnapshot.sanitizeSsid("   "))
        assertNull(ContextSnapshot.sanitizeSsid("\"\""))
        assertNull(ContextSnapshot.sanitizeSsid(null))
    }

    @Test
    fun `un ssid normal se conserva tal cual`() {
        assertEquals("Casa 5G", ContextSnapshot.sanitizeSsid("Casa 5G"))
    }

    // ── Tipos de regla de contexto ───────────────────────────────────────

    private fun rule(
        type: String,
        enabled: Boolean = true,
        blockAll: Boolean = false,
        apps: String = ""
    ) = ContextBlockRule(
        ruleName = "prueba",
        isEnabled = enabled,
        contextType = type,
        blockCompletely = blockAll,
        affectedApps = apps,
        applyToAllLimitedApps = apps.isBlank()
    )

    @Test
    fun `una regla de ubicacion no se evalua por horario`() {
        // isActiveBySchedule sólo tiene sentido para SCHEDULE; para ubicación y
        // Wi-Fi la coincidencia la decide ContextBlockingService.
        assertFalse(rule("LOCATION").isActiveBySchedule())
        assertFalse(rule("WIFI").isActiveBySchedule())
    }

    @Test
    fun `una regla de horario que cubre todo el dia esta activa`() {
        val allDay = ContextBlockRule(
            ruleName = "todo el día",
            contextType = "SCHEDULE",
            scheduleStartHour = 0,
            scheduleStartMinute = 0,
            scheduleEndHour = 23,
            scheduleEndMinute = 59,
            scheduleDaysOfWeek = "1,2,3,4,5,6,7"
        )
        val calendar = java.util.Calendar.getInstance()
        val isLastMinute = calendar.get(java.util.Calendar.HOUR_OF_DAY) == 23 &&
            calendar.get(java.util.Calendar.MINUTE) == 59
        if (!isLastMinute) {
            assertTrue(allDay.isActiveBySchedule())
        }
    }

    @Test
    fun `una regla de horario sin dias nunca esta activa`() {
        val noDays = ContextBlockRule(
            ruleName = "sin días",
            contextType = "SCHEDULE",
            scheduleStartHour = 0,
            scheduleEndHour = 23,
            scheduleEndMinute = 59,
            scheduleDaysOfWeek = ""
        )
        assertFalse(noDays.isActiveBySchedule())
    }

    // ── Alcance de apps de una regla ─────────────────────────────────────

    @Test
    fun `una regla sin apps concretas aplica a todas las limitadas`() {
        val general = rule("SCHEDULE")
        assertTrue(general.applyToAllLimitedApps)
        assertTrue(general.getAffectedAppsList().isEmpty())
    }

    @Test
    fun `una regla con apps concretas solo afecta a esas`() {
        val targeted = rule("SCHEDULE", apps = "com.instagram.android,com.whatsapp")
        val affected = targeted.getAffectedAppsList()
        assertEquals(2, affected.size)
        assertTrue(affected.contains("com.instagram.android"))
        assertFalse(affected.contains("com.google.android.youtube"))
    }

    // ── Modo solo comunicación ───────────────────────────────────────────

    @Test
    fun `una lista de apps permitidas vacia no significa bloquear todo`() {
        // Es la semántica peligrosa que había: con el modo activo y la lista
        // vacía, el monitor bloqueaba TODA app de usuario y el teléfono quedaba
        // inutilizable. El significado correcto es «aún no he elegido».
        val config = SmartBlockingConfig(
            communicationOnlyModeEnabled = true,
            communicationOnlyApps = ""
        )
        assertTrue(config.getCommunicationOnlyAppsList().isEmpty())
    }

    @Test
    fun `las apps permitidas se recuperan de la cadena guardada`() {
        val config = SmartBlockingConfig(
            communicationOnlyModeEnabled = true,
            communicationOnlyApps = "com.whatsapp,org.telegram.messenger"
        )
        val apps = config.getCommunicationOnlyAppsList()
        assertEquals(2, apps.size)
        assertTrue(apps.contains("com.whatsapp"))
        assertTrue(apps.contains("org.telegram.messenger"))
    }

    @Test
    fun `las cuatro casillas de contenido nacen activadas`() {
        // Si alguna naciera en false, el modo prometería bloquear feeds y reels
        // sin hacerlo hasta que el usuario tocara el interruptor.
        val defaults = SmartBlockingConfig.DEFAULT
        assertTrue(defaults.communicationOnlyBlockFeed)
        assertTrue(defaults.communicationOnlyBlockStories)
        assertTrue(defaults.communicationOnlyBlockReels)
        assertTrue(defaults.communicationOnlyAllowDMs)
    }

    // ── Interruptor maestro de contexto ──────────────────────────────────

    @Test
    fun `el bloqueo por contexto nace apagado`() {
        assertFalse(SmartBlockingConfig.DEFAULT.contextBlockingEnabled)
    }
}

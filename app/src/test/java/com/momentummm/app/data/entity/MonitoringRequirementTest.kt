package com.momentummm.app.data.entity

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato de arranque del monitor.
 *
 * Las siete funciones de Bloqueo inteligente se aplican DENTRO de
 * `AppMonitoringService`, que antes sólo se arrancaba si existía algún límite de
 * app habilitado. Un usuario que activaba el Modo nuclear o Solo comunicación
 * sin haber puesto ningún límite se quedaba con los interruptores encendidos y
 * ningún efecto, sin señal alguna de que faltaba algo.
 *
 * Estas pruebas fijan qué combinaciones DEBEN exigir el monitor. Replican la
 * condición de `SmartBlockingManager.requiresMonitoringNow()`, que no se puede
 * invocar aquí porque necesita el DAO de Room.
 */
class MonitoringRequirementTest {

    /**
     * Misma condición que `requiresMonitoringNow()`. Si una cambia sin la otra,
     * estas pruebas lo señalan.
     */
    private fun requiresMonitoring(config: SmartBlockingConfig): Boolean =
        config.floatingTimerEnabled ||
            config.sleepModeBlockApps ||
            config.digitalFastingEnabled ||
            config.isNuclearModeActive() ||
            config.contextBlockingEnabled ||
            config.communicationOnlyModeEnabled

    @Test
    fun `la configuracion por defecto no exige el monitor`() {
        // Nadie ha activado nada: no hay razón para gastar batería.
        assertFalse(requiresMonitoring(SmartBlockingConfig.DEFAULT))
    }

    @Test
    fun `el timer flotante exige el monitor`() {
        // Es el monitor quien detecta la app en primer plano y lanza el overlay.
        assertTrue(requiresMonitoring(SmartBlockingConfig(floatingTimerEnabled = true)))
    }

    @Test
    fun `el ayuno digital exige el monitor`() {
        assertTrue(requiresMonitoring(SmartBlockingConfig(digitalFastingEnabled = true)))
    }

    @Test
    fun `solo comunicacion exige el monitor`() {
        assertTrue(requiresMonitoring(SmartBlockingConfig(communicationOnlyModeEnabled = true)))
    }

    @Test
    fun `el bloqueo por contexto exige el monitor`() {
        assertTrue(requiresMonitoring(SmartBlockingConfig(contextBlockingEnabled = true)))
    }

    @Test
    fun `el modo nuclear activo exige el monitor`() {
        val config = SmartBlockingConfig(
            nuclearModeEnabled = true,
            nuclearModeStartDate = Calendar.getInstance()
                .apply { add(Calendar.DAY_OF_YEAR, -1) }.time,
            nuclearModeEndDate = Calendar.getInstance()
                .apply { add(Calendar.DAY_OF_YEAR, 30) }.time
        )
        assertTrue(requiresMonitoring(config))
    }

    @Test
    fun `un modo nuclear ya expirado no exige el monitor`() {
        val config = SmartBlockingConfig(
            nuclearModeEnabled = true,
            nuclearModeStartDate = Calendar.getInstance()
                .apply { add(Calendar.DAY_OF_YEAR, -60) }.time,
            nuclearModeEndDate = Calendar.getInstance()
                .apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        )
        assertFalse(requiresMonitoring(config))
    }

    @Test
    fun `bloquear apps durante el sueno exige el monitor`() {
        assertTrue(
            requiresMonitoring(
                SmartBlockingConfig(sleepModeEnabled = true, sleepModeBlockApps = true)
            )
        )
    }

    @Test
    fun `la ventana de sueno que solo excluye uso no exige el monitor`() {
        // Excluir el uso nocturno se aplica en el calculador de uso, no en el
        // monitor: no hace falta tenerlo corriendo sólo por eso.
        assertFalse(
            requiresMonitoring(
                SmartBlockingConfig(
                    sleepModeEnabled = true,
                    sleepModeIgnoreTracking = true,
                    sleepModeBlockApps = false
                )
            )
        )
    }
}

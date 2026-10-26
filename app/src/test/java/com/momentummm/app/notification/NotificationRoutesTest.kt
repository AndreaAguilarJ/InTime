package com.momentummm.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tocar una notificación debe llevar a una pantalla que EXISTE.
 *
 * El extra llega de fuera de la composición: un valor desconocido no puede
 * llegar a `navController.navigate`, que lanza IllegalArgumentException.
 */
class NotificationRoutesTest {

    @Test
    fun `las rutas conocidas se aceptan tal cual`() {
        listOf(
            NotificationRoutes.TODAY, NotificationRoutes.ANALYTICS, NotificationRoutes.FOCUS,
            NotificationRoutes.MY_LIFE, NotificationRoutes.APP_LIMITS, NotificationRoutes.WEBSITE_BLOCKS,
            NotificationRoutes.GAMIFICATION, NotificationRoutes.NOTIFICATION_SETTINGS,
            NotificationRoutes.MOTIVATIONAL_SETTINGS
        ).forEach { assertEquals(it, NotificationRoutes.sanitize(it)) }
    }

    @Test
    fun `los valores que escribian versiones anteriores se traducen`() {
        // Notificaciones ya publicadas en la bandeja antes de actualizar.
        assertEquals(NotificationRoutes.MOTIVATIONAL_SETTINGS, NotificationRoutes.sanitize("motivational_settings"))
        assertEquals(NotificationRoutes.TODAY, NotificationRoutes.sanitize("goals"))
    }

    @Test
    fun `un destino desconocido o vacio se descarta`() {
        assertNull(NotificationRoutes.sanitize("password_manage"))
        assertNull(NotificationRoutes.sanitize("../settings"))
        assertNull(NotificationRoutes.sanitize(""))
        assertNull(NotificationRoutes.sanitize(null))
    }
}

package com.momentummm.shared.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Avisos locales de iOS sobre `UNUserNotificationCenter`.
 */
class UserNotificationsNotifier : MomentumNotifier {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun requestPermission() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> /* La respuesta llega a la pantalla vía el estado del sistema. */ }
    }

    override fun scheduleAfter(id: String, seconds: Int, title: String, body: String) {
        // iOS rechaza un intervalo de cero o negativo; por debajo de un segundo el
        // aviso no tendría sentido de todos modos.
        if (seconds < 1) return

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = seconds.toDouble(),
            repeats = false,
        )
        // Reusar el identificador sustituye el aviso anterior, que es justo lo que
        // hace falta al reiniciar una sesión: no se acumulan avisos huérfanos.
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request) { _ -> }
    }

    override fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }
}

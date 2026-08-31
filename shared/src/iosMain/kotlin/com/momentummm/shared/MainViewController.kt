package com.momentummm.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.momentummm.shared.data.UserDefaultsSettingsStore
import com.momentummm.shared.notification.UserNotificationsNotifier
import com.momentummm.shared.ui.MomentumApp
import platform.UIKit.UIViewController

/**
 * Punto de entrada que consume el proyecto Xcode.
 *
 * Envuelve la interfaz Compose compartida en un `UIViewController` para que
 * SwiftUI la aloje como cualquier otra vista de iOS, e inyecta las piezas propias
 * de la plataforma: los ajustes sobre `NSUserDefaults` y los avisos sobre
 * `UNUserNotificationCenter`. Es la única costura entre el mundo Swift y el mundo
 * Kotlin.
 */
fun MainViewController(): UIViewController {
    val notifier = UserNotificationsNotifier()
    // Se pide al arrancar y no al iniciar la primera sesión: si el permiso se
    // pidiera con el cronómetro ya en marcha, el diálogo del sistema aparecería
    // encima de la cuenta atrás y el aviso podría quedarse sin programar.
    notifier.requestPermission()

    return ComposeUIViewController {
        MomentumApp(
            store = UserDefaultsSettingsStore(),
            notifier = notifier,
        )
    }
}

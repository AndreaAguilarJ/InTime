package com.momentummm.shared

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.window.ComposeUIViewController
import com.momentummm.shared.data.UserDefaultsSettingsStore
import com.momentummm.shared.notification.UserNotificationsNotifier
import com.momentummm.shared.ui.MomentumApp
import platform.Foundation.NSBundle
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
@OptIn(ExperimentalComposeApi::class)
fun MainViewController(): UIViewController {
    val notifier = UserNotificationsNotifier()
    // Se pide al arrancar y no al iniciar la primera sesión: si el permiso se
    // pidiera con el cronómetro ya en marcha, el diálogo del sistema aparecería
    // encima de la cuenta atrás y el aviso podría quedarse sin programar.
    notifier.requestPermission()

    return ComposeUIViewController(
        configure = {
            // Compose en iOS solo publica su árbol de accesibilidad cuando detecta
            // que hay un lector de pantalla activo. Esa detección no es fiable:
            // con VoiceOver apagado el árbol está vacío, y también lo está para
            // cualquier herramienta que inspeccione la interfaz. Publicarlo siempre
            // es lo que hace que la app sea navegable con VoiceOver --requisito de
            // accesibilidad de la App Store-- y comprobable de forma automática.
            accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
        },
    ) {
        MomentumApp(
            store = UserDefaultsSettingsStore(),
            notifier = notifier,
            appVersion = versionDelPaquete(),
        )
    }
}

/**
 * Versión que declara el propio paquete de la app.
 *
 * Se lee del `Info.plist` en vez de escribirla en el código para que no puedan
 * discrepar: la que ve la persona en Ajustes es exactamente la que se subió.
 */
private fun versionDelPaquete(): String {
    val bundle = NSBundle.mainBundle
    val corta = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
    return when {
        corta != null && build != null -> "$corta ($build)"
        corta != null -> corta
        else -> "—"
    }
}

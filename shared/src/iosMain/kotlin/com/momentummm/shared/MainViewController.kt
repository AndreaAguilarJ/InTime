package com.momentummm.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.momentummm.shared.data.UserDefaultsSettingsStore
import com.momentummm.shared.ui.MomentumApp
import platform.UIKit.UIViewController

/**
 * Punto de entrada que consume el proyecto Xcode.
 *
 * Envuelve la interfaz Compose compartida en un `UIViewController` para que
 * SwiftUI la aloje como cualquier otra vista de iOS, e inyecta el almacén de
 * ajustes propio de la plataforma. Es la única costura entre el mundo Swift y el
 * mundo Kotlin.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    MomentumApp(store = UserDefaultsSettingsStore())
}

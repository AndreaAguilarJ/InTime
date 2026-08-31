package com.momentummm.shared

import platform.UIKit.UIDevice

actual object Plataforma {
    actual val nombre: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    actual val esIOS: Boolean = true
}

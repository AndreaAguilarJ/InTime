package com.momentummm.shared

import android.os.Build

actual object Plataforma {
    actual val nombre: String = "Android ${Build.VERSION.SDK_INT}"
    actual val esIOS: Boolean = false
}

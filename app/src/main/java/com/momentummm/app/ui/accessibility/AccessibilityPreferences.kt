package com.momentummm.app.ui.accessibility

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Devuelve `false` cuando el usuario ha desactivado las animaciones del sistema.
 *
 * Se observa el ajuste global para que un cambio desde Accesibilidad se aplique sin
 * reiniciar la app. Si el fabricante impide leerlo, se conserva el comportamiento
 * animado existente en vez de fallar la composición.
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    var enabled by remember(context) { mutableStateOf(readAnimationsEnabled(context)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = readAnimationsEnabled(context)
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        enabled = readAnimationsEnabled(context)

        onDispose { resolver.unregisterContentObserver(observer) }
    }

    return enabled
}

private fun readAnimationsEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) > 0f
}.getOrDefault(true)

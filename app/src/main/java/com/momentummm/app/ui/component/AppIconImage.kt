package com.momentummm.app.ui.component

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icono REAL de una aplicación, cargado del sistema a partir de su
 * `packageName` (el icono de Instagram, WhatsApp, TikTok…).
 *
 * Antes las listas de la app dibujaban un cuadro de color con la inicial ("I",
 * "F", "W"…) escrito a mano; se veían todos iguales y no eran los iconos reales.
 * Este componente pinta el icono verdadero.
 *
 * Detalles de calidad:
 * - La lectura del `PackageManager` ocurre FUERA del hilo principal
 *   (`produceState` + `Dispatchers.IO`), así que una lista larga no bloquea el
 *   desplazamiento.
 * - Está cacheado por `packageName`: al reciclar filas no recarga el mismo icono.
 * - Si la app no está instalada o el icono no se puede leer, cae al mismo
 *   mosaico de color + inicial de siempre, en vez de dejar un hueco.
 */
@Composable
fun AppIconImage(
    packageName: String,
    appName: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 12.dp,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)

    val iconBitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = packageName
    ) {
        value = if (packageName.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                    drawable.toBitmap(
                        width = drawable.intrinsicWidth.coerceAtLeast(1),
                        height = drawable.intrinsicHeight.coerceAtLeast(1)
                    ).asImageBitmap()
                } catch (e: PackageManager.NameNotFoundException) {
                    null // app no instalada → respaldo de inicial
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val bitmap = iconBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(fallbackColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

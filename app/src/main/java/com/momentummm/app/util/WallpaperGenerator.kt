package com.momentummm.app.util

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import com.momentummm.app.R
import com.momentummm.app.widget.WidgetFormat
import com.momentummm.app.widget.WidgetLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fondo de pantalla «Mi vida en semanas»: la cuadrícula de la app, a la medida de
 * la pantalla del teléfono.
 *
 * Arregla lo que hacía la versión anterior:
 * - título y leyenda en negro sobre el fondo por defecto (gris casi negro);
 * - textos fijos en español para todos los idiomas;
 * - 80 años y 1080 × 1920 siempre, ignorando la esperanza de vida elegida y la
 *   forma de la pantalla;
 * - todo en el hilo principal (generar, comprimir y aplicar tarda segundos);
 * - «Guardado en la galería» aunque no se hubiera guardado nada.
 */
object WallpaperGenerator {

    private const val TAG = "WallpaperGenerator"
    private const val FILE_NAME = "momentum_life_weeks"

    /** Lo que se dibuja; los colores llegan como se guardan en Ajustes (#RRGGBB). */
    data class Spec(
        val weeksLived: Int,
        val lifeYears: Int,
        val livedHex: String?,
        val futureHex: String?,
        val backgroundHex: String?,
    )

    private const val DEFAULT_LIVED = "#6366F1"
    private const val DEFAULT_FUTURE = "#E5E7EB"
    private const val DEFAULT_BACKGROUND = "#1F2937"

    /** Lo que muestra la app: semanas vividas y años según la esperanza elegida. */
    fun specFor(data: LifeWeeksData, livedHex: String?, futureHex: String?, backgroundHex: String?): Spec =
        Spec(
            weeksLived = data.weeksLived,
            lifeYears = data.totalWeeks / LifeWallpaperLayout.COLUMNS,
            livedHex = livedHex,
            futureHex = futureHex,
            backgroundHex = backgroundHex,
        )

    /** Guarda el fondo en Imágenes. @return false si no se pudo. */
    suspend fun saveToGallery(context: Context, spec: Spec): Boolean = runCatching {
        val bitmap = render(context, spec)
        withContext(Dispatchers.IO) { writeToGallery(context, bitmap) }
    }.onFailure { Log.e(TAG, "No se pudo guardar el fondo en la galería", it) }.getOrDefault(false)

    /** Lo pone como fondo de inicio y de bloqueo. @return false si no se pudo. */
    suspend fun setAsWallpaper(context: Context, spec: Spec): Boolean = runCatching {
        val bitmap = render(context, spec)
        withContext(Dispatchers.IO) { WallpaperManager.getInstance(context).setBitmap(bitmap) }
        true
    }.onFailure { Log.e(TAG, "No se pudo poner el fondo de pantalla", it) }.getOrDefault(false)

    private suspend fun render(context: Context, spec: Spec): Bitmap = withContext(Dispatchers.Default) {
        val (screenW, screenH) = screenSize(context)
        val layout = LifeWallpaperLayout.compute(screenW, screenH, spec.lifeYears)
        val localized = WidgetLocale.localized(context)
        draw(localized, layout, spec)
    }

    private fun draw(context: Context, layout: LifeWallpaperLayout.Layout, spec: Spec): Bitmap {
        val background = parse(spec.backgroundHex, DEFAULT_BACKGROUND)
        val lived = parse(spec.livedHex, DEFAULT_LIVED)
        val future = parse(spec.futureHex, DEFAULT_FUTURE)
        val textColor = LifeWallpaperLayout.textColorOn(background)

        val bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)

        val res = context.resources
        val locale = WidgetLocale.locale(context)
        val centerX = layout.width / 2f
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
        }

        text.textSize = layout.titleSize
        text.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(res.getString(R.string.my_life_in_weeks), centerX, layout.titleBaseline, text)

        // Sin antialias: puntos cuadrados alineados a píxel, nítidos.
        val dots = Paint().apply { style = Paint.Style.FILL }
        val totalWeeks = layout.rows * LifeWallpaperLayout.COLUMNS
        val weeksLived = spec.weeksLived.coerceIn(0, totalWeeks)
        val dot = layout.cellPx - layout.gapPx
        for (week in 0 until totalWeeks) {
            val x = layout.gridLeft + (week % LifeWallpaperLayout.COLUMNS) * layout.cellPx
            val y = layout.gridTop + (week / LifeWallpaperLayout.COLUMNS) * layout.cellPx
            dots.color = if (week < weeksLived) lived else future
            canvas.drawRect(x, y, x + dot, y + dot, dots)
        }

        text.textSize = layout.legendSize
        text.typeface = Typeface.DEFAULT
        val left = totalWeeks - weeksLived
        canvas.drawText(
            res.getQuantityString(R.plurals.wallpaper_weeks_lived, weeksLived, WidgetFormat.integer(weeksLived, locale)),
            centerX,
            layout.legendFirstBaseline,
            text,
        )
        canvas.drawText(
            res.getQuantityString(R.plurals.wallpaper_weeks_left, left, WidgetFormat.integer(left, locale)),
            centerX,
            layout.legendSecondBaseline,
            text,
        )
        return bitmap
    }

    /** Tamaño real de la pantalla, sin descontar barras del sistema. */
    private fun screenSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(WindowManager::class.java)
        if (windowManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
            return metrics.widthPixels to metrics.heightPixels
        }
        val fallback = context.resources.displayMetrics
        return fallback.widthPixels to fallback.heightPixels
    }

    /**
     * En Android 10+ se guarda en Imágenes sin permisos. En 8 y 9 hace falta el
     * permiso de almacenamiento, que la app no pide: se devuelve false para no
     * anunciar un guardado que no ocurrió.
     */
    private fun writeToGallery(context: Context, bitmap: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$FILE_NAME.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            // Pendiente hasta terminar: la galería no muestra una imagen a medias.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        }.getOrDefault(false)
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        return true
    }

    private fun parse(hex: String?, fallback: String): Int =
        runCatching { Color.parseColor(hex ?: fallback) }.getOrElse { Color.parseColor(fallback) }
}

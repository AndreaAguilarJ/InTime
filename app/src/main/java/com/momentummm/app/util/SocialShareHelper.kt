package com.momentummm.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SocialShareHelper - Maneja la generación de imágenes de "vergüenza" y el viral loop.
 * Implementa el patrón "Shame or Pay" para viralidad.
 */
@Singleton
class SocialShareHelper @Inject constructor() {

    companion object {
        private const val TAG = "SocialShareHelper"
        private const val IMAGE_WIDTH = 1080
        private const val IMAGE_HEIGHT = 1920
        
        // Duración del desbloqueo temporal tras compartir (5 minutos)
        const val UNLOCK_DURATION_MS = 5 * 60 * 1000L
        
        // Intent codes
        const val SHAME_SHARE_REQUEST_CODE = 9001
    }

    /**
     * Tipos de imagen de vergüenza para diferentes contextos
     */
    enum class ShameType(
        val emoji: String,
        val title: String,
        val subtitle: String
    ) {
        DOPAMINE_FAIL(
            emoji = "🤡",
            title = "Fallé mi dieta de dopamina",
            subtitle = "La recaída es parte del proceso... supongo"
        ),
        WEAK_MOMENT(
            emoji = "😔",
            title = "Tuve un momento de debilidad",
            subtitle = "Mañana será otro día..."
        ),
        FOCUS_BROKEN(
            emoji = "💀",
            title = "Rompí mi enfoque",
            subtitle = "El scroll me ganó"
        ),
        CRAVING_ATTACK(
            emoji = "🫠",
            title = "El craving me ganó",
            subtitle = "No pude resistirme"
        ),
        STREAK_LOST(
            emoji = "💔",
            title = "Perdí mi racha",
            subtitle = "Volver a empezar..."
        )
    }

    /**
     * Colores del tema para la imagen (sincronizado con ThemeManager)
     */
    data class ThemeColors(
        val primaryColor: String = "#6366F1",
        val backgroundColor: String = "#1A0505",
        val accentColor: String = "#FF4444",
        val textColor: String = "#FFFFFF"
    )

    /**
     * Genera un Bitmap de "vergüenza" para compartir en redes sociales.
     * Formato optimizado para Instagram Stories (1080x1920).
     */
    fun generateShameBitmap(
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0,
        themeColors: ThemeColors = ThemeColors()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fondo dramático con gradiente
        drawBackground(canvas, themeColors)

        // 2. Header con branding
        drawBrandHeader(canvas, themeColors)

        // 3. Emoji central grande
        drawCentralEmoji(canvas, shameType)

        // 4. Texto principal de vergüenza
        drawShameText(canvas, shameType, appName)

        // 5. Badge de racha rota (si aplica)
        if (streakDays > 0) {
            drawStreakBrokenBadge(canvas, streakDays)
        }

        // 6. Timestamp
        drawTimestamp(canvas)

        // 7. Call to Action viral
        drawViralCTA(canvas, themeColors)

        // 8. Watermark y hashtags
        drawWatermark(canvas)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, colors: ThemeColors) {
        val paint = Paint()
        
        val gradient = LinearGradient(
            0f, 0f, 0f, IMAGE_HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#1A0000"),
                Color.parseColor("#330000"),
                Color.parseColor("#1A0000"),
                Color.parseColor("#0D0D0D")
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), paint)

        // Líneas de glitch decorativas
        drawGlitchEffect(canvas)
    }

    private fun drawGlitchEffect(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF3333")
            alpha = 25
            strokeWidth = 2f
        }

        listOf(150f, 350f, 1100f, 1400f, 1650f).forEach { y ->
            canvas.drawLine(0f, y, IMAGE_WIDTH.toFloat(), y, paint)
        }
    }

    private fun drawBrandHeader(canvas: Canvas, colors: ThemeColors) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF4444")
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
        }

        canvas.drawText("⏰ INTIME", IMAGE_WIDTH / 2f, 100f, paint)

        paint.apply {
            color = Color.parseColor("#888888")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.35f
        }
        canvas.drawText("FOCUS & PRODUCTIVITY", IMAGE_WIDTH / 2f, 145f, paint)
    }

    private fun drawCentralEmoji(canvas: Canvas, shameType: ShameType) {
        val paint = Paint().apply {
            textSize = 300f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Glow effect
        val glowPaint = Paint().apply {
            color = Color.parseColor("#FF3333")
            alpha = 25
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(IMAGE_WIDTH / 2f, 450f, 220f, glowPaint)

        canvas.drawText(shameType.emoji, IMAGE_WIDTH / 2f, 550f, paint)
    }

    private fun drawShameText(canvas: Canvas, shameType: ShameType, appName: String) {
        // Título principal
        val mainPaint = Paint().apply {
            color = Color.WHITE
            textSize = 88f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val titleLines = shameType.title.split("\n")
        var yPosition = 730f
        titleLines.forEach { line ->
            // Dividir líneas largas
            val words = line.split(" ")
            if (words.size > 3) {
                val mid = words.size / 2
                canvas.drawText(words.subList(0, mid).joinToString(" "), IMAGE_WIDTH / 2f, yPosition, mainPaint)
                yPosition += 100f
                canvas.drawText(words.subList(mid, words.size).joinToString(" "), IMAGE_WIDTH / 2f, yPosition, mainPaint)
            } else {
                canvas.drawText(line, IMAGE_WIDTH / 2f, yPosition, mainPaint)
            }
            yPosition += 100f
        }

        // App que causó el fallo
        val appPaint = Paint().apply {
            color = Color.parseColor("#FF6666")
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📱 $appName", IMAGE_WIDTH / 2f, yPosition + 50f, appPaint)

        // Subtexto irónico
        val subPaint = Paint().apply {
            color = Color.parseColor("#999999")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\"${shameType.subtitle} 😅\"", IMAGE_WIDTH / 2f, yPosition + 130f, subPaint)
    }

    private fun drawStreakBrokenBadge(canvas: Canvas, streakDays: Int) {
        val badgeRect = RectF(
            IMAGE_WIDTH / 2f - 220f,
            1080f,
            IMAGE_WIDTH / 2f + 220f,
            1170f
        )

        val bgPaint = Paint().apply {
            color = Color.parseColor("#331111")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 25f, 25f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#FF4444")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 25f, 25f, borderPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔥 Racha de $streakDays días ROTA 💔", IMAGE_WIDTH / 2f, 1140f, textPaint)
    }

    private fun drawTimestamp(canvas: Canvas) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES"))
        val timestamp = dateFormat.format(Date())

        val paint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📅 $timestamp", IMAGE_WIDTH / 2f, 1280f, paint)
    }

    private fun drawViralCTA(canvas: Canvas, colors: ThemeColors) {
        val ctaRect = RectF(80f, 1380f, IMAGE_WIDTH - 80f, 1520f)
        
        val gradient = LinearGradient(
            ctaRect.left, ctaRect.top, ctaRect.right, ctaRect.bottom,
            Color.parseColor(colors.primaryColor),
            Color.parseColor("#8B5CF6"),
            Shader.TileMode.CLAMP
        )
        
        val bgPaint = Paint().apply {
            shader = gradient
            isAntiAlias = true
        }
        canvas.drawRoundRect(ctaRect, 30f, 30f, bgPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("¿Tú también luchas contra las distracciones?", IMAGE_WIDTH / 2f, 1435f, textPaint)
        
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Descarga InTime y únete al reto 💪", IMAGE_WIDTH / 2f, 1490f, textPaint)
    }

    private fun drawWatermark(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#555555")
            textSize = 26f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("@intime.app • #DopamineDetox #FocusMode #ProductivityTok", IMAGE_WIDTH / 2f, 1680f, paint)
        canvas.drawText("play.google.com/store/apps/details?id=com.momentummm.app", IMAGE_WIDTH / 2f, 1730f, paint)
    }

    /**
     * Guarda el bitmap como archivo temporal y devuelve el URI
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            
            val fileName = "intime_shame_${System.currentTimeMillis()}.png"
            val file = File(cachePath, fileName)
            
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: ${e.message}")
            null
        }
    }

    /**
     * Genera y abre el Intent de compartir genérico
     */
    fun shareShameImage(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0,
        themeColors: ThemeColors = ThemeColors()
    ): Boolean {
        val bitmap = generateShameBitmap(appName, shameType, streakDays, themeColors)
        val uri = saveBitmapToCache(context, bitmap) ?: return false

        val shareText = buildShareText(shameType, appName, streakDays)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartir mi momento de vergüenza 🤡")
        
        return try {
            if (context is Activity) {
                context.startActivityForResult(chooserIntent, SHAME_SHARE_REQUEST_CODE)
            } else {
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing: ${e.message}")
            false
        }
    }

    /**
     * Intent específico para Instagram Stories
     */
    fun shareToInstagramStories(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0,
        themeColors: ThemeColors = ThemeColors()
    ): Boolean {
        val bitmap = generateShameBitmap(appName, shameType, streakDays, themeColors)
        val uri = saveBitmapToCache(context, bitmap) ?: return false

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("source_application", context.packageName)
            putExtra("top_background_color", "#1A0000")
            putExtra("bottom_background_color", "#330000")
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback al share genérico
                shareShameImage(context, appName, shameType, streakDays, themeColors)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to Instagram: ${e.message}")
            false
        }
    }

    /**
     * Intent específico para Twitter/X
     */
    fun shareToTwitter(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0,
        themeColors: ThemeColors = ThemeColors()
    ): Boolean {
        val bitmap = generateShameBitmap(appName, shameType, streakDays, themeColors)
        val uri = saveBitmapToCache(context, bitmap) ?: return false
        
        val shareText = buildShareText(shameType, appName, streakDays)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            `package` = "com.twitter.android"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback
                shareShameImage(context, appName, shameType, streakDays, themeColors)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing to Twitter: ${e.message}")
            false
        }
    }

    private fun buildShareText(shameType: ShameType, appName: String, streakDays: Int): String {
        val streakText = if (streakDays > 0) " Perdí una racha de $streakDays días 💔" else ""
        
        return when (shameType) {
            ShameType.DOPAMINE_FAIL -> 
                "🤡 Fallé mi dieta de dopamina con $appName.$streakText\n\n¿Tú puedes hacerlo mejor? Descarga InTime 📱\n#DopamineDetox #FocusMode #InTimeApp"
            ShameType.WEAK_MOMENT -> 
                "😔 Tuve un momento de debilidad con $appName.$streakText\n\nMañana será otro día... con InTime 💪\n#DigitalWellbeing #InTimeApp"
            ShameType.FOCUS_BROKEN -> 
                "💀 Rompí mi enfoque scrolleando $appName.$streakText\n\n¿Quién más está luchando? 🙋‍♂️ #FocusMode #InTimeApp"
            ShameType.CRAVING_ATTACK -> 
                "🫠 El craving de $appName me ganó.$streakText\n\nLa lucha continúa... 💪 #DigitalMinimalism #InTimeApp"
            ShameType.STREAK_LOST ->
                "💔 Perdí mi racha de $streakDays días por $appName.\n\nA empezar de nuevo... 🔄 #StreakBroken #InTimeApp"
        }
    }

    /**
     * Data class para tracking del desbloqueo temporal
     */
    data class TemporaryUnlock(
        val packageName: String,
        val unlockTime: Long,
        val expirationTime: Long,
        val unlockMethod: UnlockMethod
    )

    enum class UnlockMethod {
        PAYMENT,
        SHAME_SHARE
    }
}

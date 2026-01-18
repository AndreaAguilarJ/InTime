package com.momentummm.app.util

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
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SocialShareUtils - Utilidad para generar imágenes virales de "vergüenza" y compartir en redes sociales.
 * Esta feature incentiva la viralidad al hacer que los usuarios compartan su "fallo" en redes sociales.
 */
object SocialShareUtils {

    private const val IMAGE_WIDTH = 1080
    private const val IMAGE_HEIGHT = 1920 // Formato Stories Instagram/TikTok

    /**
     * Tipos de imagen de vergüenza
     */
    enum class ShameType {
        DOPAMINE_FAIL,      // "Fallé mi dieta de dopamina"
        WEAK_MOMENT,        // "Tuve un momento de debilidad"
        CRAVING_ATTACK,     // "El craving me ganó"
        FOCUS_BROKEN        // "Rompí mi enfoque"
    }

    /**
     * Genera una imagen de "vergüenza" para compartir en redes sociales.
     * Diseñada para ser llamativa y viral.
     */
    fun generateShameImage(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo con gradiente oscuro/dramático
        drawDramaticBackground(canvas)

        // Logo/Brand de InTime en la parte superior
        drawBrandHeader(canvas)

        // Emoji grande central
        drawCentralEmoji(canvas, shameType)

        // Texto principal de vergüenza
        drawShameText(canvas, shameType, appName)

        // Estadísticas si había racha
        if (streakDays > 0) {
            drawBrokenStreakBadge(canvas, streakDays)
        }

        // Fecha y hora del "fallo"
        drawTimestamp(canvas)

        // Call to action sutil
        drawCallToAction(canvas)

        // Watermark de la app
        drawWatermark(canvas)

        return bitmap
    }

    private fun drawDramaticBackground(canvas: Canvas) {
        val paint = Paint()
        
        // Gradiente de fondo dramático (rojo oscuro a negro)
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

        // Efecto de viñeta
        val vignetteGradient = LinearGradient(
            IMAGE_WIDTH / 2f, 0f, IMAGE_WIDTH / 2f, IMAGE_HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#00000000"),
                Color.parseColor("#66000000")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = vignetteGradient
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), paint)

        // Líneas decorativas de "glitch"
        drawGlitchLines(canvas)
    }

    private fun drawGlitchLines(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF3333")
            alpha = 30
            strokeWidth = 2f
        }

        // Líneas horizontales aleatorias para efecto glitch
        val linePositions = listOf(200f, 450f, 1200f, 1500f, 1700f)
        linePositions.forEach { y ->
            canvas.drawLine(0f, y, IMAGE_WIDTH.toFloat(), y, paint)
        }
    }

    private fun drawBrandHeader(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF4444")
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }

        canvas.drawText("⏰ INTIME", IMAGE_WIDTH / 2f, 120f, paint)

        // Subtítulo
        paint.apply {
            color = Color.parseColor("#888888")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.3f
        }
        canvas.drawText("FOCUS & PRODUCTIVITY", IMAGE_WIDTH / 2f, 165f, paint)
    }

    private fun drawCentralEmoji(canvas: Canvas, shameType: ShameType) {
        val emoji = when (shameType) {
            ShameType.DOPAMINE_FAIL -> "🤡"
            ShameType.WEAK_MOMENT -> "😔"
            ShameType.CRAVING_ATTACK -> "🫠"
            ShameType.FOCUS_BROKEN -> "💀"
        }

        val paint = Paint().apply {
            textSize = 280f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(emoji, IMAGE_WIDTH / 2f, 550f, paint)

        // Círculo de fondo con efecto glow
        val glowPaint = Paint().apply {
            color = Color.parseColor("#FF3333")
            alpha = 20
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(IMAGE_WIDTH / 2f, 450f, 200f, glowPaint)
    }

    private fun drawShameText(canvas: Canvas, shameType: ShameType, appName: String) {
        val mainText = when (shameType) {
            ShameType.DOPAMINE_FAIL -> "Fallé mi dieta\nde dopamina"
            ShameType.WEAK_MOMENT -> "Tuve un momento\nde debilidad"
            ShameType.CRAVING_ATTACK -> "El craving\nme ganó"
            ShameType.FOCUS_BROKEN -> "Rompí\nmi enfoque"
        }

        // Texto principal grande
        val mainPaint = Paint().apply {
            color = Color.WHITE
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val lines = mainText.split("\n")
        var yPosition = 750f
        lines.forEach { line ->
            canvas.drawText(line, IMAGE_WIDTH / 2f, yPosition, mainPaint)
            yPosition += 110f
        }

        // App que causó el fallo
        val appPaint = Paint().apply {
            color = Color.parseColor("#FF6666")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("📱 $appName", IMAGE_WIDTH / 2f, yPosition + 80f, appPaint)

        // Subtexto motivacional/irónico
        val subPaint = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("\"La recaída es parte del proceso... supongo 😅\"", IMAGE_WIDTH / 2f, yPosition + 160f, subPaint)
    }

    private fun drawBrokenStreakBadge(canvas: Canvas, streakDays: Int) {
        // Badge de racha rota
        val badgeRect = RectF(
            IMAGE_WIDTH / 2f - 200f,
            1150f,
            IMAGE_WIDTH / 2f + 200f,
            1250f
        )

        val badgePaint = Paint().apply {
            color = Color.parseColor("#331111")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 25f, 25f, badgePaint)

        // Borde rojo
        val borderPaint = Paint().apply {
            color = Color.parseColor("#FF4444")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 25f, 25f, borderPaint)

        // Texto del badge
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("🔥 Racha de $streakDays días ROTA 💔", IMAGE_WIDTH / 2f, 1215f, textPaint)
    }

    private fun drawTimestamp(canvas: Canvas) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("es", "ES"))
        val timestamp = dateFormat.format(Date())

        val paint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 32f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("📅 $timestamp", IMAGE_WIDTH / 2f, 1350f, paint)
    }

    private fun drawCallToAction(canvas: Canvas) {
        // Caja con CTA
        val ctaRect = RectF(100f, 1450f, IMAGE_WIDTH - 100f, 1580f)
        
        val bgPaint = Paint().apply {
            val gradient = LinearGradient(
                ctaRect.left, ctaRect.top, ctaRect.right, ctaRect.bottom,
                Color.parseColor("#6366F1"),
                Color.parseColor("#8B5CF6"),
                Shader.TileMode.CLAMP
            )
            shader = gradient
            isAntiAlias = true
        }
        canvas.drawRoundRect(ctaRect, 30f, 30f, bgPaint)

        // Texto del CTA
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("¿Tú también luchas contra las distracciones?", IMAGE_WIDTH / 2f, 1505f, textPaint)
        
        textPaint.textSize = 36f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Descarga InTime y únete al reto 💪", IMAGE_WIDTH / 2f, 1555f, textPaint)
    }

    private fun drawWatermark(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#444444")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("@intime.app • #DopamineDetox #FocusMode", IMAGE_WIDTH / 2f, 1750f, paint)
        canvas.drawText("play.google.com/store/apps/details?id=com.momentummm.app", IMAGE_WIDTH / 2f, 1800f, paint)
    }

    /**
     * Guarda el bitmap en un archivo temporal y devuelve el URI para compartir.
     */
    fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): Uri? {
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
            e.printStackTrace()
            null
        }
    }

    /**
     * Abre el intent de compartir con la imagen generada.
     * Soporta Instagram Stories, Twitter, WhatsApp, etc.
     */
    fun shareShameImage(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0
    ): Boolean {
        val bitmap = generateShameImage(context, appName, shameType, streakDays)
        val uri = saveBitmapAndGetUri(context, bitmap) ?: return false

        val shareText = buildShareText(shameType, appName, streakDays)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartir mi momento de vergüenza 😅")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(chooserIntent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Intent específico para Instagram Stories
     */
    fun shareToInstagramStories(
        context: Context,
        appName: String,
        shameType: ShameType = ShameType.DOPAMINE_FAIL,
        streakDays: Int = 0
    ): Boolean {
        val bitmap = generateShameImage(context, appName, shameType, streakDays)
        val uri = saveBitmapAndGetUri(context, bitmap) ?: return false

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("source_application", context.packageName)
            // Color de fondo para Instagram Stories
            putExtra("top_background_color", "#1A0000")
            putExtra("bottom_background_color", "#0D0D0D")
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Si Instagram no está instalado, usar share genérico
            shareShameImage(context, appName, shameType, streakDays)
        }
    }

    private fun buildShareText(shameType: ShameType, appName: String, streakDays: Int): String {
        val baseMessage = when (shameType) {
            ShameType.DOPAMINE_FAIL -> "Fallé mi dieta de dopamina en InTime 🤡"
            ShameType.WEAK_MOMENT -> "Tuve un momento de debilidad... 😔"
            ShameType.CRAVING_ATTACK -> "El craving me ganó esta vez 🫠"
            ShameType.FOCUS_BROKEN -> "Rompí mi enfoque 💀"
        }

        val streakMessage = if (streakDays > 0) {
            "\n🔥 Racha de $streakDays días ROTA 💔"
        } else ""

        return """
            |$baseMessage
            |$streakMessage
            |
            |La app que me venció: $appName 📱
            |
            |¿Tú también luchas contra las distracciones? 
            |Descarga InTime y únete al reto 💪
            |
            |#InTime #DopamineDetox #FocusMode #Productividad
            |🔗 https://play.google.com/store/apps/details?id=com.momentummm.app
        """.trimMargin()
    }

    /**
     * Genera una imagen de "gloria" para cuando el usuario completa un reto.
     * Útil para compartir logros positivos (viralidad positiva).
     */
    fun generateGloryImage(
        context: Context,
        achievementTitle: String,
        streakDays: Int,
        totalFocusMinutes: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo con gradiente de éxito (verde/morado)
        drawGloryBackground(canvas)

        // Header
        drawBrandHeader(canvas)

        // Emoji de victoria
        val paint = Paint().apply {
            textSize = 280f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🏆", IMAGE_WIDTH / 2f, 550f, paint)

        // Texto de logro
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(achievementTitle, IMAGE_WIDTH / 2f, 750f, titlePaint)

        // Stats
        val statsPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔥 $streakDays días de racha", IMAGE_WIDTH / 2f, 900f, statsPaint)
        canvas.drawText("⏱️ ${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m de enfoque", IMAGE_WIDTH / 2f, 980f, statsPaint)

        // CTA
        drawCallToAction(canvas)
        drawWatermark(canvas)

        return bitmap
    }

    private fun drawGloryBackground(canvas: Canvas) {
        val paint = Paint()
        
        val gradient = LinearGradient(
            0f, 0f, 0f, IMAGE_HEIGHT.toFloat(),
            intArrayOf(
                Color.parseColor("#064E3B"),
                Color.parseColor("#065F46"),
                Color.parseColor("#0F172A"),
                Color.parseColor("#0D0D0D")
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), paint)
    }

    /**
     * Comparte imagen de gloria
     */
    fun shareGloryImage(
        context: Context,
        achievementTitle: String,
        streakDays: Int,
        totalFocusMinutes: Int
    ): Boolean {
        val bitmap = generateGloryImage(context, achievementTitle, streakDays, totalFocusMinutes)
        val uri = saveBitmapAndGetUri(context, bitmap) ?: return false

        val shareText = """
            |🏆 ¡Logro desbloqueado en InTime!
            |
            |$achievementTitle
            |
            |🔥 $streakDays días de racha
            |⏱️ ${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m de enfoque total
            |
            |¡Únete al movimiento de productividad! 💪
            |
            |#InTime #Productividad #FocusMode #DopamineDetox
            |🔗 https://play.google.com/store/apps/details?id=com.momentummm.app
        """.trimMargin()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartir mi logro 🏆")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        return try {
            context.startActivity(chooserIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

package com.momentummm.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException

object WallpaperGenerator {
    
    fun generateLifeWeeksWallpaper(
        context: Context,
        weeksLived: Int,
        livedColor: Int = Color.parseColor("#6366F1"),
        futureColor: Int = Color.parseColor("#E5E7EB"),
        backgroundColor: Int = Color.WHITE,
        width: Int = 1080,
        height: Int = 1920
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill background
        canvas.drawColor(backgroundColor)
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // Calculate grid dimensions
        val totalWeeks = 4160 // 80 years * 52 weeks
        val weeksPerRow = 52
        val totalRows = 80
        
        val gridWidth = width * 0.8f // 80% of screen width
        val gridHeight = height * 0.6f // 60% of screen height
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2
        
        val cellWidth = gridWidth / weeksPerRow
        val cellHeight = gridHeight / totalRows
        val cellSize = minOf(cellWidth, cellHeight) * 0.8f // Add some spacing
        
        // Draw title
        paint.apply {
            color = Color.BLACK
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Mi Vida en Semanas", width / 2f, startY - 60f, paint)
        
        // Draw grid
        for (week in 0 until totalWeeks) {
            val row = week / weeksPerRow
            val col = week % weeksPerRow
            
            val x = startX + col * cellWidth
            val y = startY + row * cellHeight
            
            paint.color = if (week < weeksLived) livedColor else futureColor
            
            canvas.drawRect(
                x,
                y,
                x + cellSize,
                y + cellSize,
                paint
            )
        }
        
        // Add legend
        paint.apply {
            color = Color.BLACK
            textSize = 32f
            textAlign = Paint.Align.LEFT
        }
        
        val legendY = startY + gridHeight + 80f
        canvas.drawText("Semanas vividas: $weeksLived", startX, legendY, paint)
        canvas.drawText("Semanas restantes: ${totalWeeks - weeksLived}", startX, legendY + 50f, paint)
        
        return bitmap
    }
    
    fun saveToGallery(context: Context, bitmap: Bitmap, filename: String = "momentum_life_weeks"): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }
    
    fun setAsWallpaper(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = android.app.WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap)
            true
        } catch (e: Exception) {
            false
        }
    }
}
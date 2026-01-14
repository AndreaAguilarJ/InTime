package com.momentummm.app.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ExportManager(private val context: Context) {
    
    data class UsageData(
        val appName: String,
        val packageName: String,
        val totalTime: Long,
        val openCount: Int,
        val date: String,
        val category: String? = null
    )
    
    data class FocusSessionData(
        val sessionType: String,
        val duration: Int,
        val completed: Boolean,
        val date: String,
        val startTime: String,
        val endTime: String?
    )
    
    data class GoalData(
        val goalType: String,
        val target: Int,
        val achieved: Int,
        val date: String,
        val completed: Boolean
    )
    
    suspend fun exportUsageDataToCsv(
        usageData: List<UsageData>,
        fileName: String = "usage_data_${getCurrentDateString()}.csv"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            val writer = CSVWriter(FileWriter(file))
            
            // Write header
            writer.writeNext(arrayOf(
                "Fecha",
                "Aplicación",
                "Paquete",
                "Tiempo Total (minutos)",
                "Número de Aperturas",
                "Categoría"
            ))
            
            // Write data
            usageData.forEach { data ->
                writer.writeNext(arrayOf(
                    data.date,
                    data.appName,
                    data.packageName,
                    (data.totalTime / 60000).toString(), // Convert to minutes
                    data.openCount.toString(),
                    data.category ?: "Sin categoría"
                ))
            }
            
            writer.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportFocusSessionsToCsv(
        sessionsData: List<FocusSessionData>,
        fileName: String = "focus_sessions_${getCurrentDateString()}.csv"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            val writer = CSVWriter(FileWriter(file))
            
            // Write header
            writer.writeNext(arrayOf(
                "Fecha",
                "Tipo de Sesión",
                "Duración (minutos)",
                "Completada",
                "Hora de Inicio",
                "Hora de Fin"
            ))
            
            // Write data
            sessionsData.forEach { session ->
                writer.writeNext(arrayOf(
                    session.date,
                    session.sessionType,
                    session.duration.toString(),
                    if (session.completed) "Sí" else "No",
                    session.startTime,
                    session.endTime ?: "No completada"
                ))
            }
            
            writer.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportCompleteReportToPdf(
        usageData: List<UsageData>,
        focusData: List<FocusSessionData>,
        goalsData: List<GoalData>,
        fileName: String = "reporte_completo_${getCurrentDateString()}.pdf"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Set up fonts
            val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            
            // Title
            document.add(
                Paragraph("Reporte de Bienestar Digital")
                    .setFont(titleFont)
                    .setFontSize(20f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Date range
            document.add(
                Paragraph("Período: ${getDateRange(usageData, focusData, goalsData)}")
                    .setFont(normalFont)
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30f)
            )
            
            // Usage Statistics Section
            document.add(
                Paragraph("Estadísticas de Uso de Aplicaciones")
                    .setFont(headerFont)
                    .setFontSize(16f)
                    .setMarginBottom(10f)
            )
            
            if (usageData.isNotEmpty()) {
                val usageTable = createUsageTable(usageData, headerFont, normalFont)
                document.add(usageTable)
            } else {
                document.add(Paragraph("No hay datos de uso disponibles.").setFont(normalFont))
            }
            
            document.add(Paragraph("\n"))
            
            // Focus Sessions Section
            document.add(
                Paragraph("Sesiones de Enfoque")
                    .setFont(headerFont)
                    .setFontSize(16f)
                    .setMarginBottom(10f)
            )
            
            if (focusData.isNotEmpty()) {
                val focusTable = createFocusTable(focusData, headerFont, normalFont)
                document.add(focusTable)
                
                // Focus statistics
                val completedSessions = focusData.count { it.completed }
                val totalSessions = focusData.size
                val completionRate = if (totalSessions > 0) (completedSessions * 100) / totalSessions else 0
                
                document.add(
                    Paragraph("Estadísticas de Enfoque:")
                        .setFont(headerFont)
                        .setFontSize(12f)
                        .setMarginTop(10f)
                )
                document.add(
                    Paragraph("• Sesiones completadas: $completedSessions de $totalSessions ($completionRate%)")
                        .setFont(normalFont)
                        .setFontSize(10f)
                )
            } else {
                document.add(Paragraph("No hay datos de sesiones de enfoque disponibles.").setFont(normalFont))
            }
            
            document.add(Paragraph("\n"))
            
            // Goals Section
            document.add(
                Paragraph("Progreso de Metas")
                    .setFont(headerFont)
                    .setFontSize(16f)
                    .setMarginBottom(10f)
            )
            
            if (goalsData.isNotEmpty()) {
                val goalsTable = createGoalsTable(goalsData, headerFont, normalFont)
                document.add(goalsTable)
            } else {
                document.add(Paragraph("No hay datos de metas disponibles.").setFont(normalFont))
            }
            
            // Footer
            document.add(
                Paragraph("\n\nReporte generado por Momentum el ${getCurrentDateString()}")
                    .setFont(normalFont)
                    .setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
            )
            
            document.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createUsageTable(usageData: List<UsageData>, headerFont: com.itextpdf.kernel.font.PdfFont, normalFont: com.itextpdf.kernel.font.PdfFont): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 35f, 20f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Headers
        table.addHeaderCell(Cell().add(Paragraph("Fecha").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Aplicación").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Tiempo (min)").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Aperturas").setFont(headerFont).setFontSize(10f)))
        
        // Data rows (limit to top 20 for PDF)
        usageData.take(20).forEach { data ->
            table.addCell(Cell().add(Paragraph(data.date).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(data.appName).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph((data.totalTime / 60000).toString()).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(data.openCount.toString()).setFont(normalFont).setFontSize(8f)))
        }
        
        return table
    }
    
    private fun createFocusTable(focusData: List<FocusSessionData>, headerFont: com.itextpdf.kernel.font.PdfFont, normalFont: com.itextpdf.kernel.font.PdfFont): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 30f, 15f, 15f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Headers
        table.addHeaderCell(Cell().add(Paragraph("Fecha").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Tipo").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Duración").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Completada").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Hora").setFont(headerFont).setFontSize(10f)))
        
        // Data rows
        focusData.forEach { session ->
            table.addCell(Cell().add(Paragraph(session.date).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(session.sessionType).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph("${session.duration} min").setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(if (session.completed) "Sí" else "No").setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(session.startTime).setFont(normalFont).setFontSize(8f)))
        }
        
        return table
    }
    
    private fun createGoalsTable(goalsData: List<GoalData>, headerFont: com.itextpdf.kernel.font.PdfFont, normalFont: com.itextpdf.kernel.font.PdfFont): Table {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 30f, 15f, 15f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Headers
        table.addHeaderCell(Cell().add(Paragraph("Fecha").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Tipo de Meta").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Objetivo").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Logrado").setFont(headerFont).setFontSize(10f)))
        table.addHeaderCell(Cell().add(Paragraph("Completada").setFont(headerFont).setFontSize(10f)))
        
        // Data rows
        goalsData.forEach { goal ->
            table.addCell(Cell().add(Paragraph(goal.date).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(goal.goalType).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(goal.target.toString()).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(goal.achieved.toString()).setFont(normalFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(if (goal.completed) "Sí" else "No").setFont(normalFont).setFontSize(8f)))
        }
        
        return table
    }
    
    private fun getDateRange(usageData: List<UsageData>, focusData: List<FocusSessionData>, goalsData: List<GoalData>): String {
        val allDates = mutableListOf<String>()
        allDates.addAll(usageData.map { it.date })
        allDates.addAll(focusData.map { it.date })
        allDates.addAll(goalsData.map { it.date })
        
        return if (allDates.isNotEmpty()) {
            val sortedDates = allDates.sorted()
            "${sortedDates.first()} - ${sortedDates.last()}"
        } else {
            getCurrentDateString()
        }
    }
    
    private fun getCurrentDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    fun shareFile(uri: Uri, fileName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                fileName.endsWith(".pdf") -> "application/pdf"
                fileName.endsWith(".csv") -> "text/csv"
                else -> "application/octet-stream"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Bienestar Digital")
            putExtra(Intent.EXTRA_TEXT, "Reporte generado por Momentum")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Compartir reporte")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
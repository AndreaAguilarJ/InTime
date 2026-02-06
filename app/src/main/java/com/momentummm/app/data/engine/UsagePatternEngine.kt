package com.momentummm.app.data.engine

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════════════════
// ██╗   ██╗███████╗ █████╗  ██████╗ ███████╗    ██████╗  █████╗ ████████╗
// ██║   ██║██╔════╝██╔══██╗██╔════╝ ██╔════╝    ██╔══██╗██╔══██╗╚══██╔══╝
// ██║   ██║███████╗███████║██║  ███╗█████╗      ██████╔╝███████║   ██║   
// ██║   ██║╚════██║██╔══██║██║   ██║██╔══╝      ██╔═══╝ ██╔══██║   ██║   
// ╚██████╔╝███████║██║  ██║╚██████╔╝███████╗    ██║     ██║  ██║   ██║   
//  ╚═════╝ ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝    ╚═╝     ╚═╝  ╚═╝   ╚═╝   
// PATTERN ENGINE - Análisis de patrones de uso con algoritmos ML-like
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Registro histórico de uso de una app por hora del día.
 * Se almacena para alimentar el motor de patrones.
 */
@Entity(
    tableName = "usage_pattern_records",
    indices = [
        Index(value = ["packageName", "date"]),
        Index(value = ["date"]),
        Index(value = ["hourOfDay"])
    ]
)
data class UsagePatternRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val date: Long,                          // Timestamp del día (inicio del día)
    val hourOfDay: Int,                      // 0-23
    val dayOfWeek: Int,                      // 1=Lunes, 7=Domingo
    val usageMinutes: Int,                   // Minutos usados en esa hora
    val openCount: Int = 0,                  // Veces que se abrió la app en esa hora
    val longestSessionMinutes: Int = 0,      // Sesión más larga en esa hora
    val wasBlocked: Boolean = false,         // Si fue bloqueada en algún momento
    val wasUnlockedByShame: Boolean = false,  // Si se desbloqueó con shame share
    val emotionalState: String? = null,       // Estado emocional autoreportado (opcional)
    val contextType: String? = null,          // WORK, HOME, COMMUTE, etc.
    val batteryLevel: Int = -1,              // Nivel de batería al momento
    val isWeekend: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Predicción generada por el motor de patrones.
 */
@Entity(
    tableName = "usage_predictions",
    indices = [Index(value = ["packageName", "predictionDate"])]
)
data class UsagePrediction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val predictionDate: Long,                // Para qué día es la predicción
    val predictedTotalMinutes: Int,          // Minutos totales predichos
    val predictedPeakHour: Int,              // Hora de mayor uso predicha
    val predictedPeakMinutes: Int,           // Minutos en hora pico
    val confidenceScore: Float,              // 0.0 - 1.0 confianza de la predicción
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val suggestedLimit: Int? = null,         // Límite sugerido inteligente
    val pattern: String = "",                // Descripción del patrón detectado
    val hourlyBreakdown: String = "",        // JSON con predicción por hora
    val createdAt: Long = System.currentTimeMillis()
)

enum class RiskLevel {
    MINIMAL,    // < 30 min/día, uso saludable
    LOW,        // 30-60 min/día, dentro de límites
    MODERATE,   // 60-120 min/día, vigilar
    HIGH,       // 120-240 min/día, preocupante
    CRITICAL,   // > 240 min/día, adicción potencial
    ESCALATING  // Tendencia ascendente fuerte
}

/**
 * Score de adicción por app - se calcula diariamente.
 */
@Entity(
    tableName = "addiction_scores",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AddictionScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val date: Long,
    val overallScore: Float,                 // 0-100 score de adicción
    val frequencyScore: Float,               // Qué tan seguido se abre
    val durationScore: Float,                // Qué tanto tiempo se usa
    val resistanceScore: Float,              // Qué tan difícil es dejar de usarla
    val escalationScore: Float,              // Si el uso va en aumento
    val timingScore: Float,                  // Si se usa en horarios problemáticos (noche, trabajo)
    val compulsionScore: Float,              // Patrón de abrir-cerrar-abrir repetidamente
    val weeklyTrend: Float = 0f,             // Cambio semanal (-100 a +100)
    val monthlyTrend: Float = 0f,            // Cambio mensual
    val suggestedAction: String = "",         // Acción sugerida
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Patrón temporal detectado.
 */
data class TemporalPattern(
    val packageName: String,
    val patternType: PatternType,
    val description: String,
    val confidence: Float,                   // 0.0 - 1.0
    val peakHours: List<Int>,                // Horas pico de uso
    val peakDays: List<Int>,                 // Días pico
    val averageSessionLength: Int,           // Minutos
    val averageDailyUsage: Int,              // Minutos
    val trend: TrendDirection,
    val triggerContexts: List<String>         // Contextos que disparan el uso
)

enum class PatternType {
    MORNING_SCROLL,        // Uso compulsivo al despertar
    LATE_NIGHT_BINGE,      // Uso nocturno excesivo
    WORK_BREAK_ESCAPE,     // Escape durante horas de trabajo
    BOREDOM_TRIGGER,       // Uso disparado por aburrimiento (sesiones cortas frecuentes)
    STRESS_RESPONSE,       // Picos de uso correlacionados con estrés
    WEEKEND_SPIKE,         // Uso significativamente mayor en fines de semana
    STEADY_MODERATE,       // Uso estable y moderado (saludable)
    ESCALATING_WEEKLY,     // Aumento semanal constante
    POST_BLOCK_REBOUND,    // Uso inmediatamente después de desbloqueo
    NOTIFICATION_DRIVEN,   // Uso disparado por notificaciones
    SOCIAL_CHAIN,          // Abrir una red social lleva a abrir otras
    PROCRASTINATION_LOOP   // Patrón de procrastinación
}

enum class TrendDirection {
    SHARPLY_DECREASING,    // Bajando rápido (>30% semanal)
    DECREASING,            // Bajando (<30% semanal)
    STABLE,                // Sin cambios significativos
    INCREASING,            // Subiendo (<30% semanal)
    SHARPLY_INCREASING     // Subiendo rápido (>30% semanal)
}

/**
 * DAO para registros de patrones de uso.
 */
@Dao
interface UsagePatternDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UsagePatternRecord)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<UsagePatternRecord>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: UsagePrediction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddictionScore(score: AddictionScore)
    
    // ===== CONSULTAS DE PATRONES =====
    
    @Query("SELECT * FROM usage_pattern_records WHERE packageName = :pkg ORDER BY date DESC, hourOfDay ASC LIMIT :limit")
    suspend fun getRecordsForPackage(pkg: String, limit: Int = 720): List<UsagePatternRecord> // 30 días * 24h
    
    @Query("SELECT * FROM usage_pattern_records WHERE packageName = :pkg AND date >= :since ORDER BY date DESC")
    suspend fun getRecordsSince(pkg: String, since: Long): List<UsagePatternRecord>
    
    @Query("SELECT * FROM usage_pattern_records WHERE packageName = :pkg AND hourOfDay = :hour ORDER BY date DESC LIMIT :limit")
    suspend fun getRecordsForHour(pkg: String, hour: Int, limit: Int = 30): List<UsagePatternRecord>
    
    @Query("SELECT * FROM usage_pattern_records WHERE packageName = :pkg AND dayOfWeek = :dow ORDER BY date DESC LIMIT :limit")
    suspend fun getRecordsForDayOfWeek(pkg: String, dow: Int, limit: Int = 30): List<UsagePatternRecord>
    
    @Query("""
        SELECT hourOfDay, AVG(usageMinutes) as avgMinutes, AVG(openCount) as avgOpens
        FROM usage_pattern_records 
        WHERE packageName = :pkg AND date >= :since
        GROUP BY hourOfDay
        ORDER BY hourOfDay
    """)
    suspend fun getHourlyAverages(pkg: String, since: Long): List<HourlyAverage>
    
    @Query("""
        SELECT dayOfWeek, SUM(usageMinutes) as totalMinutes, COUNT(DISTINCT date) as dayCount
        FROM usage_pattern_records 
        WHERE packageName = :pkg AND date >= :since
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    suspend fun getDailyAverages(pkg: String, since: Long): List<DailyAverage>
    
    @Query("""
        SELECT date, SUM(usageMinutes) as totalMinutes 
        FROM usage_pattern_records 
        WHERE packageName = :pkg AND date >= :since
        GROUP BY date 
        ORDER BY date ASC
    """)
    suspend fun getDailyTotals(pkg: String, since: Long): List<DailyTotal>
    
    @Query("SELECT DISTINCT packageName FROM usage_pattern_records WHERE date >= :since")
    suspend fun getTrackedPackages(since: Long): List<String>
    
    // ===== PREDICCIONES =====
    
    @Query("SELECT * FROM usage_predictions WHERE packageName = :pkg ORDER BY predictionDate DESC LIMIT 1")
    suspend fun getLatestPrediction(pkg: String): UsagePrediction?
    
    @Query("SELECT * FROM usage_predictions WHERE predictionDate = :date")
    suspend fun getPredictionsForDate(date: Long): List<UsagePrediction>
    
    // ===== ADDICTION SCORES =====
    
    @Query("SELECT * FROM addiction_scores WHERE packageName = :pkg ORDER BY date DESC LIMIT 1")
    suspend fun getLatestAddictionScore(pkg: String): AddictionScore?
    
    @Query("SELECT * FROM addiction_scores WHERE packageName = :pkg AND date >= :since ORDER BY date ASC")
    suspend fun getAddictionScoreHistory(pkg: String, since: Long): List<AddictionScore>
    
    @Query("SELECT * FROM addiction_scores WHERE date = :date ORDER BY overallScore DESC")
    suspend fun getAddictionScoresForDate(date: Long): List<AddictionScore>
    
    // ===== LIMPIEZA =====
    
    @Query("DELETE FROM usage_pattern_records WHERE date < :before")
    suspend fun deleteOldRecords(before: Long)
    
    @Query("DELETE FROM usage_predictions WHERE predictionDate < :before")
    suspend fun deleteOldPredictions(before: Long)
    
    @Query("DELETE FROM addiction_scores WHERE date < :before")
    suspend fun deleteOldScores(before: Long)
}

data class HourlyAverage(
    val hourOfDay: Int,
    val avgMinutes: Float,
    val avgOpens: Float
)

data class DailyAverage(
    val dayOfWeek: Int,
    val totalMinutes: Int,
    val dayCount: Int
)

data class DailyTotal(
    val date: Long,
    val totalMinutes: Int
)

// ═══════════════════════════════════════════════════════════════════════════
// MOTOR DE PATRONES - El cerebro del sistema inteligente
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class UsagePatternEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patternDao: UsagePatternDao
) {
    private val TAG = "UsagePatternEngine"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Cache de patrones detectados
    private val _detectedPatterns = MutableStateFlow<Map<String, List<TemporalPattern>>>(emptyMap())
    val detectedPatterns: StateFlow<Map<String, List<TemporalPattern>>> = _detectedPatterns.asStateFlow()
    
    // Cache de scores de adicción
    private val _addictionScores = MutableStateFlow<Map<String, AddictionScore>>(emptyMap())
    val addictionScores: StateFlow<Map<String, AddictionScore>> = _addictionScores.asStateFlow()
    
    // Predicciones actuales
    private val _predictions = MutableStateFlow<Map<String, UsagePrediction>>(emptyMap())
    val predictions: StateFlow<Map<String, UsagePrediction>> = _predictions.asStateFlow()
    
    // ==================== RECOLECCIÓN DE DATOS ====================
    
    /**
     * Registra el uso actual de una app. Llamar cada vez que se detecta
     * que una app está en primer plano.
     */
    suspend fun recordUsage(
        packageName: String,
        usageMinutes: Int,
        openCount: Int = 1,
        longestSession: Int = usageMinutes,
        wasBlocked: Boolean = false,
        contextType: String? = null
    ) {
        try {
            val now = Calendar.getInstance()
            val dayStart = getDayStart(now)
            
            val record = UsagePatternRecord(
                packageName = packageName,
                date = dayStart,
                hourOfDay = now.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = getAdjustedDayOfWeek(now),
                usageMinutes = usageMinutes,
                openCount = openCount,
                longestSessionMinutes = longestSession,
                wasBlocked = wasBlocked,
                contextType = contextType,
                batteryLevel = getBatteryLevel(),
                isWeekend = isWeekend(now)
            )
            
            patternDao.insertRecord(record)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording usage", e)
        }
    }
    
    // ==================== ANÁLISIS DE PATRONES ====================
    
    /**
     * Ejecuta el análisis completo de patrones para todas las apps rastreadas.
     * Debe llamarse periódicamente (ej: cada hora o al abrir la app).
     */
    suspend fun analyzeAllPatterns() {
        try {
            val thirtyDaysAgo = getTimestamp(daysAgo = 30)
            val packages = patternDao.getTrackedPackages(thirtyDaysAgo)
            
            val allPatterns = mutableMapOf<String, List<TemporalPattern>>()
            val allScores = mutableMapOf<String, AddictionScore>()
            val allPredictions = mutableMapOf<String, UsagePrediction>()
            
            for (pkg in packages) {
                try {
                    val patterns = detectPatterns(pkg)
                    allPatterns[pkg] = patterns
                    
                    val score = calculateAddictionScore(pkg)
                    allScores[pkg] = score
                    patternDao.insertAddictionScore(score)
                    
                    val prediction = generatePrediction(pkg)
                    allPredictions[pkg] = prediction
                    patternDao.insertPrediction(prediction)
                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing patterns for $pkg", e)
                }
            }
            
            _detectedPatterns.value = allPatterns
            _addictionScores.value = allScores
            _predictions.value = allPredictions
            
            // Limpiar datos antiguos (mantener 90 días)
            val ninetyDaysAgo = getTimestamp(daysAgo = 90)
            patternDao.deleteOldRecords(ninetyDaysAgo)
            patternDao.deleteOldPredictions(ninetyDaysAgo)
            patternDao.deleteOldScores(ninetyDaysAgo)
            
            Log.d(TAG, "Pattern analysis completed for ${packages.size} packages")
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyzeAllPatterns", e)
        }
    }
    
    /**
     * Detecta todos los patrones de uso para una app específica.
     * Usa análisis estadístico multi-dimensional.
     */
    suspend fun detectPatterns(packageName: String): List<TemporalPattern> {
        val patterns = mutableListOf<TemporalPattern>()
        val thirtyDaysAgo = getTimestamp(daysAgo = 30)
        val sevenDaysAgo = getTimestamp(daysAgo = 7)
        
        val records = patternDao.getRecordsSince(packageName, thirtyDaysAgo)
        if (records.size < 7) return patterns // Necesitamos al menos 7 registros
        
        val hourlyAvgs = patternDao.getHourlyAverages(packageName, thirtyDaysAgo)
        val dailyAvgs = patternDao.getDailyAverages(packageName, thirtyDaysAgo)
        val dailyTotals = patternDao.getDailyTotals(packageName, thirtyDaysAgo)
        val recentTotals = patternDao.getDailyTotals(packageName, sevenDaysAgo)
        
        // 1. Detectar MORNING_SCROLL
        val morningUsage = hourlyAvgs.filter { it.hourOfDay in 6..9 }
        val morningAvg = morningUsage.map { it.avgMinutes }.average().toFloat()
        val totalDayAvg = hourlyAvgs.map { it.avgMinutes }.average().toFloat()
        
        if (morningAvg > totalDayAvg * 1.5 && morningAvg > 5) {
            val peakMorningHour = morningUsage.maxByOrNull { it.avgMinutes }?.hourOfDay ?: 7
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.MORNING_SCROLL,
                description = "Uso compulsivo matutino. Promedio de ${morningAvg.toInt()}min entre 6-9 AM",
                confidence = calculateConfidence(morningAvg, totalDayAvg, records.size),
                peakHours = morningUsage.map { it.hourOfDay },
                peakDays = emptyList(),
                averageSessionLength = (morningAvg / morningUsage.maxOf { it.avgOpens.coerceAtLeast(1f) }).toInt(),
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("WAKE_UP", "BED")
            ))
        }
        
        // 2. Detectar LATE_NIGHT_BINGE
        val nightUsage = hourlyAvgs.filter { it.hourOfDay in 22..23 || it.hourOfDay in 0..3 }
        val nightAvg = nightUsage.map { it.avgMinutes }.average().toFloat()
        
        if (nightAvg > totalDayAvg * 1.3 && nightAvg > 8) {
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.LATE_NIGHT_BINGE,
                description = "Uso nocturno excesivo. Promedio de ${nightAvg.toInt()}min después de las 10 PM",
                confidence = calculateConfidence(nightAvg, totalDayAvg, records.size),
                peakHours = nightUsage.map { it.hourOfDay },
                peakDays = emptyList(),
                averageSessionLength = nightAvg.toInt(),
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("SLEEP", "NIGHT")
            ))
        }
        
        // 3. Detectar WORK_BREAK_ESCAPE
        val workHoursUsage = hourlyAvgs.filter { it.hourOfDay in 9..17 }
        val workAvg = workHoursUsage.map { it.avgMinutes }.average().toFloat()
        val workOpens = workHoursUsage.map { it.avgOpens }.average().toFloat()
        
        if (workOpens > 3 && workAvg > 5) {
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.WORK_BREAK_ESCAPE,
                description = "Escape frecuente durante horario laboral. ~${workOpens.toInt()} aperturas/hora",
                confidence = calculateConfidence(workOpens, 2f, records.size),
                peakHours = workHoursUsage.sortedByDescending { it.avgOpens }.take(3).map { it.hourOfDay },
                peakDays = (1..5).toList(),
                averageSessionLength = if (workOpens > 0) (workAvg / workOpens).toInt() else 0,
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("WORK", "BREAK")
            ))
        }
        
        // 4. Detectar BOREDOM_TRIGGER (muchas sesiones cortas)
        val avgOpenCount = records.map { it.openCount }.average().toFloat()
        val avgSessionLength = records.filter { it.openCount > 0 }
            .map { it.usageMinutes.toFloat() / it.openCount }.average().toFloat()
        
        if (avgOpenCount > 4 && avgSessionLength < 3) {
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.BOREDOM_TRIGGER,
                description = "Patrón de aburrimiento: ${avgOpenCount.toInt()} aperturas con sesiones de ~${avgSessionLength.toInt()}min",
                confidence = min(1f, avgOpenCount / 8f),
                peakHours = hourlyAvgs.sortedByDescending { it.avgOpens }.take(5).map { it.hourOfDay },
                peakDays = emptyList(),
                averageSessionLength = avgSessionLength.toInt(),
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("BOREDOM", "IDLE")
            ))
        }
        
        // 5. Detectar WEEKEND_SPIKE
        val weekdayAvg = dailyAvgs.filter { it.dayOfWeek in 1..5 }
            .map { if (it.dayCount > 0) it.totalMinutes.toFloat() / it.dayCount else 0f }
            .average().toFloat()
        val weekendAvg = dailyAvgs.filter { it.dayOfWeek in 6..7 }
            .map { if (it.dayCount > 0) it.totalMinutes.toFloat() / it.dayCount else 0f }
            .average().toFloat()
        
        if (weekendAvg > weekdayAvg * 1.5 && weekendAvg > 30) {
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.WEEKEND_SPIKE,
                description = "Uso ${((weekendAvg / weekdayAvg.coerceAtLeast(1f) - 1) * 100).toInt()}% mayor los fines de semana",
                confidence = calculateConfidence(weekendAvg, weekdayAvg, records.size),
                peakHours = emptyList(),
                peakDays = listOf(6, 7),
                averageSessionLength = avgSessionLength.toInt(),
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("WEEKEND", "FREE_TIME")
            ))
        }
        
        // 6. Detectar ESCALATING_WEEKLY
        if (dailyTotals.size >= 14) {
            val firstWeek = dailyTotals.take(7).map { it.totalMinutes }.average()
            val lastWeek = dailyTotals.takeLast(7).map { it.totalMinutes }.average()
            val changePercent = if (firstWeek > 0) ((lastWeek - firstWeek) / firstWeek * 100) else 0.0
            
            if (changePercent > 20) {
                patterns.add(TemporalPattern(
                    packageName = packageName,
                    patternType = PatternType.ESCALATING_WEEKLY,
                    description = "Uso aumentando ${changePercent.toInt()}% esta semana vs la anterior",
                    confidence = min(1f, (changePercent / 50).toFloat()),
                    peakHours = emptyList(),
                    peakDays = emptyList(),
                    averageSessionLength = avgSessionLength.toInt(),
                    averageDailyUsage = lastWeek.toInt(),
                    trend = TrendDirection.SHARPLY_INCREASING,
                    triggerContexts = listOf("ESCALATION")
                ))
            }
        }
        
        // 7. Detectar POST_BLOCK_REBOUND
        val blockedRecords = records.filter { it.wasBlocked }
        if (blockedRecords.size >= 3) {
            val reboundHours = blockedRecords.map { (it.hourOfDay + 1) % 24 }
            val reboundRecords = records.filter { it.hourOfDay in reboundHours && !it.wasBlocked }
            val reboundAvg = reboundRecords.map { it.usageMinutes }.average().toFloat()
            
            if (reboundAvg > totalDayAvg * 1.3) {
                patterns.add(TemporalPattern(
                    packageName = packageName,
                    patternType = PatternType.POST_BLOCK_REBOUND,
                    description = "Uso intensivo después de ser bloqueado. Rebote de ${reboundAvg.toInt()}min",
                    confidence = min(1f, blockedRecords.size / 10f),
                    peakHours = reboundHours.distinct(),
                    peakDays = emptyList(),
                    averageSessionLength = reboundAvg.toInt(),
                    averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                    trend = calculateTrend(dailyTotals),
                    triggerContexts = listOf("POST_BLOCK", "REBOUND")
                ))
            }
        }
        
        // 8. Detectar PROCRASTINATION_LOOP
        val compulsionHours = hourlyAvgs.filter { it.avgOpens > 5 && it.avgMinutes < it.avgOpens * 2 }
        if (compulsionHours.size >= 3) {
            patterns.add(TemporalPattern(
                packageName = packageName,
                patternType = PatternType.PROCRASTINATION_LOOP,
                description = "Loop de procrastinación detectado: abrir-cerrar-abrir repetidamente",
                confidence = min(1f, compulsionHours.size / 6f),
                peakHours = compulsionHours.map { it.hourOfDay },
                peakDays = emptyList(),
                averageSessionLength = 1,
                averageDailyUsage = dailyTotals.map { it.totalMinutes }.average().toInt(),
                trend = calculateTrend(dailyTotals),
                triggerContexts = listOf("PROCRASTINATION", "COMPULSION")
            ))
        }
        
        return patterns
    }
    
    // ==================== CÁLCULO DE ADDICTION SCORE ====================
    
    /**
     * Calcula un score de adicción multidimensional para una app.
     * Combina frecuencia, duración, resistencia, escalación y timing.
     */
    suspend fun calculateAddictionScore(packageName: String): AddictionScore {
        val thirtyDaysAgo = getTimestamp(daysAgo = 30)
        val sevenDaysAgo = getTimestamp(daysAgo = 7)
        val fourteenDaysAgo = getTimestamp(daysAgo = 14)
        
        val records = patternDao.getRecordsSince(packageName, thirtyDaysAgo)
        val recentRecords = records.filter { it.date >= sevenDaysAgo }
        val dailyTotals = patternDao.getDailyTotals(packageName, thirtyDaysAgo)
        val hourlyAvgs = patternDao.getHourlyAverages(packageName, thirtyDaysAgo)
        
        // 1. FREQUENCY SCORE - Qué tan seguido se abre (0-100)
        val avgDailyOpens = recentRecords.groupBy { it.date }
            .values.map { dayRecords -> dayRecords.sumOf { it.openCount } }
            .average().toFloat()
        val frequencyScore = min(100f, avgDailyOpens * 3f) // 33 aperturas/día = 100
        
        // 2. DURATION SCORE - Qué tanto tiempo total se usa (0-100)
        val avgDailyMinutes = dailyTotals.takeLast(7)
            .map { it.totalMinutes }.average().toFloat()
        val durationScore = min(100f, avgDailyMinutes / 2.4f) // 240 min/día = 100
        
        // 3. RESISTANCE SCORE - Dificultad para dejar de usarla (0-100)
        val blockedCount = recentRecords.count { it.wasBlocked }.toFloat()
        val shameCount = recentRecords.count { it.wasUnlockedByShame }.toFloat()
        val avgSessionLength = recentRecords.filter { it.longestSessionMinutes > 0 }
            .map { it.longestSessionMinutes }.average().toFloat()
        val resistanceScore = min(100f, 
            (blockedCount * 5f) + (shameCount * 15f) + (avgSessionLength / 3f))
        
        // 4. ESCALATION SCORE - Si el uso va en aumento (0-100)
        val escalationScore = if (dailyTotals.size >= 14) {
            val firstHalf = dailyTotals.take(dailyTotals.size / 2).map { it.totalMinutes }.average()
            val secondHalf = dailyTotals.takeLast(dailyTotals.size / 2).map { it.totalMinutes }.average()
            val change = if (firstHalf > 0) ((secondHalf - firstHalf) / firstHalf * 100).toFloat() else 0f
            min(100f, max(0f, change * 2f)) // 50% aumento = 100
        } else 0f
        
        // 5. TIMING SCORE - Uso en horarios problemáticos (0-100)
        val nightUsage = hourlyAvgs.filter { it.hourOfDay in 23..23 || it.hourOfDay in 0..5 }
            .map { it.avgMinutes }.sum()
        val workUsage = hourlyAvgs.filter { it.hourOfDay in 9..17 }
            .map { it.avgMinutes }.sum()
        val totalUsage = hourlyAvgs.map { it.avgMinutes }.sum()
        val nightRatio = if (totalUsage > 0) nightUsage / totalUsage else 0f
        val timingScore = min(100f, nightRatio * 200f + (if (workUsage > 30) 20f else 0f))
        
        // 6. COMPULSION SCORE - Patrón compulsivo de abrir-cerrar (0-100)
        val highOpenLowDuration = recentRecords.count { it.openCount > 3 && it.usageMinutes < 5 }
        val compulsionScore = min(100f, highOpenLowDuration * 10f)
        
        // OVERALL SCORE - Promedio ponderado
        val overallScore = (
            frequencyScore * 0.15f +
            durationScore * 0.25f +
            resistanceScore * 0.20f +
            escalationScore * 0.20f +
            timingScore * 0.10f +
            compulsionScore * 0.10f
        )
        
        // Calcular tendencias
        val weeklyTrend = if (dailyTotals.size >= 14) {
            val prevWeek = dailyTotals.dropLast(7).takeLast(7).map { it.totalMinutes }.average()
            val thisWeek = dailyTotals.takeLast(7).map { it.totalMinutes }.average()
            if (prevWeek > 0) ((thisWeek - prevWeek) / prevWeek * 100).toFloat() else 0f
        } else 0f
        
        val monthlyTrend = if (dailyTotals.size >= 28) {
            val prevMonth = dailyTotals.take(14).map { it.totalMinutes }.average()
            val thisMonth = dailyTotals.takeLast(14).map { it.totalMinutes }.average()
            if (prevMonth > 0) ((thisMonth - prevMonth) / prevMonth * 100).toFloat() else 0f
        } else 0f
        
        // Acción sugerida basada en el score
        val suggestedAction = when {
            overallScore >= 80 -> "⚠️ CRÍTICO: Considerar Modo Nuclear o eliminación temporal de la app"
            overallScore >= 60 -> "🔴 ALTO: Reducir límite 30%, activar bloqueo nocturno y Ayuno Digital"
            overallScore >= 40 -> "🟡 MODERADO: Activar timer flotante, establecer límite de ${max(15, avgDailyMinutes.toInt() - 15)}min"
            overallScore >= 20 -> "🟢 BAJO: Mantener límite actual, considerar bloqueo en horas de trabajo"
            else -> "✅ SALUDABLE: Buen control del uso. Mantener hábitos actuales"
        }
        
        return AddictionScore(
            packageName = packageName,
            date = getDayStart(Calendar.getInstance()),
            overallScore = overallScore,
            frequencyScore = frequencyScore,
            durationScore = durationScore,
            resistanceScore = resistanceScore,
            escalationScore = escalationScore,
            timingScore = timingScore,
            compulsionScore = compulsionScore,
            weeklyTrend = weeklyTrend,
            monthlyTrend = monthlyTrend,
            suggestedAction = suggestedAction
        )
    }
    
    // ==================== PREDICCIONES ====================
    
    /**
     * Genera una predicción de uso para mañana basada en patrones históricos.
     * Usa media ponderada exponencial con ajuste por día de semana.
     */
    suspend fun generatePrediction(packageName: String): UsagePrediction {
        val thirtyDaysAgo = getTimestamp(daysAgo = 30)
        val hourlyAvgs = patternDao.getHourlyAverages(packageName, thirtyDaysAgo)
        val dailyTotals = patternDao.getDailyTotals(packageName, thirtyDaysAgo)
        val dailyAvgs = patternDao.getDailyAverages(packageName, thirtyDaysAgo)
        
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDow = getAdjustedDayOfWeek(tomorrow)
        val isWeekendDay = tomorrowDow in 6..7
        
        // Media Ponderada Exponencial (EWA) para predicción diaria
        var predictedTotal = 0f
        if (dailyTotals.isNotEmpty()) {
            val alpha = 0.3f // Factor de suavizado
            var ewma = dailyTotals.first().totalMinutes.toFloat()
            for (dt in dailyTotals.drop(1)) {
                ewma = alpha * dt.totalMinutes + (1 - alpha) * ewma
            }
            predictedTotal = ewma
        }
        
        // Ajuste por día de la semana
        val dowData = dailyAvgs.find { it.dayOfWeek == tomorrowDow }
        val dowAvg = if (dowData != null && dowData.dayCount > 0) {
            dowData.totalMinutes.toFloat() / dowData.dayCount
        } else predictedTotal
        predictedTotal = (predictedTotal * 0.6f + dowAvg * 0.4f)
        
        // Predicción por hora
        val peakHour = hourlyAvgs.maxByOrNull { it.avgMinutes }?.hourOfDay ?: 12
        val peakMinutes = hourlyAvgs.maxByOrNull { it.avgMinutes }?.avgMinutes?.toInt() ?: 0
        
        // Calcular nivel de riesgo
        val dailyAvg = dailyTotals.takeLast(7).map { it.totalMinutes }.average().toFloat()
        val trend = calculateTrend(dailyTotals)
        val riskLevel = when {
            predictedTotal > 240 || trend == TrendDirection.SHARPLY_INCREASING -> RiskLevel.CRITICAL
            predictedTotal > 120 || (predictedTotal > 90 && trend == TrendDirection.INCREASING) -> RiskLevel.HIGH
            trend == TrendDirection.INCREASING && predictedTotal > 60 -> RiskLevel.ESCALATING
            predictedTotal > 60 -> RiskLevel.MODERATE
            predictedTotal > 30 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
        
        // Sugerir límite inteligente
        val suggestedLimit = when (riskLevel) {
            RiskLevel.CRITICAL -> max(15, (predictedTotal * 0.4f).toInt())
            RiskLevel.HIGH -> max(20, (predictedTotal * 0.5f).toInt())
            RiskLevel.ESCALATING -> max(25, (predictedTotal * 0.6f).toInt())
            RiskLevel.MODERATE -> max(30, (predictedTotal * 0.7f).toInt())
            RiskLevel.LOW -> max(30, (predictedTotal * 0.85f).toInt())
            RiskLevel.MINIMAL -> null // No necesita límite
        }
        
        // Confidence basada en cantidad de datos
        val confidence = min(1f, dailyTotals.size / 21f) // 3 semanas = máxima confianza
        
        // Descripción del patrón
        val pattern = buildString {
            append("Uso predicho: ${predictedTotal.toInt()}min. ")
            append("Hora pico: ${peakHour}:00 (${peakMinutes}min). ")
            if (isWeekendDay) append("Fin de semana: uso potencialmente mayor. ")
            when (trend) {
                TrendDirection.SHARPLY_INCREASING -> append("⚠️ Tendencia: aumento fuerte.")
                TrendDirection.INCREASING -> append("📈 Tendencia: en aumento.")
                TrendDirection.STABLE -> append("➡️ Tendencia: estable.")
                TrendDirection.DECREASING -> append("📉 Tendencia: mejorando.")
                TrendDirection.SHARPLY_DECREASING -> append("✅ Tendencia: mejora notable.")
            }
        }
        
        // Hourly breakdown (JSON simplificado)
        val hourlyBreakdown = hourlyAvgs.joinToString(",") { 
            "${it.hourOfDay}:${it.avgMinutes.toInt()}" 
        }
        
        return UsagePrediction(
            packageName = packageName,
            predictionDate = getDayStart(tomorrow),
            predictedTotalMinutes = predictedTotal.toInt(),
            predictedPeakHour = peakHour,
            predictedPeakMinutes = peakMinutes,
            confidenceScore = confidence,
            riskLevel = riskLevel,
            suggestedLimit = suggestedLimit,
            pattern = pattern,
            hourlyBreakdown = hourlyBreakdown
        )
    }
    
    // ==================== LÍMITE INTELIGENTE ====================
    
    /**
     * Calcula un límite diario inteligente basado en patrones, tendencias y objetivos.
     * Este límite se ajusta dinámicamente cada día.
     */
    suspend fun calculateSmartLimit(
        packageName: String, 
        currentLimit: Int,
        targetReductionPercent: Float = 10f  // Objetivo: reducir 10% semanalmente
    ): Int {
        val prediction = patternDao.getLatestPrediction(packageName) ?: return currentLimit
        val addictionScore = patternDao.getLatestAddictionScore(packageName) ?: return currentLimit
        
        var smartLimit = currentLimit
        
        // 1. Si el score de adicción es alto, reducir agresivamente
        when {
            addictionScore.overallScore >= 80 -> smartLimit = (currentLimit * 0.6f).toInt()
            addictionScore.overallScore >= 60 -> smartLimit = (currentLimit * 0.7f).toInt()
            addictionScore.overallScore >= 40 -> smartLimit = (currentLimit * 0.85f).toInt()
        }
        
        // 2. Si hay tendencia de escalación, compensar
        if (addictionScore.escalationScore > 50) {
            smartLimit = (smartLimit * (1 - addictionScore.escalationScore / 200f)).toInt()
        }
        
        // 3. Reducción gradual semanal hacia objetivo
        val weeklyReduction = (currentLimit * targetReductionPercent / 100f).toInt()
        val targetLimit = currentLimit - weeklyReduction / 7 // Reducción diaria
        smartLimit = min(smartLimit, targetLimit)
        
        // 4. No permitir límites demasiado bajos
        smartLimit = max(5, smartLimit)
        
        // 5. No aumentar el límite original
        smartLimit = min(smartLimit, currentLimit)
        
        return smartLimit
    }
    
    /**
     * Obtiene la hora de mayor riesgo de uso excesivo para una app.
     * Útil para programar bloqueos preventivos.
     */
    suspend fun getPeakRiskHours(packageName: String): List<Pair<Int, Float>> {
        val thirtyDaysAgo = getTimestamp(daysAgo = 30)
        val hourlyAvgs = patternDao.getHourlyAverages(packageName, thirtyDaysAgo)
        
        if (hourlyAvgs.isEmpty()) return emptyList()
        
        val maxMinutes = hourlyAvgs.maxOf { it.avgMinutes }
        val maxOpens = hourlyAvgs.maxOf { it.avgOpens }
        
        return hourlyAvgs.map { avg ->
            val minuteRisk = if (maxMinutes > 0) avg.avgMinutes / maxMinutes else 0f
            val openRisk = if (maxOpens > 0) avg.avgOpens / maxOpens else 0f
            val combinedRisk = minuteRisk * 0.6f + openRisk * 0.4f
            avg.hourOfDay to combinedRisk
        }.sortedByDescending { it.second }
    }
    
    /**
     * Predice si el usuario va a exceder su límite hoy basándose en el uso actual.
     */
    suspend fun predictWillExceedToday(
        packageName: String, 
        currentUsageMinutes: Int,
        currentLimit: Int
    ): Pair<Boolean, Float> {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val remainingHours = 24 - currentHour
        
        val thirtyDaysAgo = getTimestamp(daysAgo = 30)
        val hourlyAvgs = patternDao.getHourlyAverages(packageName, thirtyDaysAgo)
        
        // Calcular uso esperado en las horas restantes
        val remainingUsage = hourlyAvgs.filter { it.hourOfDay > currentHour }
            .map { it.avgMinutes }.sum()
        
        val predictedTotal = currentUsageMinutes + remainingUsage
        val probability = if (currentLimit > 0) {
            min(1f, predictedTotal / currentLimit)
        } else 0f
        
        return Pair(predictedTotal > currentLimit, probability)
    }
    
    // ==================== UTILIDADES ====================
    
    private fun calculateConfidence(value: Float, baseline: Float, sampleSize: Int): Float {
        val ratio = if (baseline > 0) value / baseline else 1f
        val sampleConfidence = min(1f, sampleSize / 50f)
        return min(1f, (ratio - 1f) * sampleConfidence)
    }
    
    private fun calculateTrend(dailyTotals: List<DailyTotal>): TrendDirection {
        if (dailyTotals.size < 7) return TrendDirection.STABLE
        
        val recentWeek = dailyTotals.takeLast(7).map { it.totalMinutes }.average()
        val previousWeek = if (dailyTotals.size >= 14) {
            dailyTotals.dropLast(7).takeLast(7).map { it.totalMinutes }.average()
        } else {
            recentWeek
        }
        
        val changePercent = if (previousWeek > 0) {
            ((recentWeek - previousWeek) / previousWeek * 100)
        } else 0.0
        
        return when {
            changePercent > 30 -> TrendDirection.SHARPLY_INCREASING
            changePercent > 10 -> TrendDirection.INCREASING
            changePercent < -30 -> TrendDirection.SHARPLY_DECREASING
            changePercent < -10 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun getDayStart(cal: Calendar): Long {
        return Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun getAdjustedDayOfWeek(cal: Calendar): Int {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }
    
    private fun isWeekend(cal: Calendar): Boolean {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }
    
    private fun getTimestamp(daysAgo: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) { -1 }
    }
}

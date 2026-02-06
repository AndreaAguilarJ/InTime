package com.momentummm.app.data.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════════════════
//  █████╗ ███╗   ██╗ █████╗ ██╗  ██╗   ██╗████████╗██╗ ██████╗███████╗
// ██╔══██╗████╗  ██║██╔══██╗██║  ╚██╗ ██╔╝╚══██╔══╝██║██╔════╝██╔════╝
// ███████║██╔██╗ ██║███████║██║   ╚████╔╝    ██║   ██║██║     ███████╗
// ██╔══██║██║╚██╗██║██╔══██║██║    ╚██╔╝     ██║   ██║██║     ╚════██║
// ██║  ██║██║ ╚████║██║  ██║███████╗██║      ██║   ██║╚██████╗███████║
// ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝╚═╝      ╚═╝   ╚═╝ ╚═════╝╚══════╝
// USAGE ANALYTICS ENGINE - Insights profundos sobre el uso
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Motor de analíticas avanzado que genera insights profundos,
 * reportes semanales, y métricas de bienestar digital.
 */
@Singleton
class UsageAnalyticsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patternEngine: UsagePatternEngine,
    private val patternDao: UsagePatternDao,
    private val blockingEventDao: BlockingEventDao
) {
    private val TAG = "UsageAnalytics"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══ ESTADOS OBSERVABLES ═══
    
    private val _weeklyReport = MutableStateFlow<WeeklyReport?>(null)
    val weeklyReport: StateFlow<WeeklyReport?> = _weeklyReport.asStateFlow()
    
    private val _digitalWellbeingScore = MutableStateFlow(0f)
    val digitalWellbeingScore: StateFlow<Float> = _digitalWellbeingScore.asStateFlow()
    
    private val _insights = MutableStateFlow<List<UsageInsight>>(emptyList())
    val insights: StateFlow<List<UsageInsight>> = _insights.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════
    // REPORTE SEMANAL
    // ═══════════════════════════════════════════════════════════════
    
    data class WeeklyReport(
        val weekStart: Long,
        val weekEnd: Long,
        val totalScreenTimeMinutes: Int,
        val previousWeekMinutes: Int,
        val changePercent: Float,                // % de cambio vs semana anterior
        val dailyAverage: Int,
        val mostUsedApp: AppUsageSummary?,
        val mostImprovedApp: AppUsageSummary?,
        val worstTrendApp: AppUsageSummary?,
        val appSummaries: List<AppUsageSummary>,
        val totalBlocks: Int,
        val totalOverrides: Int,
        val resistanceRate: Float,
        val streakDays: Int,
        val bestDay: DayDetail?,
        val worstDay: DayDetail?,
        val peakUsageHour: Int,
        val lateNightMinutes: Int,
        val morningMinutes: Int,
        val focusProfileCompletions: Int,
        val digitalWellbeingScore: Float,        // 0-100
        val achievements: List<Achievement>,
        val recommendations: List<String>,
        val motivationalMessage: String,
        val generatedAt: Long = System.currentTimeMillis()
    )
    
    data class AppUsageSummary(
        val packageName: String,
        val appName: String,
        val totalMinutes: Int,
        val dailyAverage: Int,
        val previousWeekMinutes: Int,
        val changePercent: Float,
        val addictionScore: Float,
        val trend: TrendDirection,
        val topPattern: PatternType?,
        val timesBlocked: Int,
        val timesOverridden: Int
    )
    
    data class DayDetail(
        val date: Long,
        val dayOfWeek: Int,
        val totalMinutes: Int,
        val appBreakdown: Map<String, Int>
    )
    
    data class Achievement(
        val type: AchievementType,
        val title: String,
        val description: String,
        val icon: String,
        val unlockedAt: Long = System.currentTimeMillis()
    )
    
    enum class AchievementType {
        FIRST_DAY_UNDER_LIMIT,       // Primer día bajo el límite
        THREE_DAY_STREAK,            // 3 días consecutivos
        SEVEN_DAY_STREAK,            // 7 días consecutivos
        THIRTY_DAY_STREAK,           // 30 días consecutivos
        REDUCED_10_PERCENT,          // Redujo uso 10%
        REDUCED_25_PERCENT,          // Redujo uso 25%
        REDUCED_50_PERCENT,          // Redujo uso 50%
        NO_OVERRIDE_WEEK,            // Semana sin desbloqueos forzados
        MORNING_FREE,                // Semana sin uso matutino
        NIGHT_FREE,                  // Semana sin uso nocturno
        FOCUS_MASTER,                // Completó 10 sesiones de enfoque
        DIGITAL_MINIMALIST,          // Menos de 1h de pantalla por día
        SELF_AWARE,                  // Reportó un falso positivo
        APP_DELETED                  // Eliminó una app después del bloqueo
    }
    
    data class UsageInsight(
        val type: InsightType,
        val title: String,
        val description: String,
        val icon: String,
        val severity: InsightSeverity,
        val actionable: Boolean = true,
        val suggestedAction: String = "",
        val relatedApp: String? = null,
        val dataPoints: Map<String, Any> = emptyMap()
    )
    
    enum class InsightType {
        USAGE_TREND,         // Tendencia de uso
        PATTERN_DETECTED,    // Patrón detectado
        PEAK_TIME,           // Hora pico
        IMPROVEMENT,         // Mejora detectada
        WARNING,             // Advertencia
        COMPARISON,          // Comparación temporal
        PREDICTION,          // Predicción
        RECOMMENDATION       // Recomendación
    }
    
    enum class InsightSeverity {
        POSITIVE,    // Buen dato, celebrar
        NEUTRAL,     // Informativo
        WARNING,     // Precaución
        CRITICAL     // Requiere acción
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GENERACIÓN DE REPORTES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Genera el reporte semanal completo.
     */
    suspend fun generateWeeklyReport(): WeeklyReport {
        val now = Calendar.getInstance()
        val weekStart = getWeekStart(now)
        val weekEnd = now.timeInMillis
        val prevWeekStart = weekStart - 7 * 24 * 60 * 60 * 1000L
        
        val packages = patternDao.getTrackedPackages(prevWeekStart)
        val appSummaries = mutableListOf<AppUsageSummary>()
        
        var totalMinutes = 0
        var prevTotalMinutes = 0
        
        for (pkg in packages) {
            val thisWeek = patternDao.getDailyTotals(pkg, weekStart)
            val prevWeek = patternDao.getDailyTotals(pkg, prevWeekStart)
                .filter { it.date < weekStart }
            
            val thisTotal = thisWeek.sumOf { it.totalMinutes }
            val prevTotal = prevWeek.sumOf { it.totalMinutes }
            
            totalMinutes += thisTotal
            prevTotalMinutes += prevTotal
            
            val change = if (prevTotal > 0) ((thisTotal - prevTotal).toFloat() / prevTotal * 100) else 0f
            val score = patternEngine.addictionScores.value[pkg]
            val patterns = patternEngine.detectedPatterns.value[pkg]
            
            val blocks = blockingEventDao.getEventsForPackage(pkg, weekStart)
            val blockedCount = blocks.count { it.eventType in listOf(BlockingEventType.APP_BLOCKED, BlockingEventType.FEATURE_BLOCKED) }
            val overriddenCount = blocks.count { it.eventType == BlockingEventType.BLOCK_OVERRIDDEN }
            
            appSummaries.add(AppUsageSummary(
                packageName = pkg,
                appName = getAppName(pkg),
                totalMinutes = thisTotal,
                dailyAverage = if (thisWeek.isNotEmpty()) thisTotal / thisWeek.size else 0,
                previousWeekMinutes = prevTotal,
                changePercent = change,
                addictionScore = score?.overallScore ?: 0f,
                trend = when {
                    change > 20 -> TrendDirection.SHARPLY_INCREASING
                    change > 5 -> TrendDirection.INCREASING
                    change < -20 -> TrendDirection.SHARPLY_DECREASING
                    change < -5 -> TrendDirection.DECREASING
                    else -> TrendDirection.STABLE
                },
                topPattern = patterns?.maxByOrNull { it.confidence }?.patternType,
                timesBlocked = blockedCount,
                timesOverridden = overriddenCount
            ))
        }
        
        val sortedByUsage = appSummaries.sortedByDescending { it.totalMinutes }
        val sortedByImprovement = appSummaries.sortedBy { it.changePercent }
        val sortedByWorst = appSummaries.sortedByDescending { it.changePercent }
        
        // Calcular stats por día
        val allDailyTotals = mutableMapOf<Long, MutableMap<String, Int>>()
        for (pkg in packages) {
            val dailyTotals = patternDao.getDailyTotals(pkg, weekStart)
            for (dt in dailyTotals) {
                allDailyTotals.getOrPut(dt.date) { mutableMapOf() }[pkg] = dt.totalMinutes
            }
        }
        
        val dayDetails = allDailyTotals.map { (date, breakdown) ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            DayDetail(
                date = date,
                dayOfWeek = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 
                    else cal.get(Calendar.DAY_OF_WEEK) - 1,
                totalMinutes = breakdown.values.sum(),
                appBreakdown = breakdown
            )
        }
        
        val bestDay = dayDetails.minByOrNull { it.totalMinutes }
        val worstDay = dayDetails.maxByOrNull { it.totalMinutes }
        
        // Hora pico
        val allHourlyAvgs = mutableMapOf<Int, Float>()
        for (pkg in packages) {
            val hourly = patternDao.getHourlyAverages(pkg, weekStart)
            for (h in hourly) {
                allHourlyAvgs[h.hourOfDay] = (allHourlyAvgs[h.hourOfDay] ?: 0f) + h.avgMinutes
            }
        }
        val peakHour = allHourlyAvgs.maxByOrNull { it.value }?.key ?: 12
        
        // Late night y morning
        val lateNightMinutes = allHourlyAvgs.filter { it.key in 22..23 || it.key in 0..5 }
            .values.sum().toInt()
        val morningMinutes = allHourlyAvgs.filter { it.key in 6..9 }.values.sum().toInt()
        
        // Events
        val allEvents = blockingEventDao.getRecentEvents(weekStart, 1000)
        val totalBlocks = allEvents.count { it.eventType in listOf(BlockingEventType.APP_BLOCKED, BlockingEventType.FEATURE_BLOCKED, BlockingEventType.WEBSITE_BLOCKED) }
        val totalOverrides = allEvents.count { it.eventType == BlockingEventType.BLOCK_OVERRIDDEN }
        val focusCompletions = allEvents.count { it.eventType == BlockingEventType.FOCUS_MODE_ENDED }
        
        // Digital Wellbeing Score (0-100)
        val wellbeingScore = calculateDigitalWellbeingScore(
            totalMinutes, prevTotalMinutes, totalBlocks, totalOverrides, 
            lateNightMinutes, appSummaries
        )
        
        // Achievements
        val achievements = checkAchievements(totalMinutes, prevTotalMinutes, totalBlocks, totalOverrides, dayDetails)
        
        // Recommendations
        val recommendations = generateRecommendations(appSummaries, wellbeingScore, lateNightMinutes, morningMinutes)
        
        // Motivational message
        val changePercent = if (prevTotalMinutes > 0) ((totalMinutes - prevTotalMinutes).toFloat() / prevTotalMinutes * 100) else 0f
        val motivationalMessage = generateMotivationalMessage(changePercent, wellbeingScore, totalBlocks)
        
        val report = WeeklyReport(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalScreenTimeMinutes = totalMinutes,
            previousWeekMinutes = prevTotalMinutes,
            changePercent = changePercent,
            dailyAverage = if (dayDetails.isNotEmpty()) totalMinutes / dayDetails.size else 0,
            mostUsedApp = sortedByUsage.firstOrNull(),
            mostImprovedApp = sortedByImprovement.firstOrNull { it.changePercent < 0 },
            worstTrendApp = sortedByWorst.firstOrNull { it.changePercent > 10 },
            appSummaries = sortedByUsage,
            totalBlocks = totalBlocks,
            totalOverrides = totalOverrides,
            resistanceRate = if (totalBlocks > 0) 1f - (totalOverrides.toFloat() / totalBlocks) else 1f,
            streakDays = calculateStreak(dayDetails),
            bestDay = bestDay,
            worstDay = worstDay,
            peakUsageHour = peakHour,
            lateNightMinutes = lateNightMinutes,
            morningMinutes = morningMinutes,
            focusProfileCompletions = focusCompletions,
            digitalWellbeingScore = wellbeingScore,
            achievements = achievements,
            recommendations = recommendations,
            motivationalMessage = motivationalMessage
        )
        
        _weeklyReport.value = report
        _digitalWellbeingScore.value = wellbeingScore
        
        return report
    }
    
    // ═══════════════════════════════════════════════════════════════
    // INSIGHTS EN TIEMPO REAL
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Genera insights en tiempo real basados en el uso actual.
     */
    suspend fun generateInsights(): List<UsageInsight> {
        val insights = mutableListOf<UsageInsight>()
        val sevenDaysAgo = getTimestamp(daysAgo = 7)
        val fourteenDaysAgo = getTimestamp(daysAgo = 14)
        
        val packages = patternDao.getTrackedPackages(sevenDaysAgo)
        
        for (pkg in packages) {
            val patterns = patternEngine.detectedPatterns.value[pkg] ?: continue
            val score = patternEngine.addictionScores.value[pkg]
            val prediction = patternEngine.predictions.value[pkg]
            
            // Insight: Patrón de uso detectado
            for (pattern in patterns.filter { it.confidence > 0.6f }) {
                insights.add(UsageInsight(
                    type = InsightType.PATTERN_DETECTED,
                    title = getPatternTitle(pattern.patternType),
                    description = pattern.description,
                    icon = getPatternIcon(pattern.patternType),
                    severity = if (pattern.confidence > 0.8f) InsightSeverity.WARNING else InsightSeverity.NEUTRAL,
                    suggestedAction = getPatternAction(pattern.patternType),
                    relatedApp = pkg
                ))
            }
            
            // Insight: Score de adicción alto
            if (score != null && score.overallScore > 60) {
                insights.add(UsageInsight(
                    type = InsightType.WARNING,
                    title = "Score de adicción alto: ${getAppName(pkg)}",
                    description = "Tu score de adicción para ${getAppName(pkg)} es ${score.overallScore.toInt()}/100. ${score.suggestedAction}",
                    icon = "⚠️",
                    severity = if (score.overallScore > 80) InsightSeverity.CRITICAL else InsightSeverity.WARNING,
                    suggestedAction = score.suggestedAction,
                    relatedApp = pkg,
                    dataPoints = mapOf(
                        "score" to score.overallScore,
                        "frequency" to score.frequencyScore,
                        "duration" to score.durationScore,
                        "escalation" to score.escalationScore
                    )
                ))
            }
            
            // Insight: Mejora detectada
            if (score != null && score.weeklyTrend < -15) {
                insights.add(UsageInsight(
                    type = InsightType.IMPROVEMENT,
                    title = "¡Mejora en ${getAppName(pkg)}! 📉",
                    description = "Redujiste tu uso un ${abs(score.weeklyTrend).toInt()}% esta semana. ¡Sigue así!",
                    icon = "🎉",
                    severity = InsightSeverity.POSITIVE,
                    relatedApp = pkg
                ))
            }
            
            // Insight: Tendencia preocupante
            if (score != null && score.weeklyTrend > 20) {
                insights.add(UsageInsight(
                    type = InsightType.WARNING,
                    title = "Uso en aumento: ${getAppName(pkg)}",
                    description = "Tu uso aumentó ${score.weeklyTrend.toInt()}% esta semana. Considera reducir tu límite.",
                    icon = "📈",
                    severity = InsightSeverity.WARNING,
                    suggestedAction = "Reducir el límite diario un 20%",
                    relatedApp = pkg
                ))
            }
            
            // Insight: Predicción
            if (prediction != null && prediction.riskLevel in listOf(RiskLevel.HIGH, RiskLevel.CRITICAL)) {
                insights.add(UsageInsight(
                    type = InsightType.PREDICTION,
                    title = "Predicción: ${getAppName(pkg)}",
                    description = "Se predice un uso de ${prediction.predictedTotalMinutes} min mañana. ${if (prediction.suggestedLimit != null) "Límite sugerido: ${prediction.suggestedLimit}min" else ""}",
                    icon = "🔮",
                    severity = InsightSeverity.WARNING,
                    suggestedAction = prediction.suggestedLimit?.let { "Establecer límite de ${it}min" } ?: "",
                    relatedApp = pkg,
                    dataPoints = mapOf(
                        "predicted" to prediction.predictedTotalMinutes,
                        "peakHour" to prediction.predictedPeakHour,
                        "risk" to prediction.riskLevel.name
                    )
                ))
            }
        }
        
        // Ordenar: críticos primero, positivos al final
        val sorted = insights.sortedWith(compareBy { 
            when (it.severity) {
                InsightSeverity.CRITICAL -> 0
                InsightSeverity.WARNING -> 1
                InsightSeverity.NEUTRAL -> 2
                InsightSeverity.POSITIVE -> 3
            }
        })
        
        _insights.value = sorted
        return sorted
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CÁLCULOS INTERNOS
    // ═══════════════════════════════════════════════════════════════
    
    private fun calculateDigitalWellbeingScore(
        totalMinutes: Int,
        prevMinutes: Int,
        totalBlocks: Int,
        totalOverrides: Int,
        lateNightMinutes: Int,
        apps: List<AppUsageSummary>
    ): Float {
        var score = 100f
        
        // Penalizar por tiempo total excesivo
        val dailyAvg = totalMinutes / 7f
        when {
            dailyAvg > 240 -> score -= 40f  // > 4h/día
            dailyAvg > 180 -> score -= 30f  // > 3h/día
            dailyAvg > 120 -> score -= 20f  // > 2h/día
            dailyAvg > 60 -> score -= 10f   // > 1h/día
        }
        
        // Bonus por mejora vs semana anterior
        if (prevMinutes > 0) {
            val change = (totalMinutes - prevMinutes).toFloat() / prevMinutes
            when {
                change < -0.2f -> score += 15f  // Mejoró >20%
                change < -0.1f -> score += 10f  // Mejoró >10%
                change > 0.2f -> score -= 15f   // Empeoró >20%
                change > 0.1f -> score -= 10f   // Empeoró >10%
            }
        }
        
        // Penalizar por overrides
        val overrideRate = if (totalBlocks > 0) totalOverrides.toFloat() / totalBlocks else 0f
        score -= overrideRate * 20f
        
        // Penalizar por uso nocturno
        when {
            lateNightMinutes > 60 -> score -= 15f
            lateNightMinutes > 30 -> score -= 10f
            lateNightMinutes > 10 -> score -= 5f
        }
        
        // Penalizar por apps con score de adicción alto
        val highAddictionApps = apps.count { it.addictionScore > 60 }
        score -= highAddictionApps * 5f
        
        // Bonus por tener bloqueos activos (muestra intención)
        if (totalBlocks > 0) score += 5f
        
        return score.coerceIn(0f, 100f)
    }
    
    private fun calculateStreak(dayDetails: List<DayDetail>): Int {
        // Simplificado: contar días consecutivos bajo el promedio
        if (dayDetails.isEmpty()) return 0
        val avg = dayDetails.map { it.totalMinutes }.average()
        var streak = 0
        for (day in dayDetails.sortedByDescending { it.date }) {
            if (day.totalMinutes <= avg) streak++
            else break
        }
        return streak
    }
    
    private fun checkAchievements(
        totalMinutes: Int,
        prevMinutes: Int,
        totalBlocks: Int,
        totalOverrides: Int,
        dayDetails: List<DayDetail>
    ): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        
        val changePercent = if (prevMinutes > 0) ((totalMinutes - prevMinutes).toFloat() / prevMinutes * 100) else 0f
        
        if (changePercent <= -10) {
            achievements.add(Achievement(
                type = AchievementType.REDUCED_10_PERCENT,
                title = "Reductor Digital",
                description = "Redujiste tu tiempo de pantalla un ${abs(changePercent).toInt()}%",
                icon = "📉"
            ))
        }
        
        if (changePercent <= -25) {
            achievements.add(Achievement(
                type = AchievementType.REDUCED_25_PERCENT,
                title = "Transformación Digital",
                description = "¡Increíble! ${abs(changePercent).toInt()}% menos de pantalla",
                icon = "🌟"
            ))
        }
        
        if (totalOverrides == 0 && totalBlocks > 0) {
            achievements.add(Achievement(
                type = AchievementType.NO_OVERRIDE_WEEK,
                title = "Voluntad de Acero",
                description = "Toda la semana sin desbloquear apps. ¡Respetaste todos los límites!",
                icon = "🛡️"
            ))
        }
        
        val dailyAvg = if (dayDetails.isNotEmpty()) totalMinutes / dayDetails.size else 0
        if (dailyAvg < 60) {
            achievements.add(Achievement(
                type = AchievementType.DIGITAL_MINIMALIST,
                title = "Minimalista Digital",
                description = "Promedio de menos de 1 hora al día. ¡Impresionante!",
                icon = "🧘"
            ))
        }
        
        val streak = calculateStreak(dayDetails)
        when {
            streak >= 30 -> achievements.add(Achievement(AchievementType.THIRTY_DAY_STREAK, "Maestro del Autocontrol", "¡30 días consecutivos bajo el promedio!", "👑"))
            streak >= 7 -> achievements.add(Achievement(AchievementType.SEVEN_DAY_STREAK, "Semana Perfecta", "7 días consecutivos mejorando", "🔥"))
            streak >= 3 -> achievements.add(Achievement(AchievementType.THREE_DAY_STREAK, "Buena Racha", "3 días consecutivos mejorando", "⭐"))
        }
        
        return achievements
    }
    
    private fun generateRecommendations(
        apps: List<AppUsageSummary>,
        wellbeingScore: Float,
        lateNightMinutes: Int,
        morningMinutes: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (wellbeingScore < 50) {
            recommendations.add("🎯 Tu score de bienestar es bajo. Considera activar el Modo Nuclear por 7 días para un reset completo.")
        }
        
        if (lateNightMinutes > 30) {
            recommendations.add("🌙 Reduces tu uso nocturno. Activa la Ventana de Sueño para bloquear apps después de las 10 PM.")
        }
        
        if (morningMinutes > 20) {
            recommendations.add("🌅 Intenta no usar el teléfono en la primera hora del día. Establece una rutina matutina sin pantallas.")
        }
        
        val worstApp = apps.maxByOrNull { it.addictionScore }
        if (worstApp != null && worstApp.addictionScore > 60) {
            recommendations.add("⚠️ ${worstApp.appName} tiene un score de adicción de ${worstApp.addictionScore.toInt()}. Reduce su límite a ${max(10, worstApp.dailyAverage - 10)} min/día.")
        }
        
        val escalatingApps = apps.filter { it.trend in listOf(TrendDirection.INCREASING, TrendDirection.SHARPLY_INCREASING) }
        if (escalatingApps.isNotEmpty()) {
            recommendations.add("📈 ${escalatingApps.joinToString(", ") { it.appName }} están en aumento. Activa el Ayuno Digital para estas apps.")
        }
        
        val overridingApps = apps.filter { it.timesOverridden > 2 }
        if (overridingApps.isNotEmpty()) {
            recommendations.add("🔓 Desbloqueaste ${overridingApps.joinToString(", ") { it.appName }} varias veces. Activa el modo estricto para evitar tentaciones.")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ ¡Buen trabajo! Mantén tus hábitos actuales y sigue mejorando poco a poco.")
        }
        
        return recommendations.take(5)
    }
    
    private fun generateMotivationalMessage(changePercent: Float, wellbeingScore: Float, totalBlocks: Int): String {
        return when {
            changePercent < -20 && wellbeingScore > 70 -> 
                "🎉 ¡Semana increíble! Redujiste tu tiempo de pantalla un ${abs(changePercent).toInt()}%. Tu disciplina está dando frutos. Cada minuto que recuperas es un minuto para vivir."
            changePercent < -10 -> 
                "📉 ¡Gran progreso! ${abs(changePercent).toInt()}% menos de pantalla. Los pequeños cambios crean grandes transformaciones. Sigue así."
            changePercent in -10f..10f && wellbeingScore > 60 -> 
                "➡️ Semana estable. A veces mantener es tan importante como mejorar. Intenta reducir un 5% la próxima semana."
            changePercent > 20 -> 
                "📈 Tu uso aumentó esta semana. No te juzgues, pero sé consciente. Mañana es una nueva oportunidad. ¿Qué cambio pequeño puedes hacer?"
            totalBlocks > 0 && changePercent > 0 -> 
                "💪 Aunque tu uso subió, bloqueaste $totalBlocks intentos. Tu sistema de protección está funcionando. Ajusta los límites para la próxima semana."
            else -> 
                "🌱 Cada día es una oportunidad para construir una relación más saludable con la tecnología. Un paso a la vez."
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════
    
    private fun getPatternTitle(type: PatternType): String = when (type) {
        PatternType.MORNING_SCROLL -> "Scroll Matutino Compulsivo"
        PatternType.LATE_NIGHT_BINGE -> "Uso Nocturno Excesivo"
        PatternType.WORK_BREAK_ESCAPE -> "Escape Durante el Trabajo"
        PatternType.BOREDOM_TRIGGER -> "Uso por Aburrimiento"
        PatternType.STRESS_RESPONSE -> "Respuesta al Estrés"
        PatternType.WEEKEND_SPIKE -> "Pico de Fin de Semana"
        PatternType.STEADY_MODERATE -> "Uso Estable Moderado"
        PatternType.ESCALATING_WEEKLY -> "Escalación Semanal"
        PatternType.POST_BLOCK_REBOUND -> "Rebote Post-Bloqueo"
        PatternType.NOTIFICATION_DRIVEN -> "Uso por Notificaciones"
        PatternType.SOCIAL_CHAIN -> "Cadena Social"
        PatternType.PROCRASTINATION_LOOP -> "Loop de Procrastinación"
    }
    
    private fun getPatternIcon(type: PatternType): String = when (type) {
        PatternType.MORNING_SCROLL -> "🌅"
        PatternType.LATE_NIGHT_BINGE -> "🌙"
        PatternType.WORK_BREAK_ESCAPE -> "💼"
        PatternType.BOREDOM_TRIGGER -> "😐"
        PatternType.STRESS_RESPONSE -> "😰"
        PatternType.WEEKEND_SPIKE -> "📅"
        PatternType.STEADY_MODERATE -> "✅"
        PatternType.ESCALATING_WEEKLY -> "📈"
        PatternType.POST_BLOCK_REBOUND -> "🔄"
        PatternType.NOTIFICATION_DRIVEN -> "🔔"
        PatternType.SOCIAL_CHAIN -> "🔗"
        PatternType.PROCRASTINATION_LOOP -> "🔁"
    }
    
    private fun getPatternAction(type: PatternType): String = when (type) {
        PatternType.MORNING_SCROLL -> "Activa el perfil 'Dormido' hasta las 8 AM"
        PatternType.LATE_NIGHT_BINGE -> "Configura la Ventana de Sueño a partir de las 10 PM"
        PatternType.WORK_BREAK_ESCAPE -> "Activa el perfil 'Trabajo' durante horario laboral"
        PatternType.BOREDOM_TRIGGER -> "Cuando sientas la urgencia, haz 3 respiraciones profundas"
        PatternType.STRESS_RESPONSE -> "Considera técnicas de manejo del estrés: meditación, ejercicio"
        PatternType.WEEKEND_SPIKE -> "Planifica actividades offline para los fines de semana"
        PatternType.STEADY_MODERATE -> "Mantén tus hábitos actuales"
        PatternType.ESCALATING_WEEKLY -> "Reduce tu límite un 15% esta semana"
        PatternType.POST_BLOCK_REBOUND -> "Activa el modo estricto sin opción de desbloqueo"
        PatternType.NOTIFICATION_DRIVEN -> "Desactiva las notificaciones de esta app"
        PatternType.SOCIAL_CHAIN -> "Bloquea las apps relacionadas en grupo"
        PatternType.PROCRASTINATION_LOOP -> "Usa la técnica Pomodoro: 25 min trabajo, 5 min descanso"
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) { packageName.substringAfterLast(".") }
    }
    
    private fun getWeekStart(cal: Calendar): Long {
        return Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun getTimestamp(daysAgo: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
}

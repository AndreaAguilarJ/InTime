package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentummm.app.R
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.util.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AppUsageData(
    val appName: String,
    val packageName: String,
    val totalTime: Long, // in minutes
    val sessions: Int,
    val lastUsed: String,
    val category: AppCategory
)

data class WeeklyUsageData(
    val day: String,
    val screenTime: Float, // in hours
    val pickups: Int,
    val mostUsedApp: String
)

data class InsightData(
    val title: String,
    val description: String,
    val value: String,
    val change: Float,
    val isPositive: Boolean,
    val iconName: String
)

enum class AppCategory(@androidx.annotation.StringRes val displayNameRes: Int) {
    SOCIAL(R.string.analytics_category_social),
    ENTERTAINMENT(R.string.analytics_category_entertainment),
    PRODUCTIVITY(R.string.analytics_category_productivity),
    GAMES(R.string.analytics_category_games),
    COMMUNICATION(R.string.analytics_category_communication),
    NEWS(R.string.analytics_category_news),
    HEALTH(R.string.analytics_category_health),
    OTHER(R.string.analytics_category_other)
}

data class AdvancedAnalyticsUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val selectedPeriod: TimePeriod = TimePeriod.LAST_WEEK,
    val totalScreenTime: String = "0h 0m",
    val averageDailyScreenTime: String = "0h 0m",
    val totalPickups: Int = 0,
    val mostUsedApp: String = "-",
    val topApps: List<AppUsageData> = emptyList(),
    val weeklyData: List<WeeklyUsageData> = emptyList(),
    val insights: List<InsightData> = emptyList(),
    val categoryBreakdown: Map<AppCategory, Long> = emptyMap()
)

enum class TimePeriod(@androidx.annotation.StringRes val displayNameRes: Int, val days: Int) {
    TODAY(R.string.analytics_period_today, 1),
    LAST_WEEK(R.string.analytics_period_last_week, 7),
    LAST_MONTH(R.string.analytics_period_last_month, 30),
    LAST_YEAR(R.string.analytics_period_last_year, 365)
}

// Data class para datos procesados de analytics
private data class ProcessedAnalyticsData(
    val formattedTotal: String,
    val formattedAverage: String,
    val mostUsedApp: String,
    val rawTotalTime: Long
)

class AdvancedAnalyticsViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedAnalyticsUiState())
    val uiState: StateFlow<AdvancedAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalyticsData()
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadAnalyticsData()
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val hasPermission = PermissionUtils.hasUsageStatsPermission(context)

            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasPermission = false
                )
                return@launch
            }

            try {
                val period = _uiState.value.selectedPeriod

                // Obtener datos según el período seleccionado - en background thread
                val (usageStats, todayUsageStats) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val stats = when (period) {
                        TimePeriod.TODAY -> usageStatsRepository.getTodayUsageStats()
                        TimePeriod.LAST_WEEK -> usageStatsRepository.getWeeklyUsageStats()
                        TimePeriod.LAST_MONTH -> getUsageStatsForDays(30)
                        TimePeriod.LAST_YEAR -> getUsageStatsForDays(365)
                    }
                    val today = usageStatsRepository.getTodayUsageStats()
                    stats to today
                }

                // Procesar datos en Default dispatcher para no bloquear Main
                val processedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    // Calcular tiempo total del período
                    val totalPeriodTime = usageStats.sumOf { it.totalTimeInMillis }
                    val totalPeriodTimeFormatted = formatTime(totalPeriodTime)

                    // Calcular promedio diario
                    val averageDailyTime = totalPeriodTime / period.days
                    val averageDailyFormatted = formatTime(averageDailyTime)

                    // Obtener app más usada
                    val mostUsedAppName = usageStats.firstOrNull()?.appName ?: "-"

                    // Retornar todos los datos necesarios
                    ProcessedAnalyticsData(totalPeriodTimeFormatted, averageDailyFormatted, mostUsedAppName, totalPeriodTime)
                }

                val totalPeriodTimeFormatted = processedData.formattedTotal
                val averageDailyFormatted = processedData.formattedAverage
                val mostUsedAppName = processedData.mostUsedApp
                val totalPeriodTime = processedData.rawTotalTime

                // Convertir a AppUsageData con categorías
                val topApps = usageStats.take(12).map { appInfo ->
                    AppUsageData(
                        appName = appInfo.appName,
                        packageName = appInfo.packageName,
                        totalTime = appInfo.totalTimeInMillis / 60000, // convertir a minutos
                        sessions = estimateSessions(appInfo.totalTimeInMillis),
                        lastUsed = formatLastUsed(appInfo.lastTimeUsed),
                        category = determineAppCategory(appInfo.packageName, appInfo.appName)
                    )
                }

                // Generar datos del período - en background thread para evitar ANR
                val periodData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    generatePeriodData(period)
                }

                // Generar insights
                val insights = generateInsights(usageStats, todayUsageStats, totalPeriodTime)

                // Calcular breakdown por categoría
                val categoryBreakdown = topApps.groupBy { it.category }
                    .mapValues { (_, apps) -> apps.sumOf { it.totalTime } }

                // Estimar desbloqueos (aproximación basada en sesiones)
                val totalPickups = topApps.sumOf { it.sessions }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasPermission = true,
                    totalScreenTime = totalPeriodTimeFormatted,
                    averageDailyScreenTime = averageDailyFormatted,
                    totalPickups = totalPickups,
                    mostUsedApp = mostUsedAppName,
                    topApps = topApps,
                    weeklyData = periodData,
                    insights = insights,
                    categoryBreakdown = categoryBreakdown
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasPermission = true
                )
            }
        }
    }

    private fun getUsageStatsForDays(days: Int): List<AppUsageInfo> {
        // Por ahora usar los datos semanales como base
        // En una implementación completa, se debería crear un método en UsageStatsRepository
        return usageStatsRepository.getWeeklyUsageStats()
    }

    private fun generatePeriodData(period: TimePeriod): List<WeeklyUsageData> {
        val calendar = Calendar.getInstance()
        val periodData = mutableListOf<WeeklyUsageData>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val daysToShow = when (period) {
            TimePeriod.TODAY -> 1
            TimePeriod.LAST_WEEK -> 7
            TimePeriod.LAST_MONTH -> 30
            TimePeriod.LAST_YEAR -> 12 // Mostrar por meses
        }

        if (period == TimePeriod.LAST_YEAR) {
            // Para año, mostrar datos mensuales
            for (i in 11 downTo 0) {
                calendar.add(Calendar.MONTH, if (i == 11) -11 else 1)
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val monthName = monthFormat.format(calendar.time)

                val dayStats = usageStatsRepository.getWeeklyUsageStats()
                val totalTime = dayStats.sumOf { it.totalTimeInMillis }
                val screenTimeHours = totalTime / (1000f * 60f * 60f)

                periodData.add(
                    WeeklyUsageData(
                        day = monthName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        screenTime = screenTimeHours,
                        pickups = dayStats.sumOf { estimateSessions(it.totalTimeInMillis) },
                        mostUsedApp = dayStats.firstOrNull()?.appName ?: "-"
                    )
                )
            }
        } else {
            // Para otros períodos, mostrar por días
            for (i in (daysToShow - 1) downTo 0) {
                calendar.add(Calendar.DAY_OF_YEAR, if (i == daysToShow - 1) -(daysToShow - 1) else 1)
                val dayStart = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis

                val dayEnd = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                // Obtener estadísticas del día
                val dayStats = getDayUsageStats(dayStart, dayEnd)
                val totalTime = dayStats.sumOf { it.totalTimeInMillis }
                val screenTimeHours = totalTime / (1000f * 60f * 60f)

                val label = when (period) {
                    TimePeriod.TODAY -> context.getString(R.string.analytics_today_label)
                    TimePeriod.LAST_MONTH -> dateFormat.format(Date(dayStart))
                    else -> dayFormat.format(Date(dayStart)).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                }

                periodData.add(
                    WeeklyUsageData(
                        day = label,
                        screenTime = screenTimeHours,
                        pickups = dayStats.sumOf { estimateSessions(it.totalTimeInMillis) },
                        mostUsedApp = dayStats.firstOrNull()?.appName ?: "-"
                    )
                )
            }
        }

        return periodData
    }

    private fun generateWeeklyData(): List<WeeklyUsageData> {
        return generatePeriodData(TimePeriod.LAST_WEEK)
    }

    private fun getDayUsageStats(@Suppress("UNUSED_PARAMETER") startTime: Long, @Suppress("UNUSED_PARAMETER") endTime: Long): List<AppUsageInfo> {
        // Esta función debería agregarse al UsageStatsRepository, pero por ahora
        // usamos los datos semanales como aproximación
        return usageStatsRepository.getWeeklyUsageStats()
    }

    private fun generateInsights(
        weeklyStats: List<AppUsageInfo>,
        todayStats: List<AppUsageInfo>,
        totalWeeklyTime: Long
    ): List<InsightData> {
        val insights = mutableListOf<InsightData>()

        // Promedio diario
        val avgDaily = totalWeeklyTime / 7
        insights.add(
            InsightData(
                title = context.getString(R.string.analytics_insight_avg_daily_title),
                description = context.getString(R.string.analytics_insight_avg_daily_desc),
                value = formatTime(avgDaily),
                change = -12.5f, // Esto debería calcularse comparando con la semana anterior
                isPositive = true,
                iconName = "TrendingDown"
            )
        )

        // App más usada
        val topApp = weeklyStats.firstOrNull()
        if (topApp != null) {
            insights.add(
                InsightData(
                    title = context.getString(R.string.analytics_insight_top_app_title),
                    description = context.getString(
                        R.string.analytics_insight_top_app_desc,
                        topApp.appName
                    ),
                    value = formatTime(topApp.totalTimeInMillis),
                    change = 18.2f,
                    isPositive = false,
                    iconName = "Smartphone"
                )
            )
        }

        // Desbloqueos estimados
        val totalSessions = todayStats.sumOf { estimateSessions(it.totalTimeInMillis) }
        insights.add(
            InsightData(
                title = context.getString(R.string.analytics_insight_pickups_title),
                description = context.getString(R.string.analytics_insight_pickups_desc),
                value = totalSessions.toString(),
                change = -8.3f,
                isPositive = true,
                iconName = "TouchApp"
            )
        )

        // Hora pico (aproximación - hora actual si hay uso)
        val calendar = Calendar.getInstance()
        insights.add(
            InsightData(
                title = context.getString(R.string.analytics_insight_peak_time_title),
                description = context.getString(R.string.analytics_insight_peak_time_desc),
                value = String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), 30),
                change = 0f,
                isPositive = true,
                iconName = "Schedule"
            )
        )

        // Productividad
        val productivityApps = weeklyStats.filter {
            determineAppCategory(it.packageName, it.appName) == AppCategory.PRODUCTIVITY
        }
        val productivityTime = productivityApps.sumOf { it.totalTimeInMillis }
        insights.add(
            InsightData(
                title = context.getString(R.string.analytics_insight_productivity_title),
                description = context.getString(R.string.analytics_insight_productivity_desc),
                value = formatTime(productivityTime),
                change = 25.5f,
                isPositive = true,
                iconName = "WorkOutline"
            )
        )

        // Social Media
        val socialApps = weeklyStats.filter {
            determineAppCategory(it.packageName, it.appName) == AppCategory.SOCIAL
        }
        val socialTime = socialApps.sumOf { it.totalTimeInMillis }
        insights.add(
            InsightData(
                title = context.getString(R.string.analytics_insight_social_title),
                description = context.getString(R.string.analytics_insight_social_desc),
                value = formatTime(socialTime),
                change = -5.8f,
                isPositive = true,
                iconName = "Groups"
            )
        )

        return insights
    }

    private fun determineAppCategory(packageName: String, appName: String): AppCategory {
        return when {
            packageName.contains("instagram", ignoreCase = true) ||
            packageName.contains("facebook", ignoreCase = true) ||
            packageName.contains("twitter", ignoreCase = true) ||
            packageName.contains("tiktok", ignoreCase = true) ||
            packageName.contains("snapchat", ignoreCase = true) ||
            packageName.contains("reddit", ignoreCase = true) -> AppCategory.SOCIAL

            packageName.contains("youtube", ignoreCase = true) ||
            packageName.contains("netflix", ignoreCase = true) ||
            packageName.contains("spotify", ignoreCase = true) ||
            packageName.contains("music", ignoreCase = true) ||
            packageName.contains("video", ignoreCase = true) -> AppCategory.ENTERTAINMENT

            packageName.contains("whatsapp", ignoreCase = true) ||
            packageName.contains("telegram", ignoreCase = true) ||
            packageName.contains("messenger", ignoreCase = true) ||
            packageName.contains("skype", ignoreCase = true) ||
            packageName.contains("zoom", ignoreCase = true) ||
            packageName.contains("mms", ignoreCase = true) ||
            packageName.contains("dialer", ignoreCase = true) -> AppCategory.COMMUNICATION

            packageName.contains("gmail", ignoreCase = true) ||
            packageName.contains("chrome", ignoreCase = true) ||
            packageName.contains("docs", ignoreCase = true) ||
            packageName.contains("sheets", ignoreCase = true) ||
            packageName.contains("notion", ignoreCase = true) ||
            packageName.contains("office", ignoreCase = true) ||
            packageName.contains("calendar", ignoreCase = true) -> AppCategory.PRODUCTIVITY

            packageName.contains("game", ignoreCase = true) ||
            appName.contains("game", ignoreCase = true) -> AppCategory.GAMES

            packageName.contains("news", ignoreCase = true) ||
            packageName.contains("medium", ignoreCase = true) -> AppCategory.NEWS

            packageName.contains("health", ignoreCase = true) ||
            packageName.contains("fitness", ignoreCase = true) ||
            packageName.contains("fitbit", ignoreCase = true) -> AppCategory.HEALTH

            else -> AppCategory.OTHER
        }
    }

    private fun estimateSessions(totalTime: Long): Int {
        // Estimar sesiones: asumir que cada sesión dura en promedio 10 minutos
        val averageSessionMinutes = 10
        val totalMinutes = totalTime / 60000
        return maxOf(1, (totalMinutes / averageSessionMinutes).toInt())
    }

    private fun formatTime(timeInMillis: Long): String {
        return LifeWeeksCalculator.formatTimeFromMillis(timeInMillis)
    }

    private fun formatLastUsed(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> context.getString(R.string.analytics_last_used_one_min)
            diff < 3600000 -> {
                val minutes = (diff / 60000).toInt()
                context.resources.getQuantityString(
                    R.plurals.analytics_last_used_minutes,
                    minutes,
                    minutes
                )
            }
            diff < 86400000 -> {
                val hours = (diff / 3600000).toInt()
                context.resources.getQuantityString(
                    R.plurals.analytics_last_used_hours,
                    hours,
                    hours
                )
            }
            diff < 604800000 -> {
                val days = (diff / 86400000).toInt()
                context.resources.getQuantityString(
                    R.plurals.analytics_last_used_days,
                    days,
                    days
                )
            }
            else -> context.getString(R.string.analytics_last_used_more_than_week)
        }
    }

    fun refresh() {
        loadAnalyticsData()
    }
}

class AdvancedAnalyticsViewModelFactory(
    private val usageStatsRepository: UsageStatsRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvancedAnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdvancedAnalyticsViewModel(usageStatsRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

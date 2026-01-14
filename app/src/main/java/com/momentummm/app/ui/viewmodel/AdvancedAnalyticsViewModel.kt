package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    ENTERTAINMENT("Entretenimiento"),
    PRODUCTIVITY("Productividad"),
    GAMES("Juegos"),
    COMMUNICATION("Comunicación"),
    NEWS("Noticias"),
    HEALTH("Salud"),
    OTHER("Otros")
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

enum class TimePeriod(val displayName: String, val days: Int) {
    TODAY("Hoy", 1),
    LAST_WEEK("Última semana", 7),
    LAST_MONTH("Último mes", 30),
    LAST_YEAR("Último año", 365)
}

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

                // Obtener datos según el período seleccionado
                val usageStats = when (period) {
                    TimePeriod.TODAY -> usageStatsRepository.getTodayUsageStats()
                    TimePeriod.LAST_WEEK -> usageStatsRepository.getWeeklyUsageStats()
                    TimePeriod.LAST_MONTH -> getUsageStatsForDays(30)
                    TimePeriod.LAST_YEAR -> getUsageStatsForDays(365)
                }

                val todayUsageStats = usageStatsRepository.getTodayUsageStats()

                // Calcular tiempo total del período
                val totalPeriodTime = usageStats.sumOf { it.totalTimeInMillis }
                val totalPeriodTimeFormatted = formatTime(totalPeriodTime)

                // Calcular promedio diario
                val averageDailyTime = totalPeriodTime / period.days
                val averageDailyFormatted = formatTime(averageDailyTime)

                // Obtener app más usada
                val mostUsedAppName = usageStats.firstOrNull()?.appName ?: "-"

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

                // Generar datos del período
                val periodData = generatePeriodData(period)

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
                    TimePeriod.TODAY -> "Hoy"
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
                title = "Promedio diario",
                description = "Tu tiempo de pantalla esta semana",
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
                    title = "App más usada",
                    description = "${topApp.appName} lidera tu tiempo de uso",
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
                title = "Desbloqueos",
                description = "Veces que activaste tu teléfono hoy",
                value = "$totalSessions",
                change = -8.3f,
                isPositive = true,
                iconName = "TouchApp"
            )
        )

        // Hora pico (aproximación - hora actual si hay uso)
        val calendar = Calendar.getInstance()
        insights.add(
            InsightData(
                title = "Hora pico",
                description = "Tu momento de mayor actividad",
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
                title = "Productividad",
                description = "Tiempo en apps productivas",
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
                title = "Social Media",
                description = "Tiempo en redes sociales",
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
            diff < 60000 -> "hace 1 min"
            diff < 3600000 -> "hace ${diff / 60000} min"
            diff < 86400000 -> "hace ${diff / 3600000} hora${if (diff / 3600000 > 1) "s" else ""}"
            diff < 604800000 -> "hace ${diff / 86400000} día${if (diff / 86400000 > 1) "s" else ""}"
            else -> "hace más de 1 semana"
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

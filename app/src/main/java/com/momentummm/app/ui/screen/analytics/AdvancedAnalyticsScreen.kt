package com.momentummm.app.ui.screen.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes
import com.momentummm.app.R
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.theme.MomentumTextStyles
import com.momentummm.app.ui.theme.momentum
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import com.momentummm.app.ui.viewmodel.AdvancedAnalyticsViewModel
import com.momentummm.app.ui.viewmodel.AdvancedAnalyticsViewModelFactory
import com.momentummm.app.ui.viewmodel.TimePeriod
import com.momentummm.app.ui.viewmodel.AppCategory as VMAppCategory

// Mantener enums locales para compatibilidad con UI
enum class AppCategory(@StringRes val displayNameRes: Int, val color: Color) {
    Social(R.string.analytics_category_social, com.momentummm.app.ui.theme.Violet500),
    Entertainment(R.string.analytics_category_entertainment, com.momentummm.app.ui.theme.Rose500),
    Productivity(R.string.analytics_category_productivity, com.momentummm.app.ui.theme.Mint500),
    Games(R.string.analytics_category_games, com.momentummm.app.ui.theme.Amber500),
    Communication(R.string.analytics_category_communication, com.momentummm.app.ui.theme.Sky500),
    News(R.string.analytics_category_news, com.momentummm.app.ui.theme.Coral500),
    Health(R.string.analytics_category_health, com.momentummm.app.ui.theme.Indigo400),
    Other(R.string.analytics_category_other, com.momentummm.app.ui.theme.Neutral400)
}

// Data classes locales para la UI
data class UsageData(
    val appName: String,
    val packageName: String,
    val totalTime: Long,
    val sessions: Int,
    val lastUsed: String,
    val category: AppCategory,
    val icon: String? = null
)

data class WeeklyData(
    val day: String,
    val screenTime: Float,
    val pickups: Int,
    val mostUsedApp: String
)

data class InsightData(
    val title: String,
    val description: String,
    val value: String,
    val change: Float,
    val isPositive: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Función de conversión de categorías
private fun VMAppCategory.toUICategory(): AppCategory {
    return when (this) {
        VMAppCategory.SOCIAL -> AppCategory.Social
        VMAppCategory.ENTERTAINMENT -> AppCategory.Entertainment
        VMAppCategory.PRODUCTIVITY -> AppCategory.Productivity
        VMAppCategory.GAMES -> AppCategory.Games
        VMAppCategory.COMMUNICATION -> AppCategory.Communication
        VMAppCategory.NEWS -> AppCategory.News
        VMAppCategory.HEALTH -> AppCategory.Health
        VMAppCategory.OTHER -> AppCategory.Other
    }
}

// Función para convertir nombre de icono a ImageVector
private fun getIconFromName(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "TrendingDown" -> Icons.Filled.TrendingDown
        "TrendingUp" -> Icons.Filled.TrendingUp
        "Smartphone" -> Icons.Filled.Smartphone
        "TouchApp" -> Icons.Filled.TouchApp
        "Schedule" -> Icons.Filled.Schedule
        "WorkOutline" -> Icons.Filled.WorkOutline
        "Groups" -> Icons.Filled.Groups
        else -> Icons.Filled.Analytics
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalyticsScreen(
    isPremiumUser: Boolean,
    onUpgradeClick: () -> Unit
) {
    // DEVELOPMENT MODE: Always show content (comment out for production)
    val showContent = true // Change to isPremiumUser for production

    val context = LocalContext.current
    val viewModel: AdvancedAnalyticsViewModel = viewModel(
        factory = AdvancedAnalyticsViewModelFactory(
            UsageStatsRepository(context),
            context
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Si no tiene permiso, mostrar mensaje
    if (!uiState.hasPermission && !uiState.isLoading) {
        PermissionRequiredScreen()
        return
    }

    if (!showContent) {
        PremiumUpsellScreen(onUpgradeClick = onUpgradeClick)
        return
    }
    
    // Mostrar loading
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Convertir datos del ViewModel a formato UI. Se memoiza con remember(key)
    // para no rehacer el .map en cada recomposición (solo cuando cambian los datos).
    val usageData = remember(uiState.topApps) {
        uiState.topApps.map { appData ->
            UsageData(
                appName = appData.appName,
                packageName = appData.packageName,
                totalTime = appData.totalTime,
                sessions = appData.sessions,
                lastUsed = appData.lastUsed,
                category = appData.category.toUICategory()
            )
        }
    }

    val weeklyData = remember(uiState.weeklyData) {
        uiState.weeklyData.map { weekData ->
            WeeklyData(
                day = weekData.day,
                screenTime = weekData.screenTime,
                pickups = weekData.pickups,
                mostUsedApp = weekData.mostUsedApp
            )
        }
    }

    val insights = remember(uiState.insights) {
        uiState.insights.map { insightData ->
            InsightData(
                title = insightData.title,
                description = insightData.description,
                value = insightData.value,
                change = insightData.change,
                isPositive = insightData.isPositive,
                icon = getIconFromName(insightData.iconName)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.momentum.canvas),
        contentPadding = PaddingValues(
            start = MomentumDesign.Spacing.screenHorizontal,
            end = MomentumDesign.Spacing.screenHorizontal,
            top = MomentumDesign.Spacing.small,
            bottom = MomentumDesign.Spacing.large,
        ),
        verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.medium)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.momentum.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.analytics_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.momentum.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))
                // Botón de refresco como tile suave: el círculo relleno de primary
                // competía visualmente con el título.
                Box(
                    modifier = Modifier
                        .size(MomentumDesign.Size.iconTile)
                        .clip(MomentumDesign.Shapes.pill)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = MomentumDesign.Alpha.soft))
                        .clickable { viewModel.refresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.analytics_refresh_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MomentumDesign.Size.iconLarge)
                    )
                }
            }
        }

        item {
            val periods = TimePeriod.values()
            SegmentedTabs(
                options = periods.map { stringResource(it.displayNameRes) },
                selectedIndex = periods.indexOf(uiState.selectedPeriod).coerceAtLeast(0),
                onSelect = { index -> viewModel.selectPeriod(periods[index]) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            // Summary Stats Card - USANDO DATOS REALES
            SummaryStatsCard(
                totalScreenTime = uiState.totalScreenTime,
                averageDaily = uiState.averageDailyScreenTime,
                totalPickups = uiState.totalPickups,
                mostUsedApp = uiState.mostUsedApp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            // Weekly chart - USANDO DATOS REALES
            if (weeklyData.isNotEmpty()) {
                WeeklyUsageChart(
                    data = weeklyData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        item {
            // Key insights - USANDO DATOS REALES
            if (insights.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_key_insights_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(insights) { insight ->
                        InsightCard(
                            insight = insight,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }
        }
        
        item {
            // Category breakdown - USANDO DATOS REALES
            if (usageData.isNotEmpty()) {
                CategoryBreakdownChart(
                    data = usageData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        item {
            // App usage list - USANDO DATOS REALES
            if (usageData.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_usage_by_app_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        items(usageData) { app ->
            AppUsageCard(
                app = app,
                totalTime = usageData.sumOf { it.totalTime }.coerceAtLeast(1L),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // Productivity insights
            if (usageData.isNotEmpty()) {
                ProductivityInsightsCard(
                    weeklyData = weeklyData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.analytics_permission_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.analytics_permission_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.analytics_permission_instruction),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PremiumUpsellScreen(
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.analytics_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.analytics_premium_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val features = listOf(
            stringResource(R.string.analytics_premium_feature_1),
            stringResource(R.string.analytics_premium_feature_2),
            stringResource(R.string.analytics_premium_feature_3),
            stringResource(R.string.analytics_premium_feature_4),
            stringResource(R.string.analytics_premium_feature_5),
            stringResource(R.string.analytics_premium_feature_6)
        )
        
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MomentumButton(
            onClick = onUpgradeClick,
            style = ButtonStyle.Primary,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.analytics_premium_cta))
        }
    }
}

@Composable
private fun WeeklyUsageChart(
    data: List<WeeklyData>,
    modifier: Modifier = Modifier
) {
    val busiest = remember(data) { data.maxByOrNull { it.screenTime } }

    MomentumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.cozy)) {
            SectionHeader(
                title = stringResource(R.string.analytics_weekly_chart_title),
                subtitle = busiest?.let {
                    stringResource(R.string.analytics_weekly_peak, it.day, it.mostUsedApp)
                },
            )

            Spacer(Modifier.height(MomentumDesign.Spacing.cozy))

            BarChart(
                data = data.map { day ->
                    BarDatum(label = day.day.take(3), value = day.screenTime)
                },
                accent = MaterialTheme.colorScheme.primary,
                valueFormatter = { hours ->
                    if (hours <= 0f) "-" else String.format(Locale.US, "%.1f", hours)
                },
            )

            Spacer(Modifier.height(MomentumDesign.Spacing.medium))
            MomentumDivider()
            Spacer(Modifier.height(MomentumDesign.Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
            ) {
                val totalHours = data.sumOf { it.screenTime.toDouble() }.toFloat()
                val totalPickups = data.sumOf { it.pickups }
                InlineMetric(
                    label = stringResource(R.string.analytics_total_label),
                    value = String.format(Locale.US, "%.1f h", totalHours),
                    modifier = Modifier.weight(1f)
                )
                InlineMetric(
                    label = stringResource(R.string.analytics_pickups_label),
                    value = totalPickups.toString(),
                    modifier = Modifier.weight(1f)
                )
                InlineMetric(
                    label = stringResource(R.string.analytics_daily_average_label),
                    value = if (data.isEmpty()) "-" else String.format(
                        Locale.US, "%.1f h", totalHours / data.size
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Métrica en línea, sin caja, para pies de tarjeta. */
@Composable
private fun InlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.momentum.textPrimary,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.momentum.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun InsightCard(
    insight: InsightData,
    modifier: Modifier = Modifier
) {
    // El signo del cambio no basta para elegir el color: en bienestar digital, bajar
    // el tiempo de pantalla es una buena noticia. `isPositive` ya viene calculado por
    // el ViewModel con esa semántica, así que manda él.
    val accent = if (insight.isPositive) {
        MaterialTheme.momentum.success
    } else {
        MaterialTheme.momentum.danger
    }

    MomentumCard(
        modifier = modifier,
        shape = MomentumDesign.Shapes.cardCompact,
    ) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconTile(
                    icon = insight.icon,
                    tint = accent,
                    size = MomentumDesign.Size.iconTileSmall,
                )
                if (insight.change != 0f) {
                    DeltaBadge(
                        MetricDelta(
                            text = String.format(Locale.US, "%.0f%%", kotlin.math.abs(insight.change)),
                            isUp = insight.change > 0f,
                            goodWhenDown = insight.isPositive == (insight.change < 0f),
                        )
                    )
                }
            }

            Spacer(Modifier.height(MomentumDesign.Spacing.compact))

            Text(
                text = insight.value,
                style = MomentumTextStyles.metric,
                color = MaterialTheme.momentum.textPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(MomentumDesign.Spacing.hairline))
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.momentum.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(MomentumDesign.Spacing.extraSmall))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.momentum.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryBreakdownChart(
    data: List<UsageData>,
    modifier: Modifier = Modifier
) {
    val byCategory = remember(data) {
        data.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalTime } }
            .entries
            .sortedByDescending { it.value }
    }
    val total = remember(byCategory) { byCategory.sumOf { it.value }.coerceAtLeast(1L) }

    val segments = byCategory.map { entry ->
        RingSegment(
            label = stringResource(entry.key.displayNameRes),
            value = entry.value.toFloat(),
            color = entry.key.color,
        )
    }

    MomentumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.cozy)) {
            SectionHeader(
                title = stringResource(R.string.analytics_category_breakdown_title),
                subtitle = stringResource(R.string.analytics_category_breakdown_subtitle),
            )

            Spacer(Modifier.height(MomentumDesign.Spacing.cozy))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SegmentedRing(
                    segments = segments,
                    diameter = 176.dp,
                    strokeWidth = 18.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = byCategory.size.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.momentum.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.analytics_categories_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.momentum.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(MomentumDesign.Spacing.cozy))

            Column(verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)) {
                byCategory.forEach { entry ->
                    val share = entry.value.toFloat() / total.toFloat()
                    LegendRow(
                        label = stringResource(entry.key.displayNameRes),
                        value = String.format(Locale.US, "%d%%", (share * 100).toInt()),
                        color = entry.key.color,
                        share = share,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    app: UsageData,
    totalTime: Long,
    modifier: Modifier = Modifier
) {
    // Protección contra división por cero
    val safeTotalTime = totalTime.coerceAtLeast(1L)
    
    MomentumCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono REAL de la app (se carga del sistema por packageName);
            // cae al mosaico de color + inicial si la app no está instalada.
            com.momentummm.app.ui.component.AppIconImage(
                packageName = app.packageName,
                appName = app.appName,
                fallbackColor = app.category.color,
                size = 48.dp,
                cornerRadius = 12.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.analytics_app_usage_time_sessions,
                        app.totalTime / 60,
                        app.totalTime % 60,
                        app.sessions
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.analytics_app_usage_last_used,
                        app.lastUsed
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(app.totalTime.toFloat() / safeTotalTime.toFloat() * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Usage bar
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    val animatedWidth by animateFloatAsState(
                        targetValue = app.totalTime.toFloat() / safeTotalTime.toFloat(),
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "usage_bar"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedWidth)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductivityInsightsCard(
    weeklyData: List<WeeklyData>,
    modifier: Modifier = Modifier
) {
    // Antes esta tarjeta mostraba una frase fija: "tu productividad aumenta un 23% los
    // martes". Ese 23% y ese martes no salian de ningun dato: estaban escritos a mano, e
    // iguales para todos los usuarios, incluido quien acababa de instalar la app. Ahora
    // el dato se deriva del tiempo de pantalla real por dia, y si no hay historial
    // suficiente se dice, en lugar de inventarlo.
    val insight = remember(weeklyData) { calcularDiaConMenosPantalla(weeklyData) }
    MomentumGradientCard(
        modifier = modifier,
        gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.analytics_productivity_insight_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (insight != null) {
                        stringResource(
                            R.string.analytics_productivity_insight_message_day,
                            insight.dia,
                            insight.porcentajeBajoLaMedia
                        )
                    } else {
                        stringResource(R.string.analytics_productivity_insight_message_pending)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStatsCard(
    totalScreenTime: String,
    averageDaily: String,
    totalPickups: Int,
    mostUsedApp: String,
    modifier: Modifier = Modifier
) {
    // Rejilla 2x2 de tarjetas independientes en lugar de un bloque con cuatro
    // columnas: en pantallas estrechas las cifras largas ("12h 34m") ya no se
    // comprimen unas contra otras.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)) {
            StatCard(
                label = stringResource(R.string.analytics_total_label),
                value = totalScreenTime,
                icon = Icons.Filled.Schedule,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.analytics_daily_average_label),
                value = averageDaily,
                icon = Icons.Filled.TrendingUp,
                accent = MaterialTheme.momentum.info,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)) {
            StatCard(
                label = stringResource(R.string.analytics_pickups_label),
                value = totalPickups.toString(),
                icon = Icons.Filled.TouchApp,
                accent = MaterialTheme.momentum.warning,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.analytics_most_used_label),
                value = mostUsedApp,
                icon = Icons.Filled.Star,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

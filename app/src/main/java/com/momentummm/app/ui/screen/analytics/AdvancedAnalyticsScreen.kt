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
import androidx.annotation.StringRes
import com.momentummm.app.R
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.viewmodel.AdvancedAnalyticsViewModel
import com.momentummm.app.ui.viewmodel.AdvancedAnalyticsViewModelFactory
import com.momentummm.app.ui.viewmodel.TimePeriod
import com.momentummm.app.ui.viewmodel.AppCategory as VMAppCategory

// Mantener enums locales para compatibilidad con UI
enum class AppCategory(@StringRes val displayNameRes: Int, val color: Color) {
    Social(R.string.analytics_category_social, Color(0xFF4267B2)),
    Entertainment(R.string.analytics_category_entertainment, Color(0xFFFF1744)),
    Productivity(R.string.analytics_category_productivity, Color(0xFF4CAF50)),
    Games(R.string.analytics_category_games, Color(0xFF9C27B0)),
    Communication(R.string.analytics_category_communication, Color(0xFF2196F3)),
    News(R.string.analytics_category_news, Color(0xFFFF9800)),
    Health(R.string.analytics_category_health, Color(0xFF009688)),
    Other(R.string.analytics_category_other, Color(0xFF607D8B))
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

    val uiState by viewModel.uiState.collectAsState()

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

    // Convertir datos del ViewModel a formato UI
    val usageData = uiState.topApps.map { appData ->
        UsageData(
            appName = appData.appName,
            packageName = appData.packageName,
            totalTime = appData.totalTime,
            sessions = appData.sessions,
            lastUsed = appData.lastUsed,
            category = appData.category.toUICategory()
        )
    }

    val weeklyData = uiState.weeklyData.map { weekData ->
        WeeklyData(
            day = weekData.day,
            screenTime = weekData.screenTime,
            pickups = weekData.pickups,
            mostUsedApp = weekData.mostUsedApp
        )
    }

    val insights = uiState.insights.map { insightData ->
        InsightData(
            title = insightData.title,
            description = insightData.description,
            value = insightData.value,
            change = insightData.change,
            isPositive = insightData.isPositive,
            icon = getIconFromName(insightData.iconName)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.analytics_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.analytics_refresh_cd),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
        
        item {
            // Period selector - AHORA CONECTADO AL VIEWMODEL
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimePeriod.values()) { period ->
                    MomentumChip(
                        text = stringResource(period.displayNameRes),
                        isSelected = uiState.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) }
                    )
                }
            }
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
                totalTime = usageData.sumOf { it.totalTime },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // Productivity insights
            if (usageData.isNotEmpty()) {
                ProductivityInsightsCard(
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
    MomentumCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_weekly_screen_time_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxTime = data.maxOfOrNull { it.screenTime } ?: 1f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { dayData ->
                    val barHeight = ((dayData.screenTime / maxTime) * 160).dp
                    val animatedHeight by animateDpAsState(
                        targetValue = barHeight,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "bar_height"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                R.string.analytics_hours_format,
                                dayData.screenTime
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(animatedHeight)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dayData.day,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: InsightData,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier,
        containerColor = if (insight.isPositive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    insight.icon,
                    contentDescription = null,
                    tint = if (insight.isPositive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                if (insight.change != 0f) {
                    Surface(
                        color = if (insight.isPositive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (insight.change > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (insight.isPositive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${kotlin.math.abs(insight.change)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (insight.isPositive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = insight.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBreakdownChart(
    data: List<UsageData>,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_usage_by_category_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val categoryData = data.groupBy { it.category }
                .mapValues { (_, apps) -> apps.sumOf { it.totalTime } }
                .toList()
                .sortedByDescending { it.second }
            
            val totalTime = categoryData.sumOf { it.second }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie chart
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        var startAngle = 0f
                        categoryData.forEach { (category, time) ->
                            val sweepAngle = (time.toFloat() / totalTime.toFloat()) * 360f
                            drawArc(
                                color = category.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = Size(size.width * 0.8f, size.height * 0.8f),
                                topLeft = Offset(size.width * 0.1f, size.height * 0.1f)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Legend
                Column {
                    categoryData.take(5).forEach { (category, time) ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(category.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(category.displayNameRes),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(time.toFloat() / totalTime.toFloat() * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
    MomentumCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                color = app.category.color,
                shape = RoundedCornerShape(12.dp)
            ) {
                // In real app, load actual app icon
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = app.appName.first().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
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
                    text = "${(app.totalTime.toFloat() / totalTime.toFloat() * 100).toInt()}%",
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
                        targetValue = app.totalTime.toFloat() / totalTime.toFloat(),
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
    modifier: Modifier = Modifier
) {
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
                    text = stringResource(R.string.analytics_productivity_insight_message),
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
    MomentumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total Screen Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = totalScreenTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.analytics_summary_total_screen_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Average Daily
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = averageDaily,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.analytics_summary_daily_average),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Divider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total Pickups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_summary_total_pickups),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(
                        R.string.analytics_summary_pickups_times,
                        totalPickups
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Most Used App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_summary_top_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = mostUsedApp,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

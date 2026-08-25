package com.momentummm.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.R
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.ui.component.GamificationEventToast
import com.momentummm.app.ui.component.GamificationHeader
import com.momentummm.app.ui.system.ButtonSize
import com.momentummm.app.ui.system.ButtonStyle
import com.momentummm.app.ui.system.EmptyState
import com.momentummm.app.ui.system.IconTile
import com.momentummm.app.ui.system.MomentumButton
import com.momentummm.app.ui.system.MomentumCard
import com.momentummm.app.ui.system.MomentumDesign
import com.momentummm.app.ui.system.ProgressBar
import com.momentummm.app.ui.system.RingSegment
import com.momentummm.app.ui.system.SectionHeader
import com.momentummm.app.ui.system.SegmentedRing
import com.momentummm.app.ui.system.StatCard
import com.momentummm.app.ui.theme.MomentumTextStyles
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.ui.viewmodel.DashboardViewModel
import com.momentummm.app.util.LifeWeeksCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    isPremiumUser: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    // Las monedas y el nivel de la cabecera eran pulsables pero su onClick estaba
    // vacio: el usuario tocaba y no pasaba nada. Se expone el destino para que el
    // host decida a donde lleva.
    onCoinsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val hasPermission = com.momentummm.app.util.PermissionUtils.hasUsageStatsPermission(context)
        if (hasPermission != uiState.hasUsagePermission) {
            viewModel.refreshData()
        }
    }

    // El total de cada app se compara contra la más usada, no contra el total del
    // día: así la barra más larga siempre llena la fila y la comparación entre apps
    // se lee de un vistazo.
    val maxAppMillis = remember(uiState.topApps) {
        uiState.topApps.maxOfOrNull { it.totalTimeInMillis } ?: 0L
    }
    val trackedMillis = remember(uiState.topApps) {
        uiState.topApps.sumOf { it.totalTimeInMillis }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.momentum.canvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MomentumDesign.Spacing.screenHorizontal,
                end = MomentumDesign.Spacing.screenHorizontal,
                top = MomentumDesign.Spacing.small,
                bottom = MomentumDesign.Spacing.large,
            ),
            verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.medium)
        ) {
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
                    DashboardGreeting()
                    Spacer(Modifier.height(MomentumDesign.Spacing.medium))
                    GamificationHeader(
                        gamificationState = uiState.gamificationState,
                        onCoinsClick = onCoinsClick
                    )
                }
            }

            item {
                ScreenTimeHero(
                    totalScreenTime = uiState.totalScreenTime,
                    isLoading = uiState.isLoading,
                    hasPermission = uiState.hasUsagePermission,
                    topApps = uiState.topApps,
                    onGrantPermission = {
                        com.momentummm.app.util.PermissionUtils.openUsageStatsSettings(context)
                    }
                )
            }

            if (uiState.hasUsagePermission && uiState.topApps.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)) {
                        StatCard(
                            label = stringResource(R.string.dashboard_apps_tracked),
                            value = uiState.topApps.size.toString(),
                            icon = Icons.Filled.Apps,
                            accent = MaterialTheme.momentum.info,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.dashboard_in_top_apps),
                            value = LifeWeeksCalculator.formatTimeFromMillis(trackedMillis),
                            icon = Icons.Filled.Timelapse,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (!isPremiumUser) {
                item { PremiumPromotionCard(onUpgradeClick = onUpgradeClick) }
            }

            uiState.quoteOfTheDay?.let { quote ->
                item {
                    QuoteCard(
                        text = quote.text,
                        author = quote.author,
                    )
                }
            }

            if (uiState.hasUsagePermission) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.most_used_apps),
                        overline = stringResource(R.string.screen_time_today),
                        modifier = Modifier.padding(top = MomentumDesign.Spacing.small)
                    )
                }

                if (uiState.topApps.isNotEmpty()) {
                    itemsIndexed(uiState.topApps) { index, app ->
                        AppUsageRow(
                            app = app,
                            rank = index + 1,
                            maxMillis = maxAppMillis,
                            accent = MaterialTheme.momentum.series(index),
                        )
                    }
                } else {
                    item {
                        MomentumCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Filled.HourglassEmpty,
                                title = stringResource(R.string.dashboard_no_usage_data),
                                message = stringResource(R.string.dashboard_no_usage_data_hint),
                                accent = MaterialTheme.momentum.textSecondary,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(MomentumDesign.Spacing.small)) }
        }

        GamificationEventToast(
            message = uiState.gamificationEventMessage,
            xpGained = uiState.gamificationEventXp,
            coinsGained = uiState.gamificationEventCoins,
            isLevelUp = uiState.isLevelUpEvent,
            visible = uiState.showGamificationEvent,
            onDismiss = { viewModel.dismissGamificationEvent() }
        )
    }
}

/** Saludo según la hora del día. */
@Composable
private fun DashboardGreeting() {
    val greetingRes = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.greeting_morning
            in 12..19 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }

    Column {
        Text(
            text = stringResource(greetingRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.momentum.textPrimary
        )
        Spacer(Modifier.height(MomentumDesign.Spacing.hairline))
        Text(
            text = stringResource(R.string.screen_time_today),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.momentum.textSecondary
        )
    }
}

/**
 * Tarjeta protagonista: el tiempo de pantalla del día como cifra grande, rodeada de
 * un anillo cuyos segmentos son las apps más usadas. El anillo no representa una
 * meta (la app no la tiene todavía) sino el reparto real del tiempo, así que no
 * comunica nada falso.
 */
@Composable
private fun ScreenTimeHero(
    totalScreenTime: String,
    isLoading: Boolean,
    hasPermission: Boolean,
    topApps: List<AppUsageInfo>,
    onGrantPermission: () -> Unit,
) {
    val segments = remember(topApps) {
        topApps.mapIndexed { index, app ->
            RingSegment(
                label = app.appName,
                value = app.totalTimeInMillis.toFloat(),
                color = com.momentummm.app.ui.theme.DataSeries[index % com.momentummm.app.ui.theme.DataSeries.size],
            )
        }
    }

    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MomentumDesign.Shapes.hero,
        containerColor = MaterialTheme.momentum.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.momentum.veil(0.10f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MomentumDesign.Spacing.cozy,
                        vertical = MomentumDesign.Spacing.large
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.size(MomentumDesign.Size.ring),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    !hasPermission -> {
                        EmptyState(
                            icon = Icons.Filled.LockOpen,
                            title = stringResource(R.string.dashboard_usage_permission_needed),
                            message = stringResource(R.string.dashboard_usage_permission_desc),
                        ) {
                            MomentumButton(
                                onClick = onGrantPermission,
                                style = ButtonStyle.Primary,
                                size = ButtonSize.Large,
                            ) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    }

                    else -> {
                        SegmentedRing(
                            segments = segments,
                            diameter = MomentumDesign.Size.ring,
                            strokeWidth = MomentumDesign.Size.ringStroke,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = totalScreenTime,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.momentum.textPrimary,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(MomentumDesign.Spacing.extraSmall))
                                Text(
                                    text = stringResource(R.string.dashboard_screen_time_total_today),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.momentum.textSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        if (segments.isNotEmpty()) {
                            Spacer(Modifier.height(MomentumDesign.Spacing.cozy))
                            RingLegend(segments)
                        }
                    }
                }
            }
        }
    }
}

/** Leyenda compacta del anillo: punto de color + nombre de la app. */
@Composable
private fun RingLegend(segments: List<RingSegment>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
    ) {
        segments.take(3).chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
            ) {
                row.forEach { segment ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(MomentumDesign.CornerRadius.pill))
                                .background(segment.color)
                        )
                        Spacer(Modifier.width(MomentumDesign.Spacing.small))
                        Text(
                            text = segment.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.momentum.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PremiumPromotionCard(onUpgradeClick: () -> Unit) {
    com.momentummm.app.ui.system.MomentumGradientCard(
        onClick = onUpgradeClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Filled.AutoAwesome,
                tint = androidx.compose.ui.graphics.Color.White,
                background = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
            )
            Spacer(Modifier.width(MomentumDesign.Spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_unlock_premium),
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(Modifier.height(MomentumDesign.Spacing.hairline))
                Text(
                    text = stringResource(R.string.dashboard_unlock_premium_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.82f)
                )
            }
            Spacer(Modifier.width(MomentumDesign.Spacing.small))
            IconTile(
                icon = Icons.Filled.Star,
                tint = androidx.compose.ui.graphics.Color.White,
                background = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                size = MomentumDesign.Size.iconTileSmall,
            )
        }
    }
}

@Composable
private fun QuoteCard(text: String, author: String?) {
    MomentumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.cozy)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = Icons.Filled.FormatQuote,
                    tint = MaterialTheme.colorScheme.tertiary,
                    size = MomentumDesign.Size.iconTileSmall,
                )
                Spacer(Modifier.width(MomentumDesign.Spacing.compact))
                Text(
                    text = stringResource(R.string.quote_of_the_day),
                    style = MomentumTextStyles.overline,
                    color = MaterialTheme.momentum.textSecondary
                )
            }
            Spacer(Modifier.height(MomentumDesign.Spacing.compact))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.momentum.textPrimary
            )
            if (author != null) {
                Spacer(Modifier.height(MomentumDesign.Spacing.small))
                Text(
                    text = stringResource(R.string.dashboard_quote_author, author),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.momentum.textTertiary
                )
            }
        }
    }
}

/**
 * Fila de app: posición, icono real, nombre, tiempo y una barra que compara su uso
 * con el de la app más usada del día.
 */
@Composable
private fun AppUsageRow(
    app: AppUsageInfo,
    rank: Int,
    maxMillis: Long,
    accent: androidx.compose.ui.graphics.Color,
) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    val share = if (maxMillis > 0L) app.totalTimeInMillis.toFloat() / maxMillis.toFloat() else 0f

    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MomentumDesign.Shapes.cardCompact,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomentumDesign.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.momentum.textTertiary,
                modifier = Modifier.width(18.dp)
            )

            if (appIcon != null) {
                Image(
                    painter = BitmapPainter(appIcon.toBitmap().asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(MomentumDesign.Size.iconTile)
                        .clip(RoundedCornerShape(MomentumDesign.CornerRadius.medium))
                )
            } else {
                IconTile(icon = Icons.Filled.Apps, tint = accent)
            }

            Spacer(Modifier.width(MomentumDesign.Spacing.compact))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.momentum.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(MomentumDesign.Spacing.small))
                    Text(
                        text = LifeWeeksCalculator.formatTimeFromMillis(app.totalTimeInMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
                Spacer(Modifier.height(MomentumDesign.Spacing.small))
                ProgressBar(
                    progress = share,
                    color = accent,
                )
            }
        }
    }
}

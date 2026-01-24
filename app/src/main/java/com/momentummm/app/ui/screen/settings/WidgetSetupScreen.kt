package com.momentummm.app.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.annotation.StringRes
import com.momentummm.app.R
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.widget.LifeWeeksWidget
import com.momentummm.app.widget.QuoteWidget
import com.momentummm.app.widget.YearProgressWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSetupScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Colores para los widgets de vida en semanas
    val livedWeeksColor by UserPreferencesRepository.getLivedWeeksColorFlow(context)
        .collectAsState(initial = "#4CAF50")
    val futureWeeksColor by UserPreferencesRepository.getFutureWeeksColorFlow(context)
        .collectAsState(initial = "#E0E0E0")
    
    // Lista de colores disponibles
    val availableColors = remember {
        listOf(
            ColorOption("#4CAF50", R.string.widget_color_green, Color(0xFF4CAF50)),
            ColorOption("#2196F3", R.string.widget_color_blue, Color(0xFF2196F3)),
            ColorOption("#9C27B0", R.string.widget_color_purple, Color(0xFF9C27B0)),
            ColorOption("#FF5722", R.string.widget_color_orange, Color(0xFFFF5722)),
            ColorOption("#E91E63", R.string.widget_color_pink, Color(0xFFE91E63)),
            ColorOption("#00BCD4", R.string.widget_color_cyan, Color(0xFF00BCD4)),
            ColorOption("#FFC107", R.string.widget_color_amber, Color(0xFFFFC107)),
            ColorOption("#795548", R.string.widget_color_brown, Color(0xFF795548)),
            ColorOption("#607D8B", R.string.widget_color_blue_gray, Color(0xFF607D8B)),
            ColorOption("#3F51B5", R.string.widget_color_indigo, Color(0xFF3F51B5)),
            ColorOption("#009688", R.string.widget_color_teal, Color(0xFF009688)),
            ColorOption("#F44336", R.string.widget_color_red, Color(0xFFF44336))
        )
    }
    
    val grayColors = remember {
        listOf(
            ColorOption("#E0E0E0", R.string.widget_color_light_gray, Color(0xFFE0E0E0)),
            ColorOption("#BDBDBD", R.string.widget_color_medium_gray, Color(0xFFBDBDBD)),
            ColorOption("#9E9E9E", R.string.widget_color_gray, Color(0xFF9E9E9E)),
            ColorOption("#757575", R.string.widget_color_dark_gray, Color(0xFF757575)),
            ColorOption("#F5F5F5", R.string.widget_color_almost_white, Color(0xFFF5F5F5)),
            ColorOption("#EEEEEE", R.string.widget_color_very_light_gray, Color(0xFFEEEEEE))
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.widget_settings_back_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información sobre widgets
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.widget_settings_available_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.widget_settings_available_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Widgets disponibles
            item {
                Text(
                    text = stringResource(R.string.widget_settings_available_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = stringResource(R.string.widget_settings_widget_life_weeks_title),
                    description = stringResource(R.string.widget_settings_widget_life_weeks_desc),
                    icon = Icons.Default.CalendarMonth
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = stringResource(R.string.widget_settings_widget_quote_title),
                    description = stringResource(R.string.widget_settings_widget_quote_desc),
                    icon = Icons.Default.FormatQuote
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = stringResource(R.string.widget_settings_widget_year_progress_title),
                    description = stringResource(R.string.widget_settings_widget_year_progress_desc),
                    icon = Icons.Default.TrendingUp
                )
            }
            
            // Personalización de colores - Semanas vividas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_settings_lived_color_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.widget_settings_lived_color_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableColors) { colorOption ->
                        ColorCircle(
                            colorOption = colorOption,
                            isSelected = livedWeeksColor == colorOption.hex,
                            onClick = {
                                coroutineScope.launch {
                                    UserPreferencesRepository.setWidgetColors(
                                        context,
                                        colorOption.hex,
                                        futureWeeksColor
                                    )
                                    // Actualizar widgets
                                    LifeWeeksWidget().updateAll(context)
                                }
                            }
                        )
                    }
                }
            }
            
            // Personalización de colores - Semanas futuras
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_settings_future_color_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.widget_settings_future_color_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(grayColors + availableColors) { colorOption ->
                        ColorCircle(
                            colorOption = colorOption,
                            isSelected = futureWeeksColor == colorOption.hex,
                            onClick = {
                                coroutineScope.launch {
                                    UserPreferencesRepository.setWidgetColors(
                                        context,
                                        livedWeeksColor,
                                        colorOption.hex
                                    )
                                    // Actualizar widgets
                                    LifeWeeksWidget().updateAll(context)
                                }
                            }
                        )
                    }
                }
            }
            
            // Botón para actualizar widgets manualmente
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            LifeWeeksWidget().updateAll(context)
                            QuoteWidget().updateAll(context)
                            YearProgressWidget().updateAll(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_settings_refresh_button))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class ColorOption(
    val hex: String,
    @StringRes val nameRes: Int,
    val color: Color
)

@Composable
private fun ColorCircle(
    colorOption: ColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colorOption.color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.widget_settings_selected_cd),
                tint = if (colorOption.color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Extensión para calcular luminancia
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

package com.momentummm.app.ui.screen.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.ui.theme.*
import com.momentummm.app.util.LifeExpectancy
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.util.LifeWeeksMath
import com.momentummm.app.widget.LifeWeeksGridBitmap
import com.momentummm.app.widget.LifeWeeksGridLayout
import com.momentummm.app.widget.LifeWeeksWidget
import com.momentummm.app.widget.LifeWeeksWidgetReceiver
import com.momentummm.app.widget.MotivationalMessageWidgetReceiver
import com.momentummm.app.widget.QuoteWidgetReceiver
import com.momentummm.app.widget.WidgetRefresher
import com.momentummm.app.widget.YearProgressWidgetReceiver
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Semanas de ejemplo para la vista previa si aún no hay fecha de nacimiento (30 años). */
private const val SAMPLE_WEEKS_LIVED = 1560

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSetupScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Colores que el widget pinta DE VERDAD: los elegidos aquí y, si no hay, los
    // de «Mi vida en semanas». Antes la pantalla marcaba el verde #4CAF50 como
    // elegido mientras el widget pintaba el índigo de la app.
    val userSettingsFlow = remember {
        (context.applicationContext as? MomentumApplication)?.userRepository?.getUserSettings()
            ?: flowOf(null)
    }
    val userSettings by userSettingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val widgetColors by remember { UserPreferencesRepository.widgetColorsFlow(context) }
        .collectAsStateWithLifecycle(initialValue = null to null)
    val livedWeeksColor = widgetColors.first ?: userSettings?.livedWeeksColor ?: LifeWeeksWidget.DEFAULT_LIVED_COLOR
    val futureWeeksColor = widgetColors.second ?: userSettings?.futureWeeksColor ?: LifeWeeksWidget.DEFAULT_FUTURE_COLOR
    val lifeExpectancy by remember { UserPreferencesRepository.getLifeExpectancyFlow(context) }
        .collectAsStateWithLifecycle(initialValue = LifeExpectancy.DEFAULT_YEARS)
    val lifeWeeks = remember(userSettings?.birthDate, lifeExpectancy) {
        userSettings?.birthDate?.let { LifeWeeksCalculator.calculateLifeWeeks(it, lifeExpectancy) }
    }

    val saveColors: (String, String) -> Unit = { lived, future ->
        coroutineScope.launch {
            UserPreferencesRepository.setWidgetColors(context, lived, future)
            LifeWeeksWidget.updateAllWidgets(context)
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { IntroCard() }

            items(WIDGETS) { widget ->
                val title = stringResource(widget.titleRes)
                WidgetPreviewCard(
                    title = title,
                    description = stringResource(widget.descriptionRes),
                    icon = widget.icon,
                    addDescription = stringResource(R.string.widget_settings_add_cd, title),
                    onAdd = { requestPin(context, widget.receiver) }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.widget_settings_preview_title),
                    subtitle = null
                )
            }
            item {
                LifeWeeksPreview(
                    weeksLived = lifeWeeks?.weeksLived ?: SAMPLE_WEEKS_LIVED,
                    totalWeeks = lifeWeeks?.totalWeeks,
                    livedColor = parseColor(livedWeeksColor),
                    futureColor = parseColor(futureWeeksColor)
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.widget_settings_lived_color_title),
                    subtitle = stringResource(R.string.widget_settings_lived_color_subtitle)
                )
            }
            item {
                ColorRow(
                    options = ACCENT_COLORS,
                    selectedHex = livedWeeksColor,
                    onSelect = { saveColors(it, futureWeeksColor) }
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.widget_settings_future_color_title),
                    subtitle = stringResource(R.string.widget_settings_future_color_subtitle)
                )
            }
            item {
                ColorRow(
                    options = GRAY_COLORS + ACCENT_COLORS,
                    selectedHex = futureWeeksColor,
                    onSelect = { saveColors(livedWeeksColor, it) }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.widget_settings_colors_shared_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            WidgetRefresher.refreshAll(context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.widget_settings_refreshed),
                                Toast.LENGTH_SHORT
                            ).show()
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/** Un widget de la lista, con el receptor que lo publica. */
private data class WidgetEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val receiver: Class<*>
)

// Antes faltaba el Mensaje motivacional: existía en el selector, pero no aquí.
private val WIDGETS = listOf(
    WidgetEntry(
        R.string.widget_settings_widget_life_weeks_title,
        R.string.widget_settings_widget_life_weeks_desc,
        Icons.Default.CalendarMonth,
        LifeWeeksWidgetReceiver::class.java
    ),
    WidgetEntry(
        R.string.widget_settings_widget_year_progress_title,
        R.string.widget_settings_widget_year_progress_desc,
        Icons.Default.TrendingUp,
        YearProgressWidgetReceiver::class.java
    ),
    WidgetEntry(
        R.string.widget_settings_widget_quote_title,
        R.string.widget_settings_widget_quote_desc,
        Icons.Default.FormatQuote,
        QuoteWidgetReceiver::class.java
    ),
    WidgetEntry(
        R.string.widget_settings_widget_motivational_title,
        R.string.widget_settings_widget_motivational_desc,
        Icons.Default.AutoAwesome,
        MotivationalMessageWidgetReceiver::class.java
    )
)

/**
 * Pide al launcher que ancle el widget. Si no lo admite, se explica cómo
 * añadirlo a mano en vez de no hacer nada.
 */
private fun requestPin(context: Context, receiver: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    val pinned = manager.isRequestPinAppWidgetSupported &&
        runCatching { manager.requestPinAppWidget(ComponentName(context, receiver), null, null) }
            .getOrDefault(false)
    if (!pinned) {
        Toast.makeText(context, context.getString(R.string.widget_settings_pin_unsupported), Toast.LENGTH_LONG).show()
    }
}

private fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Neutral500)

@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                modifier = Modifier.size(32.dp)
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    title: String,
    description: String,
    icon: ImageVector,
    addDescription: String,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
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
            TextButton(
                onClick = onAdd,
                modifier = Modifier.semantics { contentDescription = addDescription }
            ) {
                Text(stringResource(R.string.widget_settings_add_button))
            }
        }
    }
}

/**
 * Vista previa con el MISMO dibujo que usa el widget (LifeWeeksGridBitmap), no
 * con la cuadrícula de la app: así lo que se ve aquí es lo que saldrá en la
 * pantalla de inicio.
 */
@Composable
private fun LifeWeeksPreview(weeksLived: Int, totalWeeks: Int?, livedColor: Color, futureColor: Color) {
    val lifeYears = (totalWeeks ?: (LifeWeeksMath.DEFAULT_LIFE_EXPECTANCY * LifeWeeksMath.WEEKS_PER_ROW)) /
        LifeWeeksGridLayout.WEEKS_PER_YEAR
    val yearsLived = weeksLived / LifeWeeksGridLayout.WEEKS_PER_YEAR
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.widget_life_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PREVIEW_GRID_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val bitmap = remember(widthPx, heightPx, weeksLived, lifeYears, livedColor, futureColor) {
                    LifeWeeksGridLayout.choose(lifeYears, widthPx, heightPx)?.let { layout ->
                        LifeWeeksGridBitmap.render(
                            layout = layout,
                            lifeYears = lifeYears,
                            weeksLived = weeksLived,
                            yearsLived = yearsLived,
                            livedColor = livedColor.toArgb(),
                            futureColor = futureColor.toArgb()
                        ).asImageBitmap()
                    }
                }
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null)
                }
            }
        }
    }
}

private val PREVIEW_GRID_HEIGHT = 120.dp

/** Opción de color con su nombre traducido para lectores de pantalla. */
private data class ColorOption(val hex: String, @StringRes val nameRes: Int)

// #6366F1 y #E5E7EB son los colores por defecto de la app: antes no estaban en la
// lista y quien no había cambiado nada no veía ningún color marcado.
private val ACCENT_COLORS = listOf(
    ColorOption("#6366F1", R.string.widget_color_indigo),
    ColorOption("#4CAF50", R.string.widget_color_green),
    ColorOption("#2196F3", R.string.widget_color_blue),
    ColorOption("#9C27B0", R.string.widget_color_purple),
    ColorOption("#FF5722", R.string.widget_color_orange),
    ColorOption("#E91E63", R.string.widget_color_pink),
    ColorOption("#00BCD4", R.string.widget_color_cyan),
    ColorOption("#FFC107", R.string.widget_color_amber),
    ColorOption("#795548", R.string.widget_color_brown),
    ColorOption("#607D8B", R.string.widget_color_blue_gray),
    ColorOption("#009688", R.string.widget_color_teal),
    ColorOption("#F44336", R.string.widget_color_red)
)

private val GRAY_COLORS = listOf(
    ColorOption("#E5E7EB", R.string.widget_color_light_gray),
    ColorOption("#BDBDBD", R.string.widget_color_medium_gray),
    ColorOption("#9E9E9E", R.string.widget_color_gray),
    ColorOption("#757575", R.string.widget_color_dark_gray),
    ColorOption("#F5F5F5", R.string.widget_color_almost_white),
    ColorOption("#EEEEEE", R.string.widget_color_very_light_gray)
)

@Composable
private fun ColorRow(options: List<ColorOption>, selectedHex: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(options, key = { it.hex }) { option ->
            ColorCircle(
                color = parseColor(option.hex),
                name = stringResource(option.nameRes),
                isSelected = selectedHex.equals(option.hex, ignoreCase = true),
                onClick = { onSelect(option.hex) }
            )
        }
    }
}

@Composable
private fun ColorCircle(color: Color, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            // Antes el círculo no tenía nombre: TalkBack solo decía «botón».
            .semantics {
                contentDescription = name
                selected = isSelected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

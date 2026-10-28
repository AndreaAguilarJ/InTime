package com.momentummm.app.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.notification.NotificationRoutes
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.util.LifeWeeksData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

/**
 * Widget «Mi vida en semanas»: la cuadrícula completa de la vida, una celda por
 * semana como en la app, con las cifras debajo cuando hay sitio.
 */
class LifeWeeksWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_error) {

    // Exact: la cuadrícula se dibuja a la medida real del widget.
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        private const val TAG = "LifeWeeksWidget"
        private const val LOAD_TIMEOUT_MS = 3_000L

        /** Colores por defecto de la app (UserSettings). */
        const val DEFAULT_LIVED_COLOR = "#6366F1"
        const val DEFAULT_FUTURE_COLOR = "#E5E7EB"

        suspend fun updateAllWidgets(context: Context) {
            try {
                LifeWeeksWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ui = loadUi(context)
        provideContent { LifeWeeksContent(ui) }
    }

    private data class Stored(
        val birthDate: Date?,
        val livedHex: String?,
        val futureHex: String?,
        val lifeExpectancy: Int,
    )

    private suspend fun loadUi(context: Context): LifeWeeksUi {
        val stored = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            try {
                withContext(Dispatchers.IO) {
                    val settings = AppDatabase.getDatabase(context).userDao().getUserSettingsSync()
                    val (widgetLived, widgetFuture) = UserPreferencesRepository.getWidgetColors(context)
                    // Los colores elegidos en Ajustes > Widgets mandan; si no hay,
                    // los de «Mi vida en semanas», que es lo que la app pinta.
                    Stored(
                        birthDate = settings?.birthDate,
                        livedHex = widgetLived ?: settings?.livedWeeksColor,
                        futureHex = widgetFuture ?: settings?.futureWeeksColor,
                        lifeExpectancy = UserPreferencesRepository.getLifeExpectancy(context),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading widget data", e)
                null
            }
        }
        return LifeWeeksUi.build(
            context = WidgetLocale.localized(context),
            data = stored?.birthDate?.let { LifeWeeksCalculator.calculateLifeWeeks(it, stored.lifeExpectancy) },
            livedColor = parseColor(stored?.livedHex, DEFAULT_LIVED_COLOR),
            futureColor = parseColor(stored?.futureHex, DEFAULT_FUTURE_COLOR),
        )
    }

    private fun parseColor(hex: String?, fallback: String): Int =
        runCatching { android.graphics.Color.parseColor(hex ?: fallback) }
            .getOrElse { android.graphics.Color.parseColor(fallback) }

    @Composable
    private fun LifeWeeksContent(ui: LifeWeeksUi) {
        val context = LocalContext.current
        val open = WidgetActions.open(context, NotificationRoutes.MY_LIFE)
        val stats = ui.stats
        if (stats == null) {
            EmptyState(ui, open)
            return
        }

        val size = LocalSize.current
        val fontScale = WidgetTextRoom.fontScale(context)
        val showStats = LifeWeeksSizing.showStats(size, fontScale)
        val padding = if (showStats) WidgetDimens.padding else WidgetDimens.compactPadding

        WidgetSurface(onClick = open, padding = padding, verticalAlignment = Alignment.Top) {
            Header(ui, stats, LifeWeeksSizing.header(size.width, fontScale))
            Spacer(modifier = GlanceModifier.height(LifeWeeksSizing.HEADER_GAP_DP.dp))

            val gridWidthDp = size.width.value - padding.value * 2
            val gridHeightDp = size.height.value - padding.value * 2 -
                LifeWeeksSizing.reservedTextHeightDp(showStats, fontScale)
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Grid(context, ui, stats, gridWidthDp, gridHeightDp)
            }

            if (showStats) {
                Spacer(modifier = GlanceModifier.height(LifeWeeksSizing.STATS_GAP_DP.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetStat(
                        value = stats.livedValue,
                        label = stats.livedLabel,
                        legend = Color(ui.livedColor),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    WidgetStatDivider()
                    WidgetStat(
                        value = stats.leftValue,
                        label = stats.leftLabel,
                        legend = Color(ui.futureColor),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    WidgetStatDivider()
                    WidgetStat(
                        value = stats.ageValue,
                        label = stats.ageLabel,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }

    @Composable
    private fun Header(ui: LifeWeeksUi, stats: LifeWeeksUi.Stats, header: LifeWeeksSizing.Header) {
        if (header == LifeWeeksSizing.Header.PERCENT) {
            WidgetLabel(text = stats.percent)
            return
        }
        if (header == LifeWeeksSizing.Header.PERCENT_OF_LIFE) {
            // «29,7 % de tu vida» dice lo mismo que título + cifra y cabe.
            WidgetLabel(text = stats.percentOfLife)
            return
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = GlanceModifier.defaultWeight()) { WidgetLabel(text = ui.title) }
            Text(
                text = stats.percent,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun Grid(
        context: Context,
        ui: LifeWeeksUi,
        stats: LifeWeeksUi.Stats,
        widthDp: Float,
        heightDp: Float,
    ) {
        val density = context.resources.displayMetrics.density
        var widthPx = widthDp * density
        var heightPx = heightDp * density
        val scale = LifeWeeksGridBitmap.downscaleFor(widthPx, heightPx)
        widthPx *= scale
        heightPx *= scale
        val layout = LifeWeeksGridLayout.choose(ui.lifeYears, widthPx, heightPx) ?: return
        val bitmap = LifeWeeksGridBitmap.render(
            layout = layout,
            lifeYears = ui.lifeYears,
            weeksLived = stats.weeksLived,
            yearsLived = stats.yearsLived,
            livedColor = ui.livedColor,
            futureColor = ui.futureColor,
        )
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = stats.gridDescription,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }

    @Composable
    private fun EmptyState(ui: LifeWeeksUi, open: androidx.glance.action.Action) {
        WidgetSurface(onClick = open, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_event),
                contentDescription = null,
                modifier = GlanceModifier.size(28.dp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = ui.emptyTitle,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = ui.emptyBody,
                style = TextStyle(
                    color = WidgetColors.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        }
    }
}

/**
 * Qué cabe en «Mi vida en semanas». Los umbrales de texto están medidos con la
 * letra normal y se comparan con el hueco «en letra normal» ([WidgetTextRoom]).
 */
internal object LifeWeeksSizing {

    enum class Header { PERCENT, PERCENT_OF_LIFE, TITLE_AND_PERCENT }

    // Alturas, en dp con letra normal, que la cuadrícula descuenta del hueco.
    private const val HEADER_DP = 16f
    const val HEADER_GAP_DP = 6f
    private const val STATS_DP = 36f
    const val STATS_GAP_DP = 10f

    /**
     * Por debajo de esto no caben las tres cifras: cada columna necesita ~61 dp
     * («restantes» a 10 sp con su cuadradito), y a 170 dp le tocaban 46 y la
     * etiqueta se cortaba sin puntos suspensivos.
     */
    private val STATS_MIN = DpSize(220.dp, 170.dp)

    /** Título + porcentaje: «Minha vida em semanas» y «37,5 %» necesitan ~200 dp. */
    private val TITLE_MIN_WIDTH = 200.dp

    /** «37,5 % de votre vie» necesita ~130 dp; por debajo, solo la cifra. */
    private val PERCENT_OF_LIFE_MIN_WIDTH = 150.dp

    fun showStats(size: DpSize, fontScale: Float): Boolean {
        val room = WidgetTextRoom.of(size, fontScale)
        return room.width >= STATS_MIN.width && room.height >= STATS_MIN.height
    }

    fun header(width: Dp, fontScale: Float): Header {
        val room = WidgetTextRoom.of(DpSize(width, width), fontScale).width
        return when {
            room < PERCENT_OF_LIFE_MIN_WIDTH -> Header.PERCENT
            room < TITLE_MIN_WIDTH -> Header.PERCENT_OF_LIFE
            else -> Header.TITLE_AND_PERCENT
        }
    }

    /** Alto que ocupan cabecera y cifras, con su letra a la escala del sistema. */
    fun reservedTextHeightDp(showStats: Boolean, fontScale: Float): Float {
        val scale = fontScale.coerceIn(0.5f, 3f)
        return HEADER_DP * scale + HEADER_GAP_DP +
            if (showStats) STATS_DP * scale + STATS_GAP_DP else 0f
    }
}

/**
 * Todo lo que el widget pinta, ya traducido y formateado. Se construye fuera de
 * la composición, con el contexto en el idioma de la app.
 */
internal data class LifeWeeksUi(
    val title: String,
    val emptyTitle: String,
    val emptyBody: String,
    val lifeYears: Int,
    val livedColor: Int,
    val futureColor: Int,
    /** null si no hay fecha de nacimiento. */
    val stats: Stats?,
) {
    data class Stats(
        val weeksLived: Int,
        val yearsLived: Int,
        val percent: String,
        val percentOfLife: String,
        val livedValue: String,
        val livedLabel: String,
        val leftValue: String,
        val leftLabel: String,
        val ageValue: String,
        val ageLabel: String,
        val gridDescription: String,
    )

    companion object {
        fun build(
            context: Context,
            data: LifeWeeksData?,
            livedColor: Int,
            futureColor: Int,
        ): LifeWeeksUi {
            val res = context.resources
            val locale = WidgetLocale.locale(context)
            val lifeYears = (data?.totalWeeks ?: 0) / LifeWeeksGridLayout.WEEKS_PER_YEAR
            val stats = data?.let {
                val percent = WidgetFormat.percentOneDecimal(it.progressPercentage, locale)
                LifeWeeksUi.Stats(
                    weeksLived = it.weeksLived,
                    yearsLived = it.currentAge,
                    percent = percent,
                    percentOfLife = res.getString(R.string.widget_life_percent_of_life, percent),
                    livedValue = WidgetFormat.integer(it.weeksLived, locale),
                    livedLabel = res.getQuantityString(R.plurals.widget_life_weeks_lived, it.weeksLived),
                    leftValue = WidgetFormat.integer(it.weeksRemaining, locale),
                    leftLabel = res.getQuantityString(R.plurals.widget_life_weeks_left, it.weeksRemaining),
                    ageValue = WidgetFormat.integer(it.currentAge, locale),
                    ageLabel = res.getQuantityString(R.plurals.widget_life_years, it.currentAge),
                    gridDescription = res.getString(
                        R.string.widget_life_grid_cd,
                        WidgetFormat.integer(it.weeksLived, locale),
                        WidgetFormat.integer(it.totalWeeks, locale),
                    ),
                )
            }
            return LifeWeeksUi(
                title = res.getString(R.string.widget_life_title),
                emptyTitle = res.getString(R.string.widget_life_empty_title),
                emptyBody = res.getString(R.string.widget_life_empty_body),
                lifeYears = lifeYears,
                livedColor = livedColor,
                futureColor = futureColor,
                stats = stats,
            )
        }
    }
}

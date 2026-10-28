package com.momentummm.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.momentummm.app.R
import com.momentummm.app.notification.NotificationRoutes
import java.time.LocalDate

/** Widget «Progreso del año»: cuánto del año ha pasado y cuánto queda. */
class YearProgressWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_error) {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ui = YearProgressUi.build(WidgetLocale.localized(context), YearProgress.of(LocalDate.now()))
        provideContent { YearProgressContent(ui) }
    }

    @Composable
    private fun YearProgressContent(ui: YearProgressUi) {
        val context = LocalContext.current
        val sizing = YearProgressSizing.of(LocalSize.current, WidgetTextRoom.fontScale(context))
        val open = WidgetActions.open(context, NotificationRoutes.MY_LIFE)
        if (sizing.variant == YearProgressSizing.Variant.TINY) {
            TinyContent(ui, open)
            return
        }
        val showStats = sizing.showStats
        val roomy = sizing.variant == YearProgressSizing.Variant.ROOMY

        WidgetSurface(
            onClick = open,
            padding = if (roomy) WidgetDimens.padding else WidgetDimens.compactPadding,
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.defaultWeight()) { WidgetLabel(text = ui.year) }
                if (sizing.showDayOf) {
                    Text(
                        text = ui.dayOf,
                        style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(if (roomy) 6.dp else 2.dp))
            Text(
                text = ui.percent,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = if (roomy) 36.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = GlanceModifier.semantics { contentDescription = ui.description },
            )
            Spacer(modifier = GlanceModifier.height(if (roomy) 10.dp else 6.dp))
            LinearProgressIndicator(
                progress = ui.fraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = WidgetColors.accent,
                backgroundColor = WidgetColors.track,
            )
            if (showStats) {
                Spacer(modifier = GlanceModifier.height(14.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WidgetStat(
                        value = ui.passedValue,
                        label = ui.passedLabel,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    WidgetStatDivider()
                    WidgetStat(
                        value = ui.leftValue,
                        label = ui.leftLabel,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }

    /** Una sola fila de alto: año y porcentaje en una línea, y la barra debajo. */
    @Composable
    private fun TinyContent(ui: YearProgressUi, open: androidx.glance.action.Action) {
        WidgetSurface(onClick = open, padding = WidgetDimens.compactPadding) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.defaultWeight()) { WidgetLabel(text = ui.year, fontSize = 12.sp) }
                Text(
                    text = ui.percent,
                    style = TextStyle(
                        color = WidgetColors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.semantics { contentDescription = ui.description },
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = ui.fraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = WidgetColors.accent,
                backgroundColor = WidgetColors.track,
            )
        }
    }
}

/**
 * Qué variante de Progreso del año cabe. Los umbrales están medidos con la letra
 * normal y se comparan con el hueco «en letra normal» ([WidgetTextRoom]).
 */
internal object YearProgressSizing {

    enum class Variant { TINY, COMPACT, ROOMY }

    data class Sizing(val variant: Variant, val showStats: Boolean, val showDayOf: Boolean)

    private val TINY_BELOW_HEIGHT = 96.dp
    private val STATS_MIN_HEIGHT = 150.dp
    private val STATS_MIN_WIDTH = 140.dp
    private val ROOMY_MIN_HEIGHT = 130.dp
    /** «2026» + «Jour 300 sur 365» necesitan ~153 dp más el relleno. */
    private val DAY_OF_MIN_WIDTH = 170.dp

    fun of(size: DpSize, fontScale: Float): Sizing {
        val room = WidgetTextRoom.of(size, fontScale)
        val variant = when {
            room.height < TINY_BELOW_HEIGHT -> Variant.TINY
            room.height >= ROOMY_MIN_HEIGHT -> Variant.ROOMY
            else -> Variant.COMPACT
        }
        return Sizing(
            variant = variant,
            showStats = variant != Variant.TINY && room.height >= STATS_MIN_HEIGHT && room.width >= STATS_MIN_WIDTH,
            showDayOf = variant != Variant.TINY && room.width >= DAY_OF_MIN_WIDTH,
        )
    }
}

/** Textos del widget, ya traducidos y con el formato numérico del idioma. */
internal data class YearProgressUi(
    val year: String,
    val dayOf: String,
    val percent: String,
    val fraction: Float,
    val passedValue: String,
    val passedLabel: String,
    val leftValue: String,
    val leftLabel: String,
    val description: String,
) {
    companion object {
        fun build(context: Context, progress: YearProgress): YearProgressUi {
            val res = context.resources
            val locale = WidgetLocale.locale(context)
            val percent = WidgetFormat.wholePercent(progress.wholePercent, locale)
            // El año va sin separador de miles: «2026», no «2.026».
            val year = progress.year.toString()
            return YearProgressUi(
                year = year,
                dayOf = res.getString(
                    R.string.widget_year_day_of,
                    WidgetFormat.integer(progress.dayOfYear, locale),
                    WidgetFormat.integer(progress.daysInYear, locale),
                ),
                percent = percent,
                fraction = progress.fraction,
                passedValue = WidgetFormat.integer(progress.daysPassed, locale),
                passedLabel = res.getQuantityString(R.plurals.widget_year_days_passed, progress.daysPassed),
                leftValue = WidgetFormat.integer(progress.daysLeft, locale),
                leftLabel = res.getQuantityString(R.plurals.widget_year_days_left, progress.daysLeft),
                description = res.getString(R.string.widget_year_progress_cd, percent, year),
            )
        }
    }
}

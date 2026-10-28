package com.momentummm.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.notification.NotificationRoutes
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor

/**
 * Colores de los widgets. Son recursos (values / values-night) para que el
 * launcher los cambie con el modo oscuro del sistema sin volver a pintar.
 */
object WidgetColors {
    val onSurface = ColorProvider(R.color.widget_on_surface)
    val onSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
    val accent = ColorProvider(R.color.widget_accent)
    val track = ColorProvider(R.color.widget_track)
}

/** Medidas comunes, en dp, para que los cuatro widgets respiren igual. */
object WidgetDimens {
    val padding: Dp = 14.dp
    val compactPadding: Dp = 10.dp
}

/**
 * Hueco del widget medido en «letra normal»: los umbrales de cada variante se
 * midieron con la letra al 100 %, así que con la letra del sistema al 130 % un
 * widget de 260 dp tiene sitio para el texto de uno de 200.
 */
object WidgetTextRoom {
    fun of(size: DpSize, fontScale: Float): DpSize {
        val scale = fontScale.coerceIn(0.5f, 3f)
        return DpSize(size.width / scale, size.height / scale)
    }

    fun fontScale(context: Context): Float = context.resources.configuration.fontScale
}

/** Abrir la app desde un widget, en la pantalla que le corresponde. */
object WidgetActions {

    private const val ACTION_PREFIX = "com.momentummm.app.action.OPEN_FROM_WIDGET."

    /**
     * Antes cada widget construía el Intent con
     * `setClassName("com.momentummm.app", …)`: en la versión de depuración el
     * paquete es `com.momentummm.app.debug`, así que tocar el widget no abría
     * nada. Además todos llevaban a la pantalla de inicio. Ahora se usa la clase
     * de la actividad y el mismo mecanismo de destino que las notificaciones
     * (`PendingNavigation`), que espera a que pase el bloqueo de la app.
     */
    fun open(context: Context, destination: String): Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            // Una acción por destino: dos PendingIntents que solo difieren en un
            // extra serían el mismo para el sistema.
            action = ACTION_PREFIX + destination
            putExtra(NotificationRoutes.EXTRA_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return actionStartActivity(intent)
    }
}

/** Números y porcentajes con el formato del idioma (1.560 / 1,560, 29,7 %). */
object WidgetFormat {

    fun integer(value: Int, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).format(value)

    fun wholePercent(percent: Int, locale: Locale): String =
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
            .format(percent / 100.0)

    /** Un decimal, truncado: no se anuncia 30,0 % con 29,96 %. */
    fun percentOneDecimal(percent: Float, locale: Locale): String {
        val truncated = floor(percent * 10.0) / 10.0
        return NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(truncated / 100.0)
    }
}

/**
 * Contenedor raíz de todos los widgets: fondo redondeado que sigue el modo
 * oscuro, el identificador de fondo que usa el launcher para animar la apertura,
 * y el toque que abre la app.
 */
@Composable
fun WidgetSurface(
    onClick: Action,
    padding: Dp = WidgetDimens.padding,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_surface))
            .cornerRadius(R.dimen.widget_corner_radius)
            .padding(padding)
            .clickable(onClick),
        verticalAlignment = verticalAlignment,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** Etiqueta pequeña de cabecera («Frase del día», «2026»…). */
@Composable
fun WidgetLabel(text: String, color: ColorProvider = WidgetColors.accent, fontSize: TextUnit = 11.sp) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

/**
 * Cifra con su etiqueta debajo. [legend] es un cuadradito del color que esa
 * cifra tiene en la cuadrícula, para leer la leyenda sin buscarla.
 */
@Composable
fun WidgetStat(
    value: String,
    label: String,
    modifier: GlanceModifier = GlanceModifier,
    legend: Color? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (legend != null) {
                Box(modifier = GlanceModifier.size(7.dp).background(legend)) {}
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
            Text(
                text = label,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

/** Separador vertical entre cifras. */
@Composable
fun WidgetStatDivider() {
    Box(modifier = GlanceModifier.width(1.dp).height(24.dp).background(WidgetColors.track)) {}
}

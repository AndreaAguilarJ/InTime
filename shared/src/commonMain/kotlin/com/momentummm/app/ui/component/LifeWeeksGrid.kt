package com.momentummm.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.util.LifeWeeksMath

/**
 * Cuadrícula de «Mi vida en semanas»: una casilla por semana, una fila por año.
 *
 * Es Compose puro sobre Canvas, sin nada del sistema operativo, así que Android e
 * iOS dibujan exactamente la misma rejilla.
 *
 * @param totalWeeks tamaño real de la cuadrícula. Antes estaba fijado a 4160 (80
 *   años), de modo que quien configuraba otra expectativa de vida veía una
 *   cuadrícula que no correspondía con las cifras de su propio resumen.
 */
@Composable
fun LifeWeeksGrid(
    weeksLived: Int,
    livedColor: Color,
    futureColor: Color,
    totalWeeks: Int = LifeWeeksMath.DEFAULT_LIFE_EXPECTANCY * LifeWeeksMath.WEEKS_PER_ROW,
    modifier: Modifier = Modifier,
) {
    val rows = (totalWeeks + LifeWeeksMath.WEEKS_PER_ROW - 1) / LifeWeeksMath.WEEKS_PER_ROW
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(LifeWeeksMath.WEEKS_PER_ROW.toFloat() / rows.toFloat())
    ) {
        drawLifeWeeksGrid(
            weeksLived = weeksLived,
            totalWeeks = totalWeeks,
            rows = rows,
            livedColor = livedColor,
            futureColor = futureColor,
        )
    }
}

private fun DrawScope.drawLifeWeeksGrid(
    weeksLived: Int,
    totalWeeks: Int,
    rows: Int,
    livedColor: Color,
    futureColor: Color,
) {
    val cellWidth = size.width / LifeWeeksMath.WEEKS_PER_ROW
    val cellHeight = size.height / rows
    val padding = 1.dp.toPx()
    val cellSize = minOf(cellWidth - padding * 2, cellHeight - padding * 2)
    if (cellSize <= 0f) return

    for (week in 0 until totalWeeks) {
        val (row, column) = LifeWeeksMath.weekPosition(week)
        drawRect(
            color = if (week < weeksLived) livedColor else futureColor,
            topLeft = Offset(column * cellWidth + padding, row * cellHeight + padding),
            size = Size(cellSize, cellSize),
        )
    }
}

/**
 * Resumen numérico que acompaña a la cuadrícula: edad, semanas vividas y las que
 * quedan. Se comparte para que las dos plataformas presenten las mismas cifras con
 * la misma jerarquía visual.
 */
@Composable
fun LifeWeeksSummary(
    data: LifeWeeksMath.LifeWeeksData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LifeWeeksStat(value = data.currentAge.toString(), label = "Años")
        LifeWeeksStat(value = data.weeksLived.toString(), label = "Semanas vividas")
        LifeWeeksStat(value = data.weeksRemaining.toString(), label = "Semanas por vivir")
    }
}

@Composable
private fun LifeWeeksStat(value: String, label: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.momentum.textPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.momentum.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

package com.momentummm.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.gamification.ProgressStore
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.gam_coins_label
import com.momentummm.shared.resources.gam_level_short
import com.momentummm.shared.resources.gam_multiplier
import com.momentummm.shared.resources.gam_streak_days
import com.momentummm.shared.resources.gam_streak_label
import com.momentummm.shared.resources.gam_streak_one_day
import com.momentummm.shared.resources.gam_xp_to_next
import org.jetbrains.compose.resources.stringResource

/**
 * Cabecera de progreso: nivel, barra hacia el siguiente, monedas y racha.
 *
 * Muestra lo que FALTA para el siguiente nivel y no el total acumulado, porque es
 * la cifra que dice qué hacer a continuación. Ese fue el arreglo que hizo avanzar
 * la barra en la versión Android.
 */
@Composable
fun ProgressHeader(
    progress: ProgressStore.Progress,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.momentum.surfaceSunken)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.gam_level_short, progress.level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    Res.string.gam_xp_to_next,
                    progress.xpToNextLevel.toString(),
                    progress.level + 1,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.momentum.textSecondary,
            )
        }

        Spacer(Modifier.height(8.dp))
        XpBar(fraction = progress.levelProgress)

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatPair(
                label = stringResource(Res.string.gam_coins_label),
                value = progress.coins.toString(),
            )
            StatPair(
                label = stringResource(Res.string.gam_streak_label),
                value = when (progress.streakDays) {
                    0 -> "—"
                    1 -> stringResource(Res.string.gam_streak_one_day)
                    else -> stringResource(Res.string.gam_streak_days, progress.streakDays)
                },
                // El multiplicador solo se enseña cuando de verdad multiplica:
                // «Racha ×1» ocupa sitio sin decir nada.
                extra = if (progress.streakMultiplier > 1f) {
                    stringResource(
                        Res.string.gam_multiplier,
                        formatMultiplier(progress.streakMultiplier),
                    )
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun XpBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.momentum.border),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun StatPair(label: String, value: String, extra: String? = null) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.momentum.textTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.momentum.textPrimary,
        )
        if (extra != null) {
            Text(
                text = extra,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.momentum.success,
            )
        }
    }
}

/**
 * Multiplicador sin decimales inútiles: «×2» en vez de «×2.0», pero «×1,25»
 * conserva su parte fraccionaria porque ahí sí informa.
 */
private fun formatMultiplier(value: Float): String {
    val entero = value.toInt()
    if (value == entero.toFloat()) return entero.toString()
    // Dos decimales como máximo, sin ceros a la derecha.
    val centesimas = (value * 100).toInt()
    val texto = "${centesimas / 100}.${(centesimas % 100).toString().padStart(2, '0')}"
    return texto.trimEnd('0').trimEnd('.')
}

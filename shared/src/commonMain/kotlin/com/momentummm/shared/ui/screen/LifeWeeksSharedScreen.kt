package com.momentummm.shared.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.component.LifeWeeksGrid
import com.momentummm.app.ui.component.LifeWeeksSummary
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.util.LifeWeeksMath
import com.momentummm.shared.data.SettingsKeys
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.action_cancel
import com.momentummm.shared.resources.action_save
import com.momentummm.shared.resources.birth_date_title
import com.momentummm.shared.resources.life_change_birth
import com.momentummm.shared.resources.life_empty_hint
import com.momentummm.shared.resources.life_pick_birth
import com.momentummm.shared.resources.life_progress
import com.momentummm.shared.resources.life_title
import com.momentummm.shared.ui.component.DateSelectorDialog
import org.jetbrains.compose.resources.stringResource
import com.momentummm.shared.util.shortMonthNames
import com.momentummm.shared.util.todayLocalDate
import kotlinx.datetime.LocalDate

/**
 * «Mi vida en semanas», la pantalla compartida entre Android e iOS.
 *
 * Necesita un solo dato --la fecha de nacimiento-- y lo guarda en el almacén de
 * ajustes de cada sistema, así que sobrevive a cerrar la app. Mientras no exista,
 * en lugar de una cuadrícula vacía se pide la fecha, que es lo único que falta
 * para que la pantalla signifique algo.
 */
@Composable
fun LifeWeeksSharedScreen(
    store: SettingsStore,
    modifier: Modifier = Modifier,
) {
    var birthDate by remember { mutableStateOf(store.readBirthDate()) }
    var pickingDate by remember { mutableStateOf(false) }

    val today = todayLocalDate()
    val expectancy = store.getLongOrNull(SettingsKeys.LIFE_EXPECTANCY)?.toInt()
        ?: LifeWeeksMath.DEFAULT_LIFE_EXPECTANCY

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.life_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.momentum.textPrimary,
        )

        val fecha = birthDate
        if (fecha == null) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.life_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.momentum.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { pickingDate = true }) {
                Text(stringResource(Res.string.life_pick_birth))
            }
        } else {
            val data = LifeWeeksMath.calculate(fecha, today, expectancy)

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.life_progress, "${data.progressPercentage.toInt()} %"),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.momentum.textPrimary,
            )

            Spacer(Modifier.height(20.dp))
            LifeWeeksSummary(data = data)

            Spacer(Modifier.height(24.dp))
            LifeWeeksGrid(
                weeksLived = data.weeksLived,
                livedColor = MaterialTheme.momentum.weeksLived,
                futureColor = MaterialTheme.momentum.weeksFuture,
                totalWeeks = data.totalWeeks,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { pickingDate = true }) {
                Text(stringResource(Res.string.life_change_birth))
            }
        }
    }

    if (pickingDate) {
        DateSelectorDialog(
            // Sin fecha guardada se arranca en 1990: un punto de partida razonable
            // que deja el carril de años a pocos gestos de cualquier edad adulta.
            initial = birthDate ?: LocalDate(1990, 1, 1),
            minYear = today.year - 120,
            maxYear = today.year,
            title = stringResource(Res.string.birth_date_title),
            confirmLabel = stringResource(Res.string.action_save),
            dismissLabel = stringResource(Res.string.action_cancel),
            monthNames = remember { shortMonthNames() },
            onDismiss = { pickingDate = false },
            onConfirm = { elegida ->
                store.writeBirthDate(elegida)
                birthDate = elegida
                pickingDate = false
            },
        )
    }
}

private fun SettingsStore.readBirthDate(): LocalDate? =
    getStringOrNull(SettingsKeys.BIRTH_DATE)?.let { texto ->
        runCatching { LocalDate.parse(texto) }.getOrNull()
    }

private fun SettingsStore.writeBirthDate(date: LocalDate) {
    putString(SettingsKeys.BIRTH_DATE, date.toString())
}

package com.momentummm.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum
import kotlinx.datetime.LocalDate

/**
 * Selector de fecha propio, en tres carriles: año, mes y día.
 *
 * No se usa el `DatePicker` de Material a propósito, y no es una preferencia
 * estética: en iOS ese componente **cierra la app**. Internamente pide la zona
 * horaria del sistema a kotlinx-datetime, que en Kotlin/Native no resuelve los
 * alias de la base de zonas (`Europe/Amsterdam` es un alias) y lanza
 * `IllegalTimeZoneException`. Es un fallo conocido de la librería, corregido solo en
 * una versión que exige actualizar Kotlin y Compose en todo el proyecto.
 *
 * El cambio sale ganando además en uso: para una fecha de nacimiento, retroceder
 * treinta años mes a mes en una rejilla de calendario es incómodo, mientras que
 * elegir el año directamente son dos gestos.
 */
@Composable
fun DateSelectorDialog(
    initial: LocalDate,
    minYear: Int,
    maxYear: Int,
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    monthNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var year by remember { mutableStateOf(initial.year.coerceIn(minYear, maxYear)) }
    var month by remember { mutableStateOf(initial.monthNumber) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }

    // Al cambiar de mes o año el día elegido puede dejar de existir (31 de enero
    // -> febrero, o 29 de febrero en un año no bisiesto). Se recorta en vez de
    // construir una fecha inválida, que lanzaría excepción.
    val maxDay = daysInMonth(year, month)
    if (day > maxDay) day = maxDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.momentum.textPrimary,
            )
        },
        text = {
            Column {
                SelectorRow(
                    label = "Año",
                    options = (maxYear downTo minYear).toList(),
                    selected = year,
                    optionLabel = { it.toString() },
                    onSelect = { year = it },
                )
                Spacer(Modifier.height(12.dp))
                SelectorRow(
                    label = "Mes",
                    options = (1..12).toList(),
                    selected = month,
                    optionLabel = { monthNames.getOrElse(it - 1) { it.toString() } },
                    onSelect = { month = it },
                )
                Spacer(Modifier.height(12.dp))
                SelectorRow(
                    label = "Día",
                    options = (1..maxDay).toList(),
                    selected = day,
                    optionLabel = { it.toString() },
                    onSelect = { day = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalDate(year, month, day)) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@Composable
private fun <T> SelectorRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val state = rememberLazyListState()
    val selectedIndex = options.indexOf(selected)

    // El carril arranca centrado en el valor elegido: sin esto, abrir el selector
    // con el año 1990 dejaría la lista en 2026 y habría que arrastrar a ciegas.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            state.scrollToItem(maxOf(0, selectedIndex - 1))
        }
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.momentum.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(options) { option ->
                val isSelected = option == selected
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.momentum.textPrimary
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.momentum.surfaceSunken
                            }
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Días del mes, contando bien los febreros bisiestos. */
internal fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 31
}

internal fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

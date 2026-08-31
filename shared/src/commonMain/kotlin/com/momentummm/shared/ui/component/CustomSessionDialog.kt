package com.momentummm.shared.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.action_cancel
import com.momentummm.shared.resources.action_start
import com.momentummm.shared.resources.focus_break_label
import com.momentummm.shared.resources.focus_custom_title
import com.momentummm.shared.resources.focus_duration_label
import com.momentummm.shared.resources.focus_no_break
import org.jetbrains.compose.resources.stringResource

/** Duraciones de enfoque ofrecidas, en minutos. */
private val FOCUS_OPTIONS = listOf(1, 5, 10, 15, 20, 25, 30, 40, 45, 50, 60, 75, 90, 120)

/** Duraciones de descanso, incluida la opción de no tener descanso. */
private val BREAK_OPTIONS = listOf(0, 3, 5, 10, 15, 20)

/**
 * Sesión de enfoque a medida: se elige cuánto concentrarse y cuánto descansar.
 *
 * Los valores elegidos se recuerdan, igual que en la versión Android, para que
 * quien siempre trabaja en bloques de 40 minutos no tenga que repetir la elección
 * cada vez.
 */
@Composable
fun CustomSessionDialog(
    initialFocusMinutes: Int,
    initialBreakMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (focusMinutes: Int, breakMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sinDescanso = stringResource(Res.string.focus_no_break)
    var focusMinutes by remember { mutableStateOf(initialFocusMinutes) }
    var breakMinutes by remember { mutableStateOf(initialBreakMinutes) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.focus_custom_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.momentum.textPrimary,
            )
        },
        text = {
            Column {
                OptionSelectorRow(
                    label = stringResource(Res.string.focus_duration_label),
                    options = FOCUS_OPTIONS,
                    selected = focusMinutes,
                    optionLabel = { "$it" },
                    onSelect = { focusMinutes = it },
                )
                Spacer(Modifier.height(14.dp))
                OptionSelectorRow(
                    label = stringResource(Res.string.focus_break_label),
                    options = BREAK_OPTIONS,
                    selected = breakMinutes,
                    optionLabel = { if (it == 0) sinDescanso else "$it" },
                    onSelect = { breakMinutes = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(focusMinutes, breakMinutes) }) {
                Text(stringResource(Res.string.action_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

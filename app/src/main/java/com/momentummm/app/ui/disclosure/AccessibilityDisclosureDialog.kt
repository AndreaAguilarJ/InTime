package com.momentummm.app.ui.disclosure

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.momentummm.app.R

/**
 * Aviso destacado del servicio de accesibilidad.
 *
 * La política de la API AccessibilityService de Google Play exige que, ANTES
 * de llevar al usuario a activar un servicio de accesibilidad, la app muestre
 * en su propia interfaz qué datos lee el servicio, para qué los usa y si los
 * comparte, y que el usuario lo acepte con un toque explícito. Cerrar el
 * diálogo o salir de él no cuenta como consentimiento.
 *
 * La versión 1.3.0 (16) se rechazó por no tenerlo: los tres botones que piden
 * el permiso abrían directamente los ajustes de Android. Por eso
 * [openAccessibilitySettings] es privada y solo la llama el botón «Aceptar»;
 * `AccessibilityDisclosureGuardTest` falla si otro archivo vuelve a abrir
 * `ACTION_ACCESSIBILITY_SETTINGS`.
 *
 * Si cambia lo que leen `MomentumAccessibilityService` o `WebsiteBlockService`,
 * hay que cambiar estos textos (strings_accessibility_disclosure.xml).
 */
@Composable
fun AccessibilityDisclosureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        // Un toque fuera no debe cerrarlo sin querer: la persona tiene que
        // elegir una de las dos respuestas (atrás equivale a «No, gracias»).
        properties = DialogProperties(dismissOnClickOutside = false),
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text(stringResource(R.string.a11y_disclosure_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.a11y_disclosure_intro))
                DisclosureSection(
                    titleRes = R.string.a11y_disclosure_reads_title,
                    items = listOf(
                        R.string.a11y_disclosure_reads_app,
                        R.string.a11y_disclosure_reads_content,
                        R.string.a11y_disclosure_reads_url
                    )
                )
                DisclosureSection(
                    titleRes = R.string.a11y_disclosure_use_title,
                    items = listOf(
                        R.string.a11y_disclosure_use_rules,
                        R.string.a11y_disclosure_use_private
                    )
                )
                Text(stringResource(R.string.a11y_disclosure_control))
                Text(
                    text = stringResource(R.string.a11y_disclosure_next),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    openAccessibilitySettings(context)
                }
            ) {
                Text(stringResource(R.string.a11y_disclosure_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.a11y_disclosure_decline))
            }
        }
    )
}

@Composable
private fun DisclosureSection(@StringRes titleRes: Int, items: List<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(titleRes),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        items.forEach { itemRes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•")
                Text(stringResource(itemRes))
            }
        }
    }
}

/** Solo se llega aquí desde «Aceptar y continuar» del aviso. */
private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

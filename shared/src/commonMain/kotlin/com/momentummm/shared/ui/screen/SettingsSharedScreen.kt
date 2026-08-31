package com.momentummm.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.util.LifeWeeksMath
import com.momentummm.shared.data.SettingsKeys
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.data.ThemeChoice
import com.momentummm.shared.data.readThemeChoice
import com.momentummm.shared.data.writeThemeChoice
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.settings_appearance
import com.momentummm.shared.resources.settings_life_expectancy
import com.momentummm.shared.resources.settings_life_expectancy_hint
import com.momentummm.shared.resources.settings_theme_dark
import com.momentummm.shared.resources.settings_theme_light
import com.momentummm.shared.resources.settings_theme_system
import com.momentummm.shared.resources.settings_version
import com.momentummm.shared.resources.tab_settings
import com.momentummm.shared.ui.component.OptionSelectorRow
import org.jetbrains.compose.resources.stringResource

/** Expectativas ofrecidas, en años. */
private val LIFE_EXPECTANCY_OPTIONS = listOf(60, 65, 70, 75, 80, 85, 90, 95, 100, 110)

/**
 * Ajustes compartidos entre Android e iOS.
 *
 * Solo expone lo que de verdad cambia algo visible: la expectativa de vida que
 * dibuja la cuadrícula y la apariencia. Nada de interruptores decorativos.
 *
 * @param appVersion la versión la aporta cada plataforma; el módulo compartido no
 *   puede leerla porque vive en el paquete de la app.
 */
@Composable
fun SettingsSharedScreen(
    store: SettingsStore,
    appVersion: String,
    modifier: Modifier = Modifier,
    onThemeChange: (ThemeChoice) -> Unit = {},
) {
    var expectancy by remember {
        mutableStateOf(
            store.getLongOrNull(SettingsKeys.LIFE_EXPECTANCY)?.toInt()
                ?: LifeWeeksMath.DEFAULT_LIFE_EXPECTANCY
        )
    }
    var theme by remember { mutableStateOf(store.readThemeChoice()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(Res.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.momentum.textPrimary,
        )

        Spacer(Modifier.height(24.dp))
        SettingsCard {
            Text(
                text = stringResource(Res.string.settings_life_expectancy),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.momentum.textPrimary,
            )
            Text(
                text = stringResource(Res.string.settings_life_expectancy_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.momentum.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OptionSelectorRow(
                label = "",
                options = LIFE_EXPECTANCY_OPTIONS,
                selected = expectancy,
                optionLabel = { it.toString() },
                onSelect = { elegida ->
                    expectancy = elegida
                    store.putLong(SettingsKeys.LIFE_EXPECTANCY, elegida.toLong())
                },
            )
        }

        Spacer(Modifier.height(14.dp))
        SettingsCard {
            Text(
                text = stringResource(Res.string.settings_appearance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.momentum.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            ThemeChoice.entries.forEach { opcion ->
                ThemeRow(
                    label = when (opcion) {
                        ThemeChoice.SYSTEM -> stringResource(Res.string.settings_theme_system)
                        ThemeChoice.LIGHT -> stringResource(Res.string.settings_theme_light)
                        ThemeChoice.DARK -> stringResource(Res.string.settings_theme_dark)
                    },
                    selected = theme == opcion,
                    onClick = {
                        theme = opcion
                        store.writeThemeChoice(opcion)
                        onThemeChange(opcion)
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.settings_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textSecondary,
                )
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.momentum.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.momentum.textPrimary
            },
        )
        // La marca de selección es un punto y no un emoji ni un icono inventado:
        // el sistema de diseño usa color y peso para señalar el estado.
        if (selected) {
            Text(
                text = "●",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

package com.momentummm.shared.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum

/**
 * Rejilla de opciones que se ajusta al ancho, con una seleccionada.
 *
 * Sustituye al carril horizontal para las duraciones. El carril obligaba a
 * arrastrar para ver las opciones cortas --con 25 minutos preseleccionado, elegir
 * 1 minuto exigía cuatro arrastres a ciegas-- y en una pantalla estrecha nunca se
 * sabía cuántas quedaban fuera. Aquí se ven todas de una vez.
 *
 * Para el año de nacimiento se sigue usando el carril: ciento veinte años no caben
 * en una rejilla, y ahí desplazarse sí es el gesto natural.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> OptionSelectorGrid(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.momentum.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
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

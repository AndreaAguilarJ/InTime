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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum

/**
 * Carril horizontal de opciones con una seleccionada.
 *
 * Se usa para fechas y para duraciones. El carril arranca centrado en el valor
 * elegido: sin eso, abrir un selector con el año 1990 dejaría la lista en 2026 y
 * habría que arrastrar a ciegas.
 */
@Composable
fun <T> OptionSelectorRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val selectedIndex = options.indexOf(selected)

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            state.scrollToItem(maxOf(0, selectedIndex - 1))
        }
    }

    Column(modifier = modifier) {
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

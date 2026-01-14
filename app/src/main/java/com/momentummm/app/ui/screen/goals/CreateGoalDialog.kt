package com.momentummm.app.ui.screen.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.momentummm.app.ui.system.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onGoalSelected: (String) -> Unit,
    onCreateGoal: (String, String, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Mis Metas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Goals list
        // ... Aquí iría el código para mostrar la lista de metas ...

        // Add goal button
        MomentumButton(
            onClick = { showDialog = true },
            style = ButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar Meta")
        }
    }

    // Create goal dialog
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            MomentumCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nueva Meta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    }

                    // Title input
                    var title by remember { mutableStateOf("") }
                    var showError by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            showError = false
                        },
                        label = { Text("Título de la meta") },
                        placeholder = { Text("Ej: Reducir tiempo de pantalla") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && title.isBlank()
                    )

                    // Description input
                    var description by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            showError = false
                        },
                        label = { Text("Descripción") },
                        placeholder = { Text("Describe tu objetivo...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        isError = showError && description.isBlank()
                    )

                    // Target time input
                    var targetHours by remember { mutableStateOf("") }
                    var targetMinutes by remember { mutableStateOf("") }

                    Column {
                        Text(
                            text = "Tiempo objetivo",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = targetHours,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                        targetHours = it
                                        showError = false
                                    }
                                },
                                label = { Text("Horas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                isError = showError && targetHours.isBlank() && targetMinutes.isBlank()
                            )

                            OutlinedTextField(
                                value = targetMinutes,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                        targetMinutes = it
                                        showError = false
                                    }
                                },
                                label = { Text("Minutos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                isError = showError && targetHours.isBlank() && targetMinutes.isBlank()
                            )
                        }
                    }

                    // Period selection
                    var selectedPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }

                    Column {
                        Text(
                            text = "Período",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoalPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    label = { Text(period.displayName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Category selection
                    var selectedCategory by remember { mutableStateOf(GoalCategory.SCREEN_TIME) }

                    Column {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(120.dp)
                        ) {
                            items(GoalCategory.entries) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = {
                                        Text(
                                            text = category.displayName,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = category.color.copy(alpha = 0.2f),
                                        selectedLabelColor = category.color
                                    )
                                )
                            }
                        }
                    }

                    if (showError) {
                        Text(
                            text = "Por favor completa todos los campos obligatorios",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MomentumButton(
                            onClick = { showDialog = false },
                            style = ButtonStyle.Outline,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        MomentumButton(
                            onClick = {
                                if (title.isBlank() || description.isBlank() ||
                                    (targetHours.isBlank() && targetMinutes.isBlank())) {
                                    showError = true
                                    return@MomentumButton
                                }

                                val totalMinutes = (targetHours.toIntOrNull() ?: 0) * 60 +
                                                 (targetMinutes.toIntOrNull() ?: 0)

                                if (totalMinutes <= 0) {
                                    showError = true
                                    return@MomentumButton
                                }

                                onCreateGoal(
                                    title,
                                    description,
                                    totalMinutes,
                                    selectedPeriod.name,
                                    selectedCategory.name
                                )

                                showDialog = false
                            },
                            style = ButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Crear Meta")
                        }
                    }
                }
            }
        }
    }
}

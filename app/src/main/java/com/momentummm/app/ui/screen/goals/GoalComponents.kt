package com.momentummm.app.ui.screen.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.momentummm.app.R
import com.momentummm.app.ui.system.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreateGoal: (String, String, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }
    var selectedCategory by remember { mutableStateOf(GoalCategory.SCREEN_TIME) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        MomentumCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.goals_create_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.goals_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.goals_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Target value field
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) targetValue = it },
                    label = { Text(stringResource(R.string.goals_target_minutes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Period dropdown
                ExposedDropdownMenuBox(
                    expanded = showPeriodDropdown,
                    onExpandedChange = { showPeriodDropdown = !showPeriodDropdown }
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedPeriod.displayNameRes),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.goals_period_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPeriodDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false }
                    ) {
                        GoalPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(stringResource(period.displayNameRes)) },
                                onClick = {
                                    selectedPeriod = period
                                    showPeriodDropdown = false
                                }
                            )
                        }
                    }
                }

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedCategory.displayNameRes),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.goals_category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        GoalCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(stringResource(category.displayNameRes)) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MomentumButton(
                        onClick = onDismiss,
                        style = ButtonStyle.Outline,
                        modifier = Modifier.weight(1f)
                    ) {
                            Text(stringResource(R.string.goals_cancel_button))
                    }

                    MomentumButton(
                        onClick = {
                            if (title.isNotBlank() && targetValue.isNotBlank()) {
                                onCreateGoal(
                                    title,
                                    description,
                                    targetValue.toIntOrNull() ?: 0,
                                    selectedPeriod.name,
                                    selectedCategory.name
                                )
                            }
                        },
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && targetValue.isNotBlank()
                    ) {
                        Text(stringResource(R.string.goals_create_button))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            MomentumButton(
                onClick = onActionClick,
                style = ButtonStyle.Primary
            ) {
                Text(actionText)
            }
        }
    }
}

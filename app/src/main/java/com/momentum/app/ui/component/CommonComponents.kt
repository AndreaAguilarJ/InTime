package com.momentum.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf(selectedDate?.dayOfMonth?.toString() ?: "") }
    var selectedMonth by remember { mutableStateOf(selectedDate?.monthValue?.toString() ?: "") }
    var selectedYear by remember { mutableStateOf(selectedDate?.year?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar fecha de nacimiento") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Ingresa tu fecha de nacimiento:")
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedDay,
                        onValueChange = { 
                            if (it.length <= 2) selectedDay = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Día") },
                        placeholder = { Text("01") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = { 
                            if (it.length <= 2) selectedMonth = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Mes") },
                        placeholder = { Text("01") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = selectedYear,
                        onValueChange = { 
                            if (it.length <= 4) selectedYear = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Año") },
                        placeholder = { Text("1990") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val day = selectedDay.toIntOrNull() ?: 1
                        val month = selectedMonth.toIntOrNull() ?: 1
                        val year = selectedYear.toIntOrNull() ?: 1990
                        
                        if (day in 1..31 && month in 1..12 && year in 1900..LocalDate.now().year) {
                            val date = LocalDate.of(year, month, day)
                            onDateSelected(date)
                        }
                    } catch (e: Exception) {
                        // Invalid date, do nothing
                    }
                },
                enabled = selectedDay.isNotBlank() && selectedMonth.isNotBlank() && selectedYear.isNotBlank()
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        modifier = modifier
    )
}

@Composable
fun LoadingDialog(
    isVisible: Boolean,
    text: String = "Cargando...",
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cargando") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(text)
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}
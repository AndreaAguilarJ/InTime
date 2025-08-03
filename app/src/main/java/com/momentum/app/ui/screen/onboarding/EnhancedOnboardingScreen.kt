package com.momentum.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OnboardingStep(
    val title: String,
    val description: String,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedOnboardingScreen(
    onCompleted: () -> Unit,
    onBirthDateSelected: (LocalDate) -> Unit,
    onColorPreferencesSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBirthDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedLivedColor by remember { mutableStateOf("#FF6B6B") }
    var selectedFutureColor by remember { mutableStateOf("#E8E8E8") }
    
    val steps = listOf(
        OnboardingStep(
            title = "¡Bienvenido a Momentum!",
            description = "Tu vida en semanas te ayudará a visualizar el tiempo y vivir con más propósito."
        ) {
            WelcomeStepContent()
        },
        OnboardingStep(
            title = "Tu fecha de nacimiento",
            description = "Necesitamos tu fecha de nacimiento para calcular las semanas que has vivido."
        ) {
            BirthDateStepContent(
                selectedDate = selectedBirthDate,
                onDateSelected = { selectedBirthDate = it }
            )
        },
        OnboardingStep(
            title = "Personaliza tu visualización",
            description = "Elige los colores para las semanas vividas y futuras."
        ) {
            ColorCustomizationStepContent(
                livedColor = selectedLivedColor,
                futureColor = selectedFutureColor,
                onLivedColorChanged = { selectedLivedColor = it },
                onFutureColorChanged = { selectedFutureColor = it }
            )
        },
        OnboardingStep(
            title = "¡Todo listo!",
            description = "Ya puedes comenzar a usar Momentum para vivir cada semana con propósito."
        ) {
            CompletionStepContent()
        }
    )
    
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (pagerState.currentPage + 1).toFloat() / steps.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        
        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = steps[page]
            OnboardingStepScreen(
                step = step,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anterior")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(steps.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
            
            // Next/Complete button
            Button(
                onClick = {
                    if (pagerState.currentPage == steps.size - 1) {
                        // Complete onboarding
                        selectedBirthDate?.let { onBirthDateSelected(it) }
                        onColorPreferencesSelected(selectedLivedColor, selectedFutureColor)
                        onCompleted()
                    } else {
                        // Go to next step
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = when (pagerState.currentPage) {
                    1 -> selectedBirthDate != null // Birth date step
                    else -> true
                }
            ) {
                if (pagerState.currentPage == steps.size - 1) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completar")
                } else {
                    Text("Siguiente")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepScreen(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        step.content()
    }
}

@Composable
private fun WelcomeStepContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Momentum te ayuda a:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Visualizar tu vida en semanas")
            Text("• Controlar tu tiempo de pantalla")
            Text("• Recibir motivación diaria")
            Text("• Modo teléfono mínimo")
        }
    }
}

@Composable
private fun BirthDateStepContent(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📅",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedDate?.toString() ?: "Seleccionar fecha de nacimiento"
            )
        }
        
        if (selectedDate != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val age = LocalDate.now().year - selectedDate.year
            val weeksLived = age * 52
            Text(
                text = "Tienes aproximadamente $age años\nHas vivido ~$weeksLived semanas",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun ColorCustomizationStepContent(
    livedColor: String,
    futureColor: String,
    onLivedColorChanged: (String) -> Unit,
    onFutureColorChanged: (String) -> Unit
) {
    val colorOptions = listOf(
        "#FF6B6B" to "Rojo",
        "#4ECDC4" to "Turquesa",
        "#45B7D1" to "Azul",
        "#96CEB4" to "Verde"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎨",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Semanas vividas:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { (color, name) ->
                FilterChip(
                    selected = livedColor == color,
                    onClick = { onLivedColorChanged(color) },
                    label = { Text(name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Semanas futuras:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { (color, name) ->
                FilterChip(
                    selected = futureColor == color,
                    onClick = { onFutureColorChanged(color) },
                    label = { Text(name) }
                )
            }
        }
    }
}

@Composable
private fun CompletionStepContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Perfecto!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu configuración está lista. Ahora puedes comenzar a usar Momentum.",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar fecha de nacimiento") },
        text = {
            Text("Por favor selecciona tu fecha de nacimiento para calcular las semanas vividas.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // For demo purposes, using a sample date
                    onDateSelected(LocalDate.of(1990, 1, 1))
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
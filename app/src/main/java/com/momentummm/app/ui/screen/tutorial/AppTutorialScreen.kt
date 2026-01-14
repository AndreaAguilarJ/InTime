@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@file:Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")

package com.momentummm.app.ui.screen.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tips: List<String> = emptyList(),
    val content: (@Composable () -> Unit)? = null,
    val requiresConfirmation: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppTutorialScreen(
    onCompleted: () -> Unit,
    existingDobIso: String? = null,
    initialLivedColor: String? = null,
    initialFutureColor: String? = null,
    onBirthDateSelected: (LocalDate) -> Unit = {},
    onColorPreferencesSelected: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Estados para DOB y colores
    var selectedBirthDate by remember { mutableStateOf<LocalDate?>(existingDobIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var livedColor by remember { mutableStateOf(initialLivedColor ?: "#FF6B6B") }
    var futureColor by remember { mutableStateOf(initialFutureColor ?: "#E8E8E8") }

    // Construcción dinámica de pasos: incluye DOB solo si no existe
    val steps = buildList<TutorialStep> {
        add(
            TutorialStep(
                title = "Bienvenido al Tutorial",
                description = "Una guía rápida para configurar y personalizar Momentum según tus preferencias",
                icon = Icons.Default.Info,
                tips = listOf(
                    "Puedes acceder a este tutorial en cualquier momento desde Configuración",
                    "Todos los cambios se guardan automáticamente"
                )
            )
        )
        if (selectedBirthDate == null) {
            add(
                TutorialStep(
                    title = "Tu fecha de nacimiento",
                    description = "La usaremos para calcular semanas vividas y restantes en tu visualización de vida",
                    icon = Icons.Default.CalendarMonth,
                    content = {
                        BirthDateStepContent(
                            selectedDate = selectedBirthDate,
                            onDateSelected = { newDate ->
                                selectedBirthDate = newDate
                                // Guardar inmediatamente la DOB
                                onBirthDateSelected(newDate)
                            }
                        )
                    },
                    requiresConfirmation = true,
                    tips = listOf(
                        "Esta información se usa para calcular tu progreso de vida",
                        "Se actualiza automáticamente el widget y todas las visualizaciones"
                    )
                )
            )
        }
        add(
            TutorialStep(
                title = "Personaliza tus colores",
                description = "Elige colores para representar las semanas vividas y las semanas futuras",
                icon = Icons.Default.Palette,
                content = {
                    ColorCustomizationStepContent(
                        livedColor = livedColor,
                        futureColor = futureColor,
                        onLivedColorChanged = { livedColor = it },
                        onFutureColorChanged = { futureColor = it }
                    )
                },
                tips = listOf(
                    "Los colores se aplican inmediatamente a tu widget",
                    "Puedes cambiar estos colores más tarde desde Mi Vida"
                )
            )
        )
        add(
            TutorialStep(
                title = "¡Todo listo!",
                description = "Has configurado exitosamente Momentum. Explora las diferentes secciones para descubrir todas las funcionalidades.",
                icon = Icons.Default.CheckCircle,
                tips = listOf(
                    "Visita 'Mi Vida' para ver tu visualización completa",
                    "Usa 'Enfoque' para sesiones de productividad",
                    "Revisa 'Análisis' para estadísticas detalladas"
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Tutorial de Uso") },
            actions = {
                TextButton(onClick = onCompleted) { Text("Omitir") }
            }
        )

        LinearProgressIndicator(
            progress = (pagerState.currentPage + 1).toFloat() / steps.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = steps[page]
            if (step.content != null) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) { step.content.invoke() }
            } else {
                TutorialStepContent(
                    step = com.momentummm.app.ui.screen.tutorial.TutorialStep(
                        title = step.title,
                        description = step.description,
                        icon = step.icon,
                        tips = step.tips
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        TutorialNavigationBar(
            pagerState = pagerState,
            totalSteps = steps.size,
            onCompleted = {
                // Persistir preferencia de colores
                onColorPreferencesSelected(livedColor, futureColor)
                // Ya se guardó DOB al seleccionarla (si aplicaba)
                onCompleted()
            },
            isCurrentStepConfirmable = !steps[pagerState.currentPage].requiresConfirmation || selectedBirthDate != null
        )
    }
}

// Reutilización: contenido del paso básico
@Composable
private fun TutorialStepContent(
    step: com.momentummm.app.ui.screen.tutorial.TutorialStep,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Icon
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item {
            // Title
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            // Description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (step.tips.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(
                    text = "Consejos:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            items(step.tips) { tip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TutorialNavigationBar(
    pagerState: PagerState,
    totalSteps: Int,
    onCompleted: () -> Unit,
    isCurrentStepConfirmable: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pagerState.currentPage > 0) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        animateToPage(pagerState, pagerState.currentPage - 1)
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

        PageIndicators(
            total = totalSteps,
            current = pagerState.currentPage
        )

        if (pagerState.currentPage < totalSteps - 1) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        animateToPage(pagerState, pagerState.currentPage + 1)
                    }
                },
                enabled = isCurrentStepConfirmable
            ) {
                Text("Siguiente")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = onCompleted,
                enabled = isCurrentStepConfirmable
            ) {
                Text("¡Empezar!")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicators(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .run {
                        if (index == current) {
                            this.background(MaterialTheme.colorScheme.primary)
                        } else {
                            this.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "🎨", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Semanas vividas:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorOptions.forEach { (color, name) ->
                FilterChip(selected = livedColor == color, onClick = { onLivedColorChanged(color) }, label = { Text(name) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Semanas futuras:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorOptions.forEach { (color, name) ->
                FilterChip(selected = futureColor == color, onClick = { onFutureColorChanged(color) }, label = { Text(name) })
            }
        }
    }
}

@Composable
private fun BirthDateStepContent(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "📅", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = selectedDate?.toString() ?: "Seleccionar fecha de nacimiento")
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
        com.momentummm.app.ui.component.SimpleDatePicker(
            selectedDate = selectedDate,
            onDateSelected = { onDateSelected(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun animateToPage(pagerState: PagerState, page: Int) {
    pagerState.animateScrollToPage(page)
}

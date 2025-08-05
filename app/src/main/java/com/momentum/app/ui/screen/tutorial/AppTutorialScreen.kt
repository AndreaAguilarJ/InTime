package com.momentum.app.ui.screen.tutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentum.app.R
import kotlinx.coroutines.launch

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tips: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTutorialScreen(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tutorialSteps = listOf(
        TutorialStep(
            title = "Panel Principal - Hoy",
            description = "Monitorea tu tiempo de pantalla diario y ve tus apps más usadas",
            icon = Icons.Default.Home,
            tips = listOf(
                "Revisa tu tiempo de pantalla total del día",
                "Identifica qué aplicaciones usas más",
                "Lee la frase motivacional diaria",
                "Establece límites para apps específicas"
            )
        ),
        TutorialStep(
            title = "Mi Vida en Semanas",
            description = "Visualiza tu vida completa en una cuadrícula de 4,160 semanas",
            icon = Icons.Default.Person,
            tips = listOf(
                "Cada cuadrito representa una semana de vida",
                "Las semanas verdes son las que ya has vivido",
                "Las grises representan tu tiempo futuro",
                "Personaliza los colores según tus preferencias",
                "Guarda tu visualización como fondo de pantalla"
            )
        ),
        TutorialStep(
            title = "Análisis Avanzado",
            description = "Obtén insights detallados sobre tus patrones de uso",
            icon = Icons.Default.Analytics,
            tips = listOf(
                "Ve gráficos de uso semanal y mensual",
                "Analiza tendencias en tu tiempo de pantalla",
                "Compara diferentes períodos",
                "Exporta tus datos para análisis personal"
            )
        ),
        TutorialStep(
            title = "Sesiones de Enfoque",
            description = "Mejora tu productividad con sesiones de trabajo enfocado",
            icon = Icons.Default.Psychology,
            tips = listOf(
                "Inicia sesiones de enfoque cronometradas",
                "Bloquea apps distractoras temporalmente",
                "Sigue la técnica Pomodoro",
                "Revisa tus estadísticas de productividad"
            )
        ),
        TutorialStep(
            title = "Teléfono Mínimo",
            description = "Simplifica tu experiencia móvil para reducir distracciones",
            icon = Icons.Default.PhoneAndroid,
            tips = listOf(
                "Activa el modo teléfono mínimo",
                "Solo accede a apps esenciales",
                "Reduce notificaciones innecesarias",
                "Configura qué apps están permitidas"
            )
        ),
        TutorialStep(
            title = "Widgets de Pantalla",
            description = "Mantente motivado con widgets en tu pantalla de inicio",
            icon = Icons.Default.ViewModule,
            tips = listOf(
                "Agrega el widget 'Mi Vida en Semanas'",
                "Instala el widget de frases motivacionales",
                "Los widgets se actualizan automáticamente",
                "Redimensiona según tus necesidades"
            )
        ),
        TutorialStep(
            title = "Configuración y Personalización",
            description = "Ajusta la app según tus preferencias",
            icon = Icons.Default.Settings,
            tips = listOf(
                "Configura notificaciones diarias",
                "Personaliza temas y colores",
                "Establece límites de uso por app",
                "Sincroniza tus datos en la nube",
                "Exporta datos en CSV o PDF"
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = { 
                Text("Tutorial de Uso") 
            },
            actions = {
                TextButton(
                    onClick = onCompleted
                ) {
                    Text("Omitir")
                }
            }
        )

        // Progress indicator
        LinearProgressIndicator(
            progress = (pagerState.currentPage + 1).toFloat() / tutorialSteps.size,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )

        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = tutorialSteps[page]
            TutorialStepContent(
                step = step,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
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
                repeat(tutorialSteps.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .run {
                                if (index == pagerState.currentPage) {
                                    this.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    this.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                    )
                }
            }

            // Next/Finish button
            if (pagerState.currentPage < tutorialSteps.size - 1) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text("Siguiente")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    onClick = onCompleted
                ) {
                    Text("¡Empezar!")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun TutorialStepContent(
    step: TutorialStep,
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

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
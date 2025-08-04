package com.momentum.app.ui.screen.focus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentum.app.ui.system.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class FocusSession(
    val id: String,
    val name: String,
    val duration: Int, // in minutes
    val breakDuration: Int, // in minutes
    val blockedApps: List<String>,
    val isCustom: Boolean = false
)

data class SessionHistory(
    val id: String,
    val sessionName: String,
    val date: String,
    val duration: Int, // actual duration in minutes
    val plannedDuration: Int,
    val wasCompleted: Boolean,
    val distractions: Int
)

enum class SessionState {
    NOT_STARTED,
    RUNNING,
    BREAK,
    COMPLETED,
    PAUSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    isPremiumUser: Boolean,
    onUpgradeClick: () -> Unit
) {
    var selectedSession by remember { mutableStateOf<FocusSession?>(null) }
    var sessionState by remember { mutableStateOf(SessionState.NOT_STARTED) }
    var timeRemaining by remember { mutableStateOf(0) }
    var totalTime by remember { mutableStateOf(0) }
    
    val predefinedSessions = listOf(
        FocusSession("pomodoro", "Pomodoro Clásico", 25, 5, emptyList()),
        FocusSession("deep_work", "Trabajo Profundo", 90, 15, emptyList()),
        FocusSession("study", "Sesión de Estudio", 45, 10, emptyList()),
        FocusSession("creative", "Trabajo Creativo", 60, 10, emptyList()),
        FocusSession("meeting", "Preparación de Reunión", 30, 5, emptyList())
    )
    
    val sessionHistory = listOf(
        SessionHistory("1", "Pomodoro Clásico", "Hoy", 25, 25, true, 0),
        SessionHistory("2", "Trabajo Profundo", "Ayer", 85, 90, false, 3),
        SessionHistory("3", "Sesión de Estudio", "Ayer", 45, 45, true, 1),
    )
    
    if (!isPremiumUser) {
        PremiumFocusUpsellScreen(onUpgradeClick = onUpgradeClick)
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sesiones de Enfoque",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mejora tu concentración con sesiones estructuradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        if (selectedSession != null && sessionState != SessionState.NOT_STARTED) {
            item {
                // Active session display
                ActiveSessionCard(
                    session = selectedSession!!,
                    sessionState = sessionState,
                    timeRemaining = timeRemaining,
                    totalTime = totalTime,
                    onPause = { sessionState = SessionState.PAUSED },
                    onResume = { sessionState = SessionState.RUNNING },
                    onStop = { 
                        sessionState = SessionState.NOT_STARTED
                        selectedSession = null
                    }
                )
            }
        } else {
            item {
                // Session selector
                Text(
                    text = "Elige una Sesión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(predefinedSessions) { session ->
                SessionCard(
                    session = session,
                    onSelect = { 
                        selectedSession = session
                        totalTime = session.duration * 60 // Convert to seconds
                        timeRemaining = totalTime
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                // Custom session button
                MomentumCard(
                    onClick = { /* Open custom session creator */ },
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Crear Sesión Personalizada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configura tu propia duración y apps bloqueadas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        item {
            // Statistics
            SessionStatsCard(
                completedToday = 3,
                totalFocusTime = 115, // minutes
                streakDays = 7,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // History
            Text(
                text = "Historial Reciente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(sessionHistory) { history ->
            SessionHistoryCard(
                history = history,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Timer effect
    LaunchedEffect(sessionState, timeRemaining) {
        if (sessionState == SessionState.RUNNING && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        } else if (sessionState == SessionState.RUNNING && timeRemaining <= 0) {
            sessionState = SessionState.COMPLETED
        }
    }
}

@Composable
private fun PremiumFocusUpsellScreen(
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Psychology,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Sesiones de Enfoque",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Mejora tu productividad con sesiones estructuradas y bloqueo de aplicaciones",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val features = listOf(
            "Técnica Pomodoro y sesiones personalizadas",
            "Bloqueo automático de apps distractoras",
            "Estadísticas detalladas de productividad",
            "Recordatorios inteligentes",
            "Sesiones de trabajo profundo",
            "Seguimiento de rachas de enfoque"
        )
        
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MomentumButton(
            onClick = onUpgradeClick,
            style = ButtonStyle.Primary,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Actualizar a Premium")
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: FocusSession,
    sessionState: SessionState,
    timeRemaining: Int,
    totalTime: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    MomentumGradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (sessionState) {
                    SessionState.RUNNING -> "En progreso"
                    SessionState.BREAK -> "Descanso"
                    SessionState.PAUSED -> "Pausado"
                    SessionState.COMPLETED -> "¡Completado!"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Circular timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                val progress = if (totalTime > 0) {
                    (totalTime - timeRemaining).toFloat() / totalTime.toFloat()
                } else 0f
                
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(300),
                    label = "timer_progress"
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    // Background circle
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(strokeWidth)
                    )
                    
                    // Progress arc
                    drawArc(
                        color = if (sessionState == SessionState.RUNNING) Color.Green else Color.Orange,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val minutes = timeRemaining / 60
                    val seconds = timeRemaining % 60
                    
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (sessionState == SessionState.RUNNING) {
                        Text(
                            text = "restantes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (sessionState) {
                    SessionState.RUNNING -> {
                        MomentumButton(
                            onClick = onPause,
                            style = ButtonStyle.Secondary,
                            icon = Icons.Filled.Pause
                        ) {
                            Text("Pausar")
                        }
                    }
                    SessionState.PAUSED -> {
                        MomentumButton(
                            onClick = onResume,
                            style = ButtonStyle.Primary,
                            icon = Icons.Filled.PlayArrow
                        ) {
                            Text("Continuar")
                        }
                    }
                    SessionState.COMPLETED -> {
                        MomentumButton(
                            onClick = onStop,
                            style = ButtonStyle.Primary,
                            icon = Icons.Filled.Check
                        ) {
                            Text("Finalizar")
                        }
                    }
                    else -> {}
                }
                
                if (sessionState != SessionState.COMPLETED) {
                    MomentumButton(
                        onClick = onStop,
                        style = ButtonStyle.Outline,
                        icon = Icons.Filled.Stop
                    ) {
                        Text("Detener")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: FocusSession,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.duration}min de trabajo • ${session.breakDuration}min de descanso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.blockedApps.isNotEmpty()) {
                    Text(
                        text = "${session.blockedApps.size} apps bloqueadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionStatsCard(
    completedToday: Int,
    totalFocusTime: Int, // in minutes
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Estadísticas de Hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = completedToday.toString(),
                    label = "Sesiones\ncompletadas",
                    icon = Icons.Filled.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    value = "${totalFocusTime / 60}h ${totalFocusTime % 60}m",
                    label = "Tiempo\ntotal",
                    icon = Icons.Filled.Schedule,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    value = streakDays.toString(),
                    label = "Días\nconsecutivos",
                    icon = Icons.Filled.LocalFire,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionHistoryCard(
    history: SessionHistory,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (history.wasCompleted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (history.wasCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = history.sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${history.date} • ${history.duration}/${history.plannedDuration}min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (history.distractions > 0) {
                    Text(
                        text = "${history.distractions} distracciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Surface(
                color = if (history.wasCompleted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (history.wasCompleted) "Completado" else "Interrumpido",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (history.wasCompleted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
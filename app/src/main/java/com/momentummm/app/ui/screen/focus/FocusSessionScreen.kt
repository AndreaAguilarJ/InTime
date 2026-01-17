package com.momentummm.app.ui.screen.focus

import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentummm.app.ui.system.*
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.appwrite.models.AppwriteFocusSession
import com.momentummm.app.data.appwrite.models.FocusSessionStats
import com.momentummm.app.service.FocusTimerStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FocusSession(
    val id: String,
    val name: String,
    val duration: Int, // in minutes
    val breakDuration: Int, // in minutes
    val blockedApps: List<String>,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    @Suppress("UNUSED_PARAMETER") isPremiumUser: Boolean,
    @Suppress("UNUSED_PARAMETER") onUpgradeClick: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val focusRepository = application.appwriteFocusSessionRepository
    val currentUser = application.appwriteService.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val viewModel: FocusSessionViewModel = viewModel()
    val focusState by viewModel.sessionState.collectAsState()

    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var customSessions by remember { mutableStateOf<List<FocusSession>>(emptyList()) }
    var showDeleteConfirmation by remember { mutableStateOf<FocusSession?>(null) }
    var savedSessionKey by remember { mutableStateOf<String?>(null) }

    // Estados para estadísticas reales
    var focusStats by remember { mutableStateOf(FocusSessionStats()) }
    var sessionHistory by remember { mutableStateOf<List<AppwriteFocusSession>>(emptyList()) }
    var isLoadingStats by remember { mutableStateOf(true) }

    val predefinedSessions = listOf(
        FocusSession("pomodoro", "🍅 Pomodoro Clásico", 25, 5, emptyList()),
        FocusSession("deep_work", "🎯 Trabajo Profundo", 90, 15, emptyList()),
        FocusSession("study", "📚 Sesión de Estudio", 45, 10, emptyList()),
        FocusSession("creative", "🎨 Trabajo Creativo", 60, 10, emptyList()),
        FocusSession("meeting", "💼 Preparación de Reunión", 30, 5, emptyList()),
        FocusSession("quick", "⚡ Enfoque Rápido", 15, 3, emptyList())
    )

    val activeSession = if (focusState.status != FocusTimerStatus.IDLE && focusState.sessionType != null) {
        FocusSession(
            id = focusState.sessionType,
            name = focusState.sessionName ?: "Sesión de Enfoque",
            duration = (focusState.totalSeconds / 60).coerceAtLeast(1),
            breakDuration = focusState.breakMinutes,
            blockedApps = focusState.blockedApps
        )
    } else {
        null
    }

    LaunchedEffect(focusState.status, focusState.sessionType, focusState.remainingSeconds, focusState.totalSeconds) {
        if (focusState.status == FocusTimerStatus.RUNNING && focusState.remainingSeconds == focusState.totalSeconds) {
            savedSessionKey = null
        }
    }

    LaunchedEffect(focusState.status, focusState.sessionType) {
        val session = activeSession ?: return@LaunchedEffect
        if (focusState.status == FocusTimerStatus.COMPLETED && savedSessionKey != session.id) {
            saveCompletedSession(
                session = session,
                actualDuration = session.duration,
                wasCompleted = true,
                startTimeIso = focusState.startTimeIso
            )
            savedSessionKey = session.id
        }
    }

    // Cargar estadísticas y historial
    LaunchedEffect(currentUser.value) {
        currentUser.value?.let { user ->
            try {
                isLoadingStats = true
                focusRepository.getFocusSessionStats(user.id).collect { stats ->
                    focusStats = stats
                    isLoadingStats = false
                }
            } catch (_: Exception) {
                isLoadingStats = false
            }
        }
    }

    LaunchedEffect(currentUser.value) {
        currentUser.value?.let { user ->
            try {
                focusRepository.getFocusSessionHistory(user.id, 10).collect { history ->
                    sessionHistory = history
                }
            } catch (_: Exception) {
                // Manejar error silenciosamente
            }
        }
    }

    // Función para guardar sesión completada
    suspend fun saveCompletedSession(
        session: FocusSession,
        actualDuration: Int,
        wasCompleted: Boolean,
        startTimeIso: String?
    ) {
        currentUser.value?.let { user ->
            try {
                val sessionId = "sess_${System.currentTimeMillis()}"
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val focusSession = AppwriteFocusSession(
                    userId = user.id,
                    sessionId = sessionId,
                    sessionType = session.id,
                    date = currentDate,
                    startTime = startTimeIso,
                    endTime = currentTimestamp,
                    plannedDuration = session.duration,
                    actualDuration = actualDuration,
                    wasCompleted = wasCompleted,
                    distractions = 0,
                    blockedApps = session.blockedApps,
                    breakDuration = session.breakDuration
                )

                focusRepository.saveFocusSession(focusSession)
            } catch (_: Exception) {
                // Manejar error silenciosamente
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header mejorado
                MomentumCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Sesiones de Enfoque",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Maximiza tu productividad",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sesión activa con animación
            if (activeSession != null && focusState.status != FocusTimerStatus.IDLE) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        ActiveSessionCard(
                            session = activeSession,
                            sessionState = focusState.status,
                            timeRemaining = focusState.remainingSeconds,
                            totalTime = focusState.totalSeconds,
                            onPause = { viewModel.pauseSession() },
                            onResume = { viewModel.resumeSession() },
                            onStop = {
                                val elapsedMinutes = ((focusState.totalSeconds - focusState.remainingSeconds) / 60)
                                    .coerceAtLeast(0)
                                val currentSession = activeSession

                                if (currentSession != null && focusState.status != FocusTimerStatus.COMPLETED) {
                                    coroutineScope.launch {
                                        try {
                                            saveCompletedSession(
                                                session = currentSession,
                                                actualDuration = elapsedMinutes,
                                                wasCompleted = false,
                                                startTimeIso = focusState.startTimeIso
                                            )
                                            savedSessionKey = currentSession.id
                                        } catch (_: Exception) {
                                            // Manejar error silenciosamente
                                        }
                                    }
                                }

                                viewModel.stopSession()
                            }
                        )
                    }
                }
            } else {
                // Estadísticas primero cuando no hay sesión activa
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn()
                    ) {
                        if (isLoadingStats) {
                            MomentumCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            SessionStatsCard(
                                completedToday = focusStats.completedToday,
                                totalFocusTime = focusStats.totalFocusTimeToday,
                                streakDays = focusStats.streakDays,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sesiones Predefinidas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                items(predefinedSessions) { session ->
                    SessionCard(
                        session = session,
                        onSelect = {
                            viewModel.startSession(session)
                        },
                        onDelete = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Sesiones personalizadas
                if (customSessions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mis Sesiones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    items(customSessions) { session ->
                        SessionCard(
                            session = session,
                            onSelect = {
                                viewModel.startSession(session)
                            },
                            onDelete = { showDeleteConfirmation = session },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Text(
                        text = "Historial Reciente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(sessionHistory) { history ->
                    AppwriteSessionHistoryCard(
                        history = history,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (sessionHistory.isEmpty() && !isLoadingStats) {
                    item {
                        MomentumCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sin historial de sesiones",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completa tu primera sesión para ver las estadísticas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botón flotante para crear sesión personalizada
        if (focusState.status == FocusTimerStatus.IDLE) {
            FloatingActionButton(
                onClick = { showCreateSessionDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear sesión personalizada")
            }
        }
    }

    // Diálogo para crear sesión personalizada
    if (showCreateSessionDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateSessionDialog = false },
            onCreateSession = { newSession ->
                customSessions = customSessions + newSession
                showCreateSessionDialog = false
            }
        )
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Eliminar sesión") },
            text = { Text("¿Estás seguro de que deseas eliminar '${showDeleteConfirmation!!.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        customSessions = customSessions.filter { it.id != showDeleteConfirmation!!.id }
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

}

@Composable
private fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreateSession: (FocusSession) -> Unit
) {
    var sessionName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("25") }
    var breakDuration by remember { mutableStateOf("5") }
    var selectedEmoji by remember { mutableStateOf("⏱️") }

    val emojis = listOf("⏱️", "🎯", "📚", "💻", "🎨", "✍️", "🧘", "💡", "🚀", "⚡")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Crear Sesión Personalizada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector de emoji
                Text(
                    "Ícono",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        Surface(
                            onClick = { selectedEmoji = emoji },
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (selectedEmoji == emoji)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedEmoji == emoji)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Nombre de la sesión") },
                    placeholder = { Text("Ej: Escritura creativa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 3) duration = it },
                        label = { Text("Duración") },
                        suffix = { Text("min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = breakDuration,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) breakDuration = it },
                        label = { Text("Descanso") },
                        suffix = { Text("min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Crea sesiones adaptadas a tu estilo de trabajo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            MomentumButton(
                onClick = {
                    if (sessionName.isNotBlank() && duration.isNotBlank() && breakDuration.isNotBlank()) {
                        val newSession = FocusSession(
                            id = "custom_${System.currentTimeMillis()}",
                            name = "$selectedEmoji $sessionName",
                            duration = duration.toIntOrNull() ?: 25,
                            breakDuration = breakDuration.toIntOrNull() ?: 5,
                            blockedApps = emptyList(),
                            isCustom = true
                        )
                        onCreateSession(newSession)
                    }
                },
                style = ButtonStyle.Primary,
                enabled = sessionName.isNotBlank() && duration.isNotBlank() && breakDuration.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            MomentumButton(
                onClick = onDismiss,
                style = ButtonStyle.Outline
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ActiveSessionCard(
    session: FocusSession,
    sessionState: FocusTimerStatus,
    timeRemaining: Int,
    totalTime: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val progress = if (totalTime > 0) {
        (totalTime - timeRemaining).toFloat() / totalTime.toFloat()
    } else 0f

    MomentumGradientCard(
        modifier = Modifier.fillMaxWidth(),
        gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.surface
            )
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = when (sessionState) {
                    FocusTimerStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                    FocusTimerStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
                    FocusTimerStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = when (sessionState) {
                        FocusTimerStatus.RUNNING -> "🔥 En progreso"
                        FocusTimerStatus.BREAK -> "☕ Descanso"
                        FocusTimerStatus.PAUSED -> "⏸️ Pausado"
                        FocusTimerStatus.COMPLETED -> "✅ ¡Completado!"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                        .scale(if (sessionState == FocusTimerStatus.RUNNING) scale else 1f)
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(300),
                    label = "timer_progress"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Fondo del círculo
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.15f),
                        radius = radius,
                        center = center,
                        style = Stroke(strokeWidth)
                    )

                    // Progreso con gradiente
                    val progressColor = when (sessionState) {
                        FocusTimerStatus.RUNNING -> Color(0xFF4CAF50)
                        FocusTimerStatus.PAUSED -> Color(0xFFFF9800)
                        FocusTimerStatus.COMPLETED -> Color(0xFF2196F3)
                        else -> Color.Gray
                    }

                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                progressColor.copy(alpha = 0.7f),
                                progressColor,
                                progressColor.copy(alpha = 0.9f)
                            ),
                            center = center
                        ),
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
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (sessionState == FocusTimerStatus.RUNNING) {
                        Text(
                            text = "restantes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de progreso adicional
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .width(120.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when (sessionState) {
                            FocusTimerStatus.RUNNING -> Color(0xFF4CAF50)
                            FocusTimerStatus.PAUSED -> Color(0xFFFF9800)
                            FocusTimerStatus.COMPLETED -> Color(0xFF2196F3)
                            else -> Color.Gray
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Información adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${session.duration}min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Duración total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Completado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                when (sessionState) {
                    FocusTimerStatus.RUNNING -> {
                        MomentumButton(
                            onClick = onPause,
                            style = ButtonStyle.Secondary,
                            icon = Icons.Filled.Pause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pausar")
                        }
                    }
                    FocusTimerStatus.PAUSED -> {
                        MomentumButton(
                            onClick = onResume,
                            style = ButtonStyle.Primary,
                            icon = Icons.Filled.PlayArrow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continuar")
                        }
                    }
                    FocusTimerStatus.COMPLETED -> {
                        MomentumButton(
                            onClick = onStop,
                            style = ButtonStyle.Primary,
                            icon = Icons.Filled.Check,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finalizar Sesión")
                        }
                    }
                    else -> {}
                }

                if (sessionState != FocusTimerStatus.COMPLETED) {
                    MomentumButton(
                        onClick = onStop,
                        style = ButtonStyle.Outline,
                        icon = Icons.Filled.Stop,
                        modifier = Modifier.weight(1f)
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
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    MomentumCard(
        modifier = modifier
            .animateContentSize(),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⏱️ ${session.duration}min",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "☕ ${session.breakDuration}min",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionStatsCard(
    completedToday: Int,
    totalFocusTime: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    MomentumGradientCard(
        modifier = modifier,
        gradient = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tu Progreso de Hoy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                StatItem(
                    value = if (totalFocusTime >= 60) "${totalFocusTime / 60}h ${totalFocusTime % 60}m" else "${totalFocusTime}m",
                    label = "Tiempo\ntotal",
                    icon = Icons.Filled.Schedule,
                    color = MaterialTheme.colorScheme.secondary
                )

                Divider(
                    modifier = Modifier
                        .height(60.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                StatItem(
                    value = streakDays.toString(),
                    label = "Días\nconsecutivos",
                    icon = Icons.Filled.Whatshot,
                    color = Color(0xFFFF6F00)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}

@Composable
private fun AppwriteSessionHistoryCard(
    history: AppwriteFocusSession,
    modifier: Modifier = Modifier
) {
    val sessionName = when (history.sessionType) {
        "pomodoro" -> "🍅 Pomodoro Clásico"
        "deep_work" -> "🎯 Trabajo Profundo"
        "study" -> "📚 Sesión de Estudio"
        "creative" -> "🎨 Trabajo Creativo"
        "meeting" -> "💼 Preparación de Reunión"
        "quick" -> "⚡ Enfoque Rápido"
        else -> history.sessionType
    }

    MomentumCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (history.wasCompleted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (history.wasCompleted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (history.wasCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = history.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${history.actualDuration}/${history.plannedDuration}min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (history.distractions > 0) {
                    Text(
                        text = "⚠️ ${history.distractions} distracciones",
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (history.wasCompleted) "Completado" else "Interrumpido",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (history.wasCompleted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
import androidx.compose.foundation.gestures.detectTapGestures
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
// Necesarios para leer los textos desde recursos en vez de tenerlos escritos a mano.
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.momentummm.app.ui.accessibility.rememberSystemAnimationsEnabled
import com.momentummm.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.theme.MomentumTextStyles
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.appwrite.models.AppwriteFocusSession
import com.momentummm.app.data.appwrite.models.FocusSessionStats
import com.momentummm.app.service.FocusTimerStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.momentummm.app.ui.theme.*

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
    val currentUser = application.appwriteService.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val viewModel: FocusSessionViewModel = viewModel()
    val focusState by viewModel.sessionState.collectAsStateWithLifecycle()

    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var customSessions by remember { mutableStateOf<List<FocusSession>>(emptyList()) }
    var showDeleteConfirmation by remember { mutableStateOf<FocusSession?>(null) }
    var savedSessionKey by remember { mutableStateOf<String?>(null) }

    // Estados para estadísticas reales
    var focusStats by remember { mutableStateOf(FocusSessionStats()) }
    var sessionHistory by remember { mutableStateOf<List<AppwriteFocusSession>>(emptyList()) }
    var isLoadingStats by remember { mutableStateOf(true) }

    val predefinedSessions = listOf(
        FocusSession("pomodoro", stringResource(R.string.focus_preset_pomodoro), 25, 5, emptyList()),
        FocusSession("deep_work", stringResource(R.string.focus_preset_deep_work), 90, 15, emptyList()),
        FocusSession("study", stringResource(R.string.focus_preset_study), 45, 10, emptyList()),
        FocusSession("creative", stringResource(R.string.focus_preset_creative), 60, 10, emptyList()),
        FocusSession("meeting", stringResource(R.string.focus_preset_meeting), 30, 5, emptyList()),
        FocusSession("quick", stringResource(R.string.focus_preset_quick), 15, 3, emptyList())
    )

    val activeSession = if (focusState.status != FocusTimerStatus.IDLE) {
        focusState.sessionType?.let { sessionType ->
            FocusSession(
                id = sessionType,
                name = focusState.sessionName ?: stringResource(R.string.focus_screen_title),
                duration = (focusState.totalSeconds / 60).coerceAtLeast(1),
                breakDuration = focusState.breakMinutes,
                blockedApps = focusState.blockedApps
            )
        }
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
                        startTime = focusState.startTimeIso,
                        endTime = currentTimestamp,
                        plannedDuration = session.duration,
                        actualDuration = session.duration,
                        wasCompleted = true,
                        distractions = 0,
                        blockedApps = session.blockedApps,
                        breakDuration = session.breakDuration
                    )

                    focusRepository.saveFocusSession(focusSession)
                    savedSessionKey = session.id
                } catch (_: Exception) {
                    // Manejar error silenciosamente
                }
            }
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
                                        text = stringResource(R.string.focus_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.focus_subtitle),
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
                            text = stringResource(R.string.focus_presets),
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
                                text = stringResource(R.string.focus_my_sessions),
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
                        text = stringResource(R.string.focus_recent_history),
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
                                    text = stringResource(R.string.focus_history_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.focus_history_empty_message),
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_create_custom_session))
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

    // Diálogo de confirmación de eliminación - usar let para evitar NullPointerException
    showDeleteConfirmation?.let { sessionToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(stringResource(R.string.focus_delete_title)) },
            text = { Text(stringResource(R.string.focus_delete_message, sessionToDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        customSessions = customSessions.filter { it.id != sessionToDelete.id }
                        showDeleteConfirmation = null
                    }
                ) {
                    Text(stringResource(R.string.focus_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(stringResource(R.string.focus_cancel))
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
                stringResource(R.string.focus_create_custom),
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
                    stringResource(R.string.focus_icon),
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
                    label = { Text(stringResource(R.string.focus_session_name_label)) },
                    placeholder = { Text(stringResource(R.string.focus_session_name_placeholder)) },
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
                        label = { Text(stringResource(R.string.focus_duration)) },
                        suffix = { Text("min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = breakDuration,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) breakDuration = it },
                        label = { Text(stringResource(R.string.focus_break)) },
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
                            stringResource(R.string.focus_custom_hint),
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
                Text(stringResource(R.string.focus_create))
            }
        },
        dismissButton = {
            MomentumButton(
                onClick = onDismiss,
                style = ButtonStyle.Outline
            ) {
                Text(stringResource(R.string.focus_cancel))
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
    // El anillo respira sólo mientras corre el temporizador: en pausa o completado
    // el movimiento distraería en vez de informar.
    val breathing = rememberInfiniteTransition(label = "breathing")
    val breathScale by breathing.animateFloat(
        initialValue = 1f,
        targetValue = if (rememberSystemAnimationsEnabled()) 1.018f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val progress = if (totalTime > 0) {
        (totalTime - timeRemaining).toFloat() / totalTime.toFloat()
    } else 0f

    val stateColor = when (sessionState) {
        FocusTimerStatus.RUNNING -> MaterialTheme.momentum.success
        FocusTimerStatus.BREAK -> MaterialTheme.momentum.info
        FocusTimerStatus.PAUSED -> MaterialTheme.momentum.warning
        FocusTimerStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.momentum.textTertiary
    }

    val stateLabel = when (sessionState) {
        FocusTimerStatus.RUNNING -> stringResource(R.string.focus_status_running)
        FocusTimerStatus.BREAK -> stringResource(R.string.focus_status_break)
        FocusTimerStatus.PAUSED -> stringResource(R.string.focus_status_paused)
        FocusTimerStatus.COMPLETED -> stringResource(R.string.focus_status_done)
        else -> ""
    }

    val stateIcon = when (sessionState) {
        FocusTimerStatus.RUNNING -> Icons.Filled.Bolt
        FocusTimerStatus.BREAK -> Icons.Filled.Coffee
        FocusTimerStatus.PAUSED -> Icons.Filled.Pause
        FocusTimerStatus.COMPLETED -> Icons.Filled.Check
        else -> Icons.Filled.Timer
    }

    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MomentumDesign.Shapes.hero,
        containerColor = MaterialTheme.momentum.surfaceElevated,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            stateColor.copy(alpha = 0.16f),
                            stateColor.copy(alpha = 0.04f),
                            Color.Transparent,
                        )
                    )
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MomentumDesign.Spacing.cozy,
                        vertical = MomentumDesign.Spacing.large
                    )
            ) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.momentum.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.compact))

                // Pastilla de estado: icono vectorial en lugar de emoji, para que
                // herede el color del estado y escale con la densidad.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MomentumDesign.Shapes.pill)
                        .background(stateColor.copy(alpha = MomentumDesign.Alpha.soft))
                        .padding(
                            horizontal = MomentumDesign.Spacing.compact,
                            vertical = MomentumDesign.Spacing.small
                        )
                ) {
                    Icon(
                        imageVector = stateIcon,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(MomentumDesign.Size.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = stateColor
                    )
                }

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraLarge))

                ProgressRing(
                    progress = progress,
                    modifier = Modifier.scale(
                        if (sessionState == FocusTimerStatus.RUNNING) breathScale else 1f
                    ),
                    diameter = MomentumDesign.Size.ringHero,
                    strokeWidth = 16.dp,
                    trackColor = MaterialTheme.momentum.surfaceSunken,
                    brush = Brush.sweepGradient(
                        listOf(
                            stateColor.copy(alpha = 0.35f),
                            stateColor,
                            stateColor.copy(alpha = 0.35f),
                        )
                    ),
                    animate = false,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val minutes = timeRemaining / 60
                        val seconds = timeRemaining % 60

                        Text(
                            text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                            style = MomentumTextStyles.timer,
                            color = MaterialTheme.momentum.textPrimary
                        )
                        Text(
                            text = when (sessionState) {
                                FocusTimerStatus.COMPLETED -> stringResource(R.string.focus_session_over)
                                FocusTimerStatus.PAUSED -> stringResource(R.string.focus_paused_lower)
                                else -> stringResource(R.string.focus_remaining)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.momentum.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
                ) {
                    TimerMetric(
                        value = "${session.duration} min",
                        label = stringResource(R.string.focus_duration_label),
                        modifier = Modifier.weight(1f)
                    )
                    TimerMetric(
                        value = "${(progress * 100).toInt()}%",
                        label = stringResource(R.string.focus_completed_label),
                        accent = stateColor,
                        modifier = Modifier.weight(1f)
                    )
                    TimerMetric(
                        value = "${session.breakDuration} min",
                        label = stringResource(R.string.focus_status_break),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.large))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (sessionState) {
                        FocusTimerStatus.RUNNING, FocusTimerStatus.BREAK -> {
                            MomentumButton(
                                onClick = onPause,
                                style = ButtonStyle.Secondary,
                                size = ButtonSize.Large,
                                icon = Icons.Filled.Pause,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.focus_pause))
                            }
                        }
                        FocusTimerStatus.PAUSED -> {
                            MomentumButton(
                                onClick = onResume,
                                style = ButtonStyle.Primary,
                                size = ButtonSize.Large,
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.focus_resume))
                            }
                        }
                        FocusTimerStatus.COMPLETED -> {
                            MomentumButton(
                                onClick = onStop,
                                style = ButtonStyle.Primary,
                                size = ButtonSize.Large,
                                icon = Icons.Filled.Check,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.focus_finish))
                            }
                        }
                        else -> {}
                    }

                    if (sessionState != FocusTimerStatus.COMPLETED) {
                        LongPressStopButton(
                            onStop = onStop,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Métrica compacta bajo el anillo del temporizador. */
@Composable
private fun TimerMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.momentum.textPrimary,
) {
    Column(
        modifier = modifier
            .clip(MomentumDesign.Shapes.cardCompact)
            .background(MaterialTheme.momentum.surfaceSunken)
            .padding(vertical = MomentumDesign.Spacing.compact),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(MomentumDesign.Spacing.hairline))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.momentum.textSecondary
        )
    }
}

@Composable
private fun LongPressStopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "button_scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            val duration = 1000L // 1 segundo para completar
            val steps = 50
            val stepDuration = duration / steps
            
            repeat(steps) { step ->
                delay(stepDuration)
                progress = (step + 1).toFloat() / steps
                
                if (progress >= 1f) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStop()
                    isPressed = false
                    progress = 0f
                }
            }
        } else {
            progress = 0f
        }
    }
    
    Surface(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(48.dp)
        ) {
            // Progress background
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Rose400,
                            Rose600
                        )
                    ),
                    size = Size(size.width * progress, size.height)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = null,
                    tint = if (progress > 0.5f) Color.White else MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPressed) "Manteniendo..." else stringResource(R.string.focus_hold_to_stop),
                    color = if (progress > 0.5f) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
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
    val haptics = LocalHapticFeedback.current
    
    MomentumCard(
        modifier = modifier
            .animateContentSize(),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelect()
        }
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
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${session.duration}min",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
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
                        contentDescription = stringResource(R.string.a11y_delete),
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
                    text = stringResource(R.string.focus_today_progress),
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
                    label = stringResource(R.string.focus_sessions_done),
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
                    label = stringResource(R.string.focus_total_time),
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
                    label = stringResource(R.string.focus_streak_days),
                    icon = Icons.Filled.Whatshot,
                    color = Coral500
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
        "pomodoro" -> stringResource(R.string.focus_preset_pomodoro)
        "deep_work" -> stringResource(R.string.focus_preset_deep_work)
        "study" -> stringResource(R.string.focus_preset_study)
        "creative" -> stringResource(R.string.focus_preset_creative)
        "meeting" -> stringResource(R.string.focus_preset_meeting)
        "quick" -> stringResource(R.string.focus_preset_quick)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.momentum.danger,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = pluralStringResource(R.plurals.focus_distractions, history.distractions, history.distractions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.momentum.danger
                        )
                    }
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
                    text = if (history.wasCompleted) stringResource(R.string.focus_completed_label) else stringResource(R.string.focus_interrupted),
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

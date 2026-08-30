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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.momentummm.app.data.UserPreferencesRepository
import org.json.JSONArray
import org.json.JSONObject
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

    // Apps a bloquear durante el enfoque (persistente) y sesiones personalizadas
    // cargadas desde disco: antes ambas vivían solo en memoria y se perdían al salir.
    var blockList by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBlockPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        customSessions = decodeCustomSessions(
            withContext(Dispatchers.IO) { UserPreferencesRepository.getFocusCustomSessions(context) }
        )
        blockList = withContext(Dispatchers.IO) { UserPreferencesRepository.getFocusBlockList(context) }
    }

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
            // El guardado se lanza en el scope de la composición, NO en el del
            // efecto: al empezar el descanso el estado cambia de COMPLETED a
            // BREAK y eso cancelaría este LaunchedEffect a media escritura.
            savedSessionKey = session.id
            coroutineScope.launch {
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
                    } catch (_: Exception) {
                        // Manejar error silenciosamente
                    }
                }
            }
        }
    }

    // Cargar estadísticas y historial.
    // Si no hay usuario, no dejamos el indicador de carga encendido para siempre:
    // se resuelve a "sin datos" y la pantalla muestra los presets como acción principal.
    LaunchedEffect(currentUser.value) {
        val user = currentUser.value
        if (user == null) {
            focusStats = FocusSessionStats()
            isLoadingStats = false
            return@LaunchedEffect
        }
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
                .background(MaterialTheme.colorScheme.background),
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
                                IconTile(
                                    icon = Icons.Filled.Psychology,
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                            onBreak = focusState.onBreak,
                            onPause = { viewModel.pauseSession() },
                            onResume = { viewModel.resumeSession() },
                            onStartBreak = { viewModel.startBreak() },
                            onStop = {
                                val elapsedMinutes = ((focusState.totalSeconds - focusState.remainingSeconds) / 60)
                                    .coerceAtLeast(0)
                                val currentSession = activeSession

                                // Durante el DESCANSO no se guarda nada: el enfoque ya
                                // quedó registrado al completarse y guardar aquí crearía
                                // un segundo registro con los minutos del descanso.
                                if (currentSession != null &&
                                    focusState.status != FocusTimerStatus.COMPLETED &&
                                    focusState.status != FocusTimerStatus.BREAK &&
                                    savedSessionKey != currentSession.id
                                ) {
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
                // Con cero datos NO mostramos ceros crudos ni un anillo vacío: se
                // ocultan las estadísticas y los presets de abajo son la acción de inicio.
                val hasFocusStats = focusStats.completedToday > 0 ||
                    focusStats.totalFocusTimeToday > 0 ||
                    focusStats.streakDays > 0
                if (isLoadingStats || hasFocusStats) {
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
                                            .padding(MomentumDesign.Spacing.extraLarge),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
                }

                item {
                    BlockAppsRow(
                        count = blockList.size,
                        onClick = { showBlockPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.focus_presets),
                        modifier = Modifier.padding(vertical = MomentumDesign.Spacing.small)
                    )
                }

                items(predefinedSessions) { session ->
                    SessionCard(
                        session = session,
                        onSelect = {
                            viewModel.startSession(session.copy(blockedApps = blockList.toList()))
                        },
                        onDelete = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Sesiones personalizadas
                if (customSessions.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.focus_my_sessions),
                            modifier = Modifier.padding(vertical = MomentumDesign.Spacing.small)
                        )
                    }

                    items(customSessions) { session ->
                        SessionCard(
                            session = session,
                            onSelect = {
                                viewModel.startSession(session.copy(blockedApps = blockList.toList()))
                            },
                            onDelete = { showDeleteConfirmation = session },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.focus_recent_history),
                        modifier = Modifier.padding(vertical = MomentumDesign.Spacing.small)
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
                val updated = customSessions + newSession
                customSessions = updated
                coroutineScope.launch {
                    UserPreferencesRepository.setFocusCustomSessions(context, encodeCustomSessions(updated))
                }
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
                        val updated = customSessions.filter { it.id != sessionToDelete.id }
                        customSessions = updated
                        coroutineScope.launch {
                            UserPreferencesRepository.setFocusCustomSessions(context, encodeCustomSessions(updated))
                        }
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

    if (showBlockPicker) {
        BlockAppsPicker(
            initial = blockList,
            onDismiss = { showBlockPicker = false },
            onConfirm = { selected ->
                blockList = selected
                coroutineScope.launch { UserPreferencesRepository.setFocusBlockList(context, selected) }
                showBlockPicker = false
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
                    shape = MomentumDesign.Shapes.field
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
                            name = sessionName,
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
    onBreak: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStartBreak: () -> Unit,
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

                // Durante el descanso estas tres cifras describirían el enfoque
                // que ya terminó, así que se ocultan: la fase de descanso solo
                // necesita su cuenta atrás. Se usa `onBreak` y no el estado,
                // porque un descanso PAUSADO sigue siendo descanso.
                if (!onBreak) {
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
                }

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
                            // Con descanso configurado, la acción principal es
                            // empezarlo; terminar la sesión queda como secundaria.
                            if (session.breakDuration > 0) {
                                MomentumButton(
                                    onClick = onStartBreak,
                                    style = ButtonStyle.Primary,
                                    size = ButtonSize.Large,
                                    icon = Icons.Filled.Coffee,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.focus_start_break,
                                            session.breakDuration
                                        )
                                    )
                                }
                                MomentumButton(
                                    onClick = onStop,
                                    style = ButtonStyle.Outline,
                                    size = ButtonSize.Large,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.focus_finish))
                                }
                            } else {
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
                        }
                        else -> {}
                    }

                    // En el descanso no hace falta proteger la parada con una
                    // pulsación larga: el enfoque ya está guardado y saltarse un
                    // descanso no cuesta nada.
                    if (onBreak) {
                        MomentumButton(
                            onClick = onStop,
                            style = ButtonStyle.Outline,
                            size = ButtonSize.Large,
                            icon = Icons.Filled.SkipNext,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.focus_skip_break))
                        }
                    } else if (sessionState != FocusTimerStatus.COMPLETED) {
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
    val dangerColor = MaterialTheme.momentum.danger

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
        shape = MomentumDesign.Shapes.pill,
        color = dangerColor.copy(alpha = MomentumDesign.Alpha.soft),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = dangerColor.copy(alpha = 0.5f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(56.dp)
        ) {
            // Relleno de progreso mientras se mantiene pulsado.
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    color = dangerColor,
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
                    tint = if (progress > 0.5f) Color.White else dangerColor
                )
                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))
                Text(
                    text = if (isPressed) stringResource(R.string.focus_holding) else stringResource(R.string.focus_hold_to_stop),
                    color = if (progress > 0.5f) Color.White else dangerColor,
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
    MomentumCard(
        modifier = modifier.animateContentSize(),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomentumDesign.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = focusPresetIcon(session.id))

            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.momentum.textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.hairline))
                Text(
                    text = stringResource(
                        R.string.focus_card_meta,
                        session.duration,
                        session.breakDuration
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.momentum.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.a11y_delete),
                        tint = MaterialTheme.momentum.danger
                    )
                }
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MomentumDesign.Size.iconLarge)
                )
            }
        }
    }
}

/** Icono vectorial por tipo de preset. Los personalizados usan el temporizador. */
private fun focusPresetIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when (id) {
    "pomodoro" -> Icons.Filled.Timer
    "deep_work" -> Icons.Filled.Psychology
    "study" -> Icons.Filled.School
    "creative" -> Icons.Filled.Palette
    "meeting" -> Icons.Filled.Groups
    "quick" -> Icons.Filled.Bolt
    else -> Icons.Filled.Timer
}

@Composable
private fun SessionStatsCard(
    completedToday: Int,
    totalFocusTime: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    MomentumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.cozy)) {
            Text(
                text = stringResource(R.string.focus_today_progress),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.momentum.textPrimary
            )

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            // Las tres cifras viven en un solo contenedor separadas por divisores
            // de 1px, no en tarjetas sueltas ni con medallones de color.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    value = completedToday.toString(),
                    label = stringResource(R.string.focus_sessions_done),
                    icon = Icons.Filled.CheckCircle,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.momentum.border)
                )
                StatItem(
                    value = if (totalFocusTime >= 60) "${totalFocusTime / 60}h ${totalFocusTime % 60}m" else "${totalFocusTime}m",
                    label = stringResource(R.string.focus_total_time),
                    icon = Icons.Filled.Schedule,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.momentum.border)
                )
                StatItem(
                    value = streakDays.toString(),
                    label = stringResource(R.string.focus_streak_days),
                    icon = Icons.Filled.LocalFireDepartment,
                    accent = MaterialTheme.momentum.warning,
                    modifier = Modifier.weight(1f)
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
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = MomentumDesign.Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.momentum.textPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(MomentumDesign.Spacing.small))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.extraSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(MomentumDesign.Size.iconSmall)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.momentum.textSecondary,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
        }
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
                shape = MomentumDesign.Shapes.pill
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

// ─── Persistencia local de sesiones personalizadas (org.json, sin dependencias nuevas) ───

private fun encodeCustomSessions(sessions: List<FocusSession>): String {
    val array = JSONArray()
    sessions.forEach { s ->
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("name", s.name)
        obj.put("duration", s.duration)
        obj.put("breakDuration", s.breakDuration)
        obj.put("isCustom", s.isCustom)
        val apps = JSONArray()
        s.blockedApps.forEach { apps.put(it) }
        obj.put("blockedApps", apps)
        array.put(obj)
    }
    return array.toString()
}

private fun decodeCustomSessions(json: String): List<FocusSession> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val appsArray = obj.optJSONArray("blockedApps")
            val apps = if (appsArray != null) {
                (0 until appsArray.length()).mapNotNull { j -> appsArray.optString(j).takeIf { it.isNotEmpty() } }
            } else emptyList()
            FocusSession(
                id = obj.optString("id", "custom_${System.currentTimeMillis()}"),
                name = obj.optString("name"),
                duration = obj.optInt("duration", 25),
                breakDuration = obj.optInt("breakDuration", 5),
                blockedApps = apps,
                isCustom = obj.optBoolean("isCustom", true)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Fila de acceso al selector de apps a bloquear durante el enfoque. */
@Composable
private fun BlockAppsRow(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MomentumCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomentumDesign.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = Icons.Filled.Block, tint = MaterialTheme.momentum.danger)
            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.focus_block_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.momentum.textPrimary
                )
                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.hairline))
                Text(
                    text = if (count == 0) stringResource(R.string.focus_block_none)
                    else pluralStringResource(R.plurals.focus_block_count, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.momentum.textSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.momentum.textSecondary
            )
        }
    }
}

private data class InstalledAppItem(val packageName: String, val label: String)

private fun loadLaunchableApps(context: android.content.Context): List<InstalledAppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return try {
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            InstalledAppItem(pkg, ri.loadLabel(pm).toString())
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Selector de apps a bloquear (patrón Opal: icono real, un control, buscador, contador). */
@Composable
private fun BlockAppsPicker(
    initial: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppItem>?>(null) }
    var selected by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
            shape = MomentumDesign.Shapes.hero,
            color = MaterialTheme.momentum.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.momentum.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MomentumDesign.Spacing.cozy)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.focus_block_picker_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.momentum.textPrimary
                        )
                        Text(
                            text = if (selected.isEmpty()) stringResource(R.string.focus_block_none)
                            else pluralStringResource(R.plurals.focus_block_count, selected.size, selected.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.momentum.textSecondary
                        )
                    }
                    MomentumButton(
                        onClick = { onConfirm(selected) },
                        style = ButtonStyle.Primary,
                        size = ButtonSize.Small
                    ) {
                        Text(stringResource(R.string.focus_block_done))
                    }
                }

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.focus_block_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MomentumDesign.Shapes.field
                )

                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.compact))

                val list = apps
                when {
                    list == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    else -> {
                        val filtered = if (query.isBlank()) list
                        else list.filter { it.label.contains(query.trim(), ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.focus_block_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.momentum.textSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.extraSmall)
                            ) {
                                items(filtered, key = { it.packageName }) { app ->
                                    BlockAppRow(
                                        app = app,
                                        checked = selected.contains(app.packageName),
                                        onToggle = {
                                            selected = if (selected.contains(app.packageName))
                                                selected - app.packageName
                                            else selected + app.packageName
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockAppRow(
    app: InstalledAppItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap()
        } catch (_: Exception) {
            null
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MomentumDesign.Shapes.cardCompact)
            .clickable { onToggle() }
            .padding(
                horizontal = MomentumDesign.Spacing.small,
                vertical = MomentumDesign.Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MomentumDesign.Size.iconTile)
                .clip(MomentumDesign.Shapes.icon)
                .background(MaterialTheme.momentum.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    painter = BitmapPainter(icon.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MomentumDesign.Shapes.icon)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.momentum.textSecondary,
                    modifier = Modifier.size(MomentumDesign.Size.iconLarge)
                )
            }
        }
        Spacer(modifier = Modifier.width(MomentumDesign.Spacing.medium))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.momentum.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

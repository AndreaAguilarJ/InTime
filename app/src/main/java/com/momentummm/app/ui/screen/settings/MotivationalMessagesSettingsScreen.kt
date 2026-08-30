package com.momentummm.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.notification.MotivationalAlarmScheduler
import com.momentummm.app.ui.viewmodel.MotivationalMessagesViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationalMessagesSettingsScreen(
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: MotivationalMessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // El resultado del botón stringResource(R.string.motiv_test) viene del envío real, así que el mensaje
    // que ve el usuario dice lo que ha pasado de verdad.
    LaunchedEffect(uiState.testNotificationResult) {
        uiState.testNotificationResult?.let { result ->
            snackbarHostState.showSnackbar(result)
            viewModel.clearTestNotificationResult()
        }
    }

    // Al volver de los ajustes del sistema, revisar si ya hay alarmas exactas.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshExactAlarmPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.motiv_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = stringResource(R.string.a11y_message_library))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Card
                item {
                    MotivationalStatsCard(
                        messageOfTheDay = uiState.messageOfTheDay?.content ?: stringResource(R.string.motiv_default_message),
                        totalShown = uiState.totalMessagesShown,
                        totalLoved = uiState.totalLoved,
                        openRate = uiState.openRate
                    )
                }
                
                // Aviso de alarmas aproximadas.
                // En Android 14+ el permiso de alarmas exactas NO se concede por
                // defecto, así que los mensajes pueden llegar con retraso. Antes
                // esto pasaba en silencio.
                if (!uiState.canScheduleExactAlarms) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.motiv_late_warning_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.motiv_late_warning_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = {
                                    MotivationalAlarmScheduler.exactAlarmSettingsIntent(context)
                                        ?.let { intent ->
                                            runCatching { context.startActivity(intent) }
                                        }
                                }) {
                                    Text(stringResource(R.string.motiv_allow_exact_alarms))
                                }
                            }
                        }
                    }
                }

                // Personalización: cómo quiere el usuario que le hablen.
                item {
                    AnimatedVisibility(
                        visible = uiState.preferences.enabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.motiv_personalization), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.motiv_personalization_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = uiState.displayName,
                                    onValueChange = { viewModel.setDisplayName(it) },
                                    label = { Text(stringResource(R.string.motiv_name_question)) },
                                    placeholder = { Text(stringResource(R.string.motiv_name_placeholder)) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Main toggle
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.motiv_notifications_active), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.motiv_notifications_active_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.preferences.enabled,
                                onCheckedChange = { viewModel.toggleNotificationsEnabled(it) }
                            )
                        }
                    }
                }
                
                // Frequency settings
                item {
                    AnimatedVisibility(
                        visible = uiState.preferences.enabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.motiv_frequency), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                FrequencySlider(
                                    value = uiState.preferences.dailyFrequency,
                                    onValueChange = { viewModel.updateDailyFrequency(it) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TimeRangePicker(
                                    startHour = uiState.preferences.startHour,
                                    endHour = uiState.preferences.endHour,
                                    onStartHourChange = { viewModel.updateTimeRange(it, uiState.preferences.endHour) },
                                    onEndHourChange = { viewModel.updateTimeRange(uiState.preferences.startHour, it) }
                                )
                            }
                        }
                    }
                }
                
                // Categories
                item {
                    AnimatedVisibility(
                        visible = uiState.preferences.enabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.motiv_categories), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                CategorySelector(
                                    enabledCategories = uiState.preferences.getEnabledCategoriesList(),
                                    onToggleCategory = { viewModel.toggleCategory(it) }
                                )
                            }
                        }
                    }
                }
                
                // Tones
                item {
                    AnimatedVisibility(
                        visible = uiState.preferences.enabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.motiv_tone), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                ToneSelector(
                                    enabledTones = uiState.preferences.getEnabledTonesList(),
                                    onToggleTone = { viewModel.toggleTone(it) }
                                )
                            }
                        }
                    }
                }
                
                // Smart features
                item {
                    AnimatedVisibility(
                        visible = uiState.preferences.enabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.motiv_smart_features), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SettingSwitch(
                                    stringResource(R.string.motiv_smart_timing),
                                    stringResource(R.string.motiv_smart_timing_desc),
                                    uiState.preferences.smartTimingEnabled
                                ) { viewModel.toggleSmartTiming(it) }
                                
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                SettingSwitch(
                                    stringResource(R.string.motiv_weekend_schedule),
                                    stringResource(R.string.motiv_weekend_schedule_desc),
                                    uiState.preferences.differentWeekendSchedule
                                ) { viewModel.toggleWeekendSchedule(it) }
                                
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                SettingSwitch(
                                    stringResource(R.string.motiv_ai_messages),
                                    stringResource(R.string.motiv_ai_messages_desc),
                                    uiState.preferences.aiGenerationEnabled
                                ) { viewModel.toggleAIMessages(it) }
                            }
                        }
                    }
                }
                
                // Actions
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.motiv_actions), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // El aviso lo emite el LaunchedEffect con
                                        // el resultado real del envío, no un texto
                                        // fijo que afirmaba haberlo enviado siempre.
                                        viewModel.sendTestNotification()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.motiv_test))
                                }
                                Button(
                                    onClick = onOpenLibrary,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LibraryBooks, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.motiv_library))
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MotivationalStatsCard(
    messageOfTheDay: String,
    totalShown: Int,
    totalLoved: Int,
    openRate: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.motiv_message_of_day),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                messageOfTheDay,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("$totalShown", stringResource(R.string.motiv_stat_shown), "📬")
                StatItem("$totalLoved", stringResource(R.string.motiv_stat_loved), "❤️")
                StatItem("${(openRate * 100).toInt()}%", stringResource(R.string.motiv_stat_engagement), "📊")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FrequencySlider(value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.motiv_messages_per_day), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.motiv_messages_count, value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1", style = MaterialTheme.typography.labelSmall)
            Text("5", style = MaterialTheme.typography.labelSmall)
            Text("10", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TimeRangePicker(
    startHour: Int,
    endHour: Int,
    onStartHourChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.motiv_notification_hours), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeButton(stringResource(R.string.motiv_from), startHour) { showStartPicker = true }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TimeButton(stringResource(R.string.motiv_to), endHour) { showEndPicker = true }
        }
    }
    
    if (showStartPicker) {
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text(stringResource(R.string.limit_dialog_start_time_title)) },
            text = {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((6..12).toList()) { hour ->
                        FilterChip(
                            selected = hour == startHour,
                            onClick = {
                                onStartHourChange(hour)
                                showStartPicker = false
                            },
                            label = { Text("${hour}:00") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.motiv_close))
                }
            }
        )
    }
    
    if (showEndPicker) {
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text(stringResource(R.string.limit_dialog_end_time_title)) },
            text = {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((18..23).toList()) { hour ->
                        FilterChip(
                            selected = hour == endHour,
                            onClick = {
                                onEndHourChange(hour)
                                showEndPicker = false
                            },
                            label = { Text("${hour}:00") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.motiv_close))
                }
            }
        )
    }
}

@Composable
private fun TimeButton(label: String, hour: Int, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                String.format("%02d:00", hour),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    enabledCategories: List<MessageCategory>,
    onToggleCategory: (MessageCategory) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MessageCategory.entries.forEach { category ->
            val isEnabled = enabledCategories.contains(category)
            FilterChip(
                selected = isEnabled,
                onClick = { onToggleCategory(category) },
                label = { Text("${category.emoji} ${category.displayName}") },
                leadingIcon = if (isEnabled) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToneSelector(
    enabledTones: List<MessageTone>,
    onToggleTone: (MessageTone) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MessageTone.entries.forEach { tone ->
            val isEnabled = enabledTones.contains(tone)
            FilterChip(
                selected = isEnabled,
                onClick = { onToggleTone(tone) },
                label = { Text("${tone.emoji} ${tone.displayName}") },
                leadingIcon = if (isEnabled) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

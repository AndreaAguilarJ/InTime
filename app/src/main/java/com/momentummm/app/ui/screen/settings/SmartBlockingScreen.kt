package com.momentummm.app.ui.screen.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.R
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.service.FloatingTimerService
import com.momentummm.app.ui.system.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBlockingScreen(
    onBackClick: () -> Unit,
    viewModel: SmartBlockingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val contextRules by viewModel.contextRules.collectAsState()
    val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Dialogs
    var showSleepTimeDialog by remember { mutableStateOf(false) }
    var showFastingDialog by remember { mutableStateOf(false) }
    var showNuclearModeDialog by remember { mutableStateOf(false) }
    var showContextRuleDialog by remember { mutableStateOf(false) }
    var showGraceDaysDialog by remember { mutableStateOf(false) }
    var showFloatingTimerSettings by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // Mostrar mensaje de bienvenida la primera vez
    var showWelcomeCard by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Bloqueo Inteligente",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Ayuda")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === HEADER ===
            item {
                AnimatedVisibility(
                    visible = showWelcomeCard,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "🧠",
                                    fontSize = 48.sp
                                )
                                IconButton(onClick = { showWelcomeCard = false }) {
                                    Icon(Icons.Default.Close, "Cerrar", modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Control Avanzado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configura bloqueos inteligentes para mejorar tu productividad",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Quick stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                QuickStat(
                                    value = if (config.floatingTimerEnabled) "✓" else "✗",
                                    label = "Timer",
                                    isActive = config.floatingTimerEnabled
                                )
                                QuickStat(
                                    value = if (config.sleepModeEnabled) "✓" else "✗",
                                    label = "Sueño",
                                    isActive = config.sleepModeEnabled
                                )
                                QuickStat(
                                    value = if (config.digitalFastingEnabled) "✓" else "✗",
                                    label = "Ayuno",
                                    isActive = config.digitalFastingEnabled
                                )
                                QuickStat(
                                    value = if (config.isNuclearModeActive()) "🔴" else "⚫",
                                    label = "Nuclear",
                                    isActive = config.isNuclearModeActive()
                                )
                            }
                        }
                    }
                }
            }
            
            // === TIMER FLOTANTE ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Timer,
                    title = "⏱️ Timer Flotante",
                    subtitle = "Siempre visible sobre todas las apps",
                    isEnabled = config.floatingTimerEnabled,
                    onToggle = { viewModel.setFloatingTimerEnabled(it) },
                    extraContent = if (config.floatingTimerEnabled) {
                        {
                            if (!hasOverlayPermission.value) {
                                WarningCard(
                                    message = "Requiere permiso de superposición",
                                    actionLabel = "Conceder",
                                    onAction = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                        )
                                        context.startActivity(intent)
                                    }
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Opacidad: ${(config.floatingTimerOpacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { showFloatingTimerSettings = true }) {
                                        Text("Configurar")
                                    }
                                }
                                Slider(
                                    value = config.floatingTimerOpacity,
                                    onValueChange = { viewModel.setFloatingTimerOpacity(it) },
                                    valueRange = 0.3f..1f
                                )
                            }
                        }
                    } else null
                )
            }
            
            // === VENTANA DE SUEÑO ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Bedtime,
                    title = "😴 Ventana de Sueño",
                    subtitle = "No contar uso durante horas de sueño",
                    isEnabled = config.sleepModeEnabled,
                    onToggle = { viewModel.setSleepModeEnabled(it) },
                    extraContent = if (config.sleepModeEnabled) {
                        {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSleepTimeDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Horario de sueño",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${formatTime(config.sleepStartHour, config.sleepStartMinute)} - ${formatTime(config.sleepEndHour, config.sleepEndMinute)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ignorar tracking durante sueño",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = config.sleepModeIgnoreTracking,
                                    onCheckedChange = { viewModel.setSleepIgnoreTracking(it) }
                                )
                            }
                        }
                    } else null
                )
            }
            
            // === AYUNO INTERMITENTE DIGITAL ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Restaurant,
                    title = "🥗 Ayuno Digital",
                    subtitle = "Límites estrictos en horario laboral",
                    isEnabled = config.digitalFastingEnabled,
                    onToggle = { viewModel.setDigitalFastingEnabled(it) },
                    extraContent = if (config.digitalFastingEnabled) {
                        {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFastingDialog = true }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.WorkOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Horario de ayuno",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${formatTime(config.fastingStartHour, config.fastingStartMinute)} - ${formatTime(config.fastingEndHour, config.fastingEndMinute)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null
                                        )
                                    }
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Límite durante ayuno",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "${config.fastingDailyLimitMinutes} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Text(
                                        "Días: ${getDaysText(config.fastingDaysOfWeek)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else null
                )
            }
            
            // === MODO NUCLEAR ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Dangerous,
                    title = "☢️ Modo Nuclear",
                    subtitle = "Bloqueo extremo de 1-3 meses",
                    isEnabled = config.nuclearModeEnabled && config.isNuclearModeActive(),
                    onToggle = { 
                        if (it) showNuclearModeDialog = true 
                        else viewModel.deactivateNuclearMode()
                    },
                    accentColor = Color(0xFFEF4444),
                    extraContent = if (config.isNuclearModeActive()) {
                        {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("☢️", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "MODO NUCLEAR ACTIVO",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                            val remaining = viewModel.getNuclearModeRemainingDays()
                                            Text(
                                                "$remaining días restantes",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = 1f - (viewModel.getNuclearModeRemainingDays().toFloat() / config.nuclearModeDurationDays),
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFEF4444)
                                    )
                                    
                                    if (config.nuclearModeRequiresAppOpen) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "⚠️ El timer de desbloqueo solo avanza con InTime abierto",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else null
                )
            }
            
            // === PROTECCIÓN DE RACHAS ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "🔥 Protección de Rachas",
                    subtitle = "Días de gracia para no perder tu racha",
                    isEnabled = config.streakProtectionEnabled,
                    onToggle = { viewModel.setStreakProtectionEnabled(it) },
                    extraContent = if (config.streakProtectionEnabled) {
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Días de gracia por semana",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${config.graceDaysUsedThisWeek}/${config.graceDaysPerWeek} usados",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Row {
                                    (0..3).forEach { days ->
                                        FilterChip(
                                            selected = config.graceDaysPerWeek == days,
                                            onClick = { viewModel.setGraceDaysPerWeek(days) },
                                            label = { Text("$days") },
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Avisar antes de romper racha",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = config.warningBeforeStreakBreak,
                                    onCheckedChange = { viewModel.setWarningBeforeStreakBreak(it) }
                                )
                            }
                        }
                    } else null
                )
            }
            
            // === BLOQUEO POR CONTEXTO ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.LocationOn,
                    title = "📍 Bloqueo por Contexto",
                    subtitle = "Reglas por horario, ubicación o WiFi",
                    isEnabled = config.contextBlockingEnabled,
                    onToggle = { viewModel.setContextBlockingEnabled(it) },
                    extraContent = if (config.contextBlockingEnabled) {
                        {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                contextRules.forEach { rule ->
                                    ContextRuleCard(
                                        rule = rule,
                                        onToggle = { viewModel.toggleContextRule(rule.id, it) },
                                        onDelete = { viewModel.deleteContextRule(rule.id) }
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = { showContextRuleDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Agregar regla de contexto")
                                }
                            }
                        }
                    } else null
                )
            }
            
            // === MODO SOLO COMUNICACIÓN ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Chat,
                    title = "💬 Modo Solo Comunicación",
                    subtitle = "Permite mensajes, bloquea feeds y reels",
                    isEnabled = config.communicationOnlyModeEnabled,
                    onToggle = { viewModel.setCommunicationOnlyMode(it) },
                    extraContent = if (config.communicationOnlyModeEnabled) {
                        {
                            Column {
                                Text(
                                    "Apps en modo comunicación:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val apps = listOf(
                                    "Instagram" to "com.instagram.android",
                                    "Facebook" to "com.facebook.katana",
                                    "Twitter/X" to "com.twitter.android",
                                    "TikTok" to "com.zhiliaoapp.musically"
                                )
                                
                                apps.forEach { (name, packageName) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, style = MaterialTheme.typography.bodyMedium)
                                        Switch(
                                            checked = config.communicationOnlyApps.contains(packageName),
                                            onCheckedChange = { 
                                                viewModel.toggleCommunicationOnlyApp(packageName, it)
                                            }
                                        )
                                    }
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                Text(
                                    "Configuración de bloqueo:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                SwitchRow("Permitir DMs/Mensajes", config.communicationOnlyAllowDMs) {
                                    viewModel.setCommunicationOnlyAllowDMs(it)
                                }
                                SwitchRow("Bloquear Feed", config.communicationOnlyBlockFeed) {
                                    viewModel.setCommunicationOnlyBlockFeed(it)
                                }
                                SwitchRow("Bloquear Stories", config.communicationOnlyBlockStories) {
                                    viewModel.setCommunicationOnlyBlockStories(it)
                                }
                                SwitchRow("Bloquear Reels/Shorts", config.communicationOnlyBlockReels) {
                                    viewModel.setCommunicationOnlyBlockReels(it)
                                }
                            }
                        }
                    } else null
                )
            }
            
            // Espacio al final
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
    
    // === DIALOGS ===
    
    if (showSleepTimeDialog) {
        SleepTimeDialog(
            currentStartHour = config.sleepStartHour,
            currentStartMinute = config.sleepStartMinute,
            currentEndHour = config.sleepEndHour,
            currentEndMinute = config.sleepEndMinute,
            onDismiss = { showSleepTimeDialog = false },
            onConfirm = { startH, startM, endH, endM ->
                viewModel.setSleepSchedule(startH, startM, endH, endM)
                showSleepTimeDialog = false
            }
        )
    }
    
    if (showFastingDialog) {
        FastingScheduleDialog(
            config = config,
            onDismiss = { showFastingDialog = false },
            onConfirm = { startH, startM, endH, endM, limit, days ->
                viewModel.setFastingSchedule(startH, startM, endH, endM, limit, days)
                showFastingDialog = false
            }
        )
    }
    
    if (showNuclearModeDialog) {
        NuclearModeDialog(
            onDismiss = { showNuclearModeDialog = false },
            onConfirm = { days, apps, waitMinutes ->
                viewModel.activateNuclearMode(days, apps, waitMinutes)
                showNuclearModeDialog = false
            }
        )
    }
    
    if (showContextRuleDialog) {
        ContextRuleDialog(
            onDismiss = { showContextRuleDialog = false },
            onConfirm = { rule ->
                viewModel.addContextRule(rule)
                showContextRuleDialog = false
            }
        )
    }
    
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
private fun SmartBlockingSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                accentColor.copy(alpha = 0.05f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f)
                    )
                )
            }
            
            AnimatedVisibility(
                visible = isEnabled && extraContent != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    extraContent?.invoke()
                }
            }
        }
    }
}

@Composable
private fun ContextRuleCard(
    rule: ContextBlockRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.ruleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${rule.getDaysAsText()} • ${formatTime(rule.scheduleStartHour, rule.scheduleStartMinute)} - ${formatTime(rule.scheduleEndHour, rule.scheduleEndMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Límite: ${rule.contextDailyLimitMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WarningCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

// === DIALOGS ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimeDialog(
    currentStartHour: Int,
    currentStartMinute: Int,
    currentEndHour: Int,
    currentEndMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int) -> Unit
) {
    var startHour by remember { mutableStateOf(currentStartHour) }
    var startMinute by remember { mutableStateOf(currentStartMinute) }
    var endHour by remember { mutableStateOf(currentEndHour) }
    var endMinute by remember { mutableStateOf(currentEndMinute) }
    var editingStart by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Horario de Sueño") },
        text = {
            Column {
                Text("El uso de apps durante estas horas no se contará")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerButton(
                        label = "Inicio",
                        hour = startHour,
                        minute = startMinute,
                        isSelected = editingStart,
                        onClick = { editingStart = true }
                    )
                    TimePickerButton(
                        label = "Fin",
                        hour = endHour,
                        minute = endMinute,
                        isSelected = !editingStart,
                        onClick = { editingStart = false }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horas predefinidas comunes
                val presets = listOf(
                    "22:00 - 06:00" to listOf(22, 0, 6, 0),
                    "23:00 - 07:00" to listOf(23, 0, 7, 0),
                    "00:00 - 08:00" to listOf(0, 0, 8, 0)
                )
                
                presets.forEach { (label, times) ->
                    OutlinedButton(
                        onClick = {
                            startHour = times[0]
                            startMinute = times[1]
                            endHour = times[2]
                            endMinute = times[3]
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(startHour, startMinute, endHour, endMinute) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun TimePickerButton(
    label: String,
    hour: Int,
    minute: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                formatTime(hour, minute),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FastingScheduleDialog(
    config: SmartBlockingConfig,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int, Int, List<Int>) -> Unit
) {
    var startHour by remember { mutableStateOf(config.fastingStartHour) }
    var startMinute by remember { mutableStateOf(config.fastingStartMinute) }
    var endHour by remember { mutableStateOf(config.fastingEndHour) }
    var endMinute by remember { mutableStateOf(config.fastingEndMinute) }
    var limitMinutes by remember { mutableStateOf(config.fastingDailyLimitMinutes) }
    var selectedDays by remember { 
        mutableStateOf(
            config.fastingDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Ayuno Digital") },
        text = {
            Column {
                Text(
                    "Durante el ayuno, tendrás un límite estricto para todas las apps",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horario
                Text("Horario de ayuno", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerButton("Inicio", startHour, startMinute, false) {}
                    Text(" → ", modifier = Modifier.align(Alignment.CenterVertically))
                    TimePickerButton("Fin", endHour, endMinute, false) {}
                }
                
                // Límite durante ayuno
                Text("Límite durante ayuno: $limitMinutes min", fontWeight = FontWeight.Medium)
                Slider(
                    value = limitMinutes.toFloat(),
                    onValueChange = { limitMinutes = it.toInt() },
                    valueRange = 5f..60f,
                    steps = 10
                )
                
                // Días de la semana
                Text("Días de ayuno", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("L" to 1, "M" to 2, "X" to 3, "J" to 4, "V" to 5, "S" to 6, "D" to 7)
                        .forEach { (label, day) ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    selectedDays = if (day in selectedDays)
                                        selectedDays - day
                                    else
                                        selectedDays + day
                                },
                                label = { Text(label) }
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onConfirm(startHour, startMinute, endHour, endMinute, limitMinutes, selectedDays.toList())
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun NuclearModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, List<String>, Int) -> Unit
) {
    var durationDays by remember { mutableStateOf(30) }
    var waitMinutes by remember { mutableStateOf(30) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("☢️", fontSize = 48.sp) },
        title = { 
            Text(
                "Modo Nuclear",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        },
        text = {
            Column {
                Text(
                    "⚠️ ADVERTENCIA: Este modo es EXTREMO",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "• No podrás desactivarlo hasta que termine\n" +
                    "• El timer de desbloqueo SOLO avanza con InTime abierto\n" +
                    "• Deberás esperar $waitMinutes minutos con la app visible",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Duración: $durationDays días", fontWeight = FontWeight.Medium)
                Slider(
                    value = durationDays.toFloat(),
                    onValueChange = { durationDays = it.toInt() },
                    valueRange = 7f..90f,
                    steps = 11
                )
                
                Text("Tiempo de espera para desbloquear: $waitMinutes min", fontWeight = FontWeight.Medium)
                Slider(
                    value = waitMinutes.toFloat(),
                    onValueChange = { waitMinutes = it.toInt() },
                    valueRange = 15f..60f,
                    steps = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(durationDays, emptyList(), waitMinutes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text("⚠️ ACTIVAR MODO NUCLEAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ContextRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (ContextBlockRule) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(9) }
    var endHour by remember { mutableStateOf(18) }
    var limitMinutes by remember { mutableStateOf(15) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Regla de Contexto") },
        text = {
            Column {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Nombre (ej: Trabajo, Escuela)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Horario: $startHour:00 - $endHour:00")
                RangeSlider(
                    value = startHour.toFloat()..endHour.toFloat(),
                    onValueChange = {
                        startHour = it.start.toInt()
                        endHour = it.endInclusive.toInt()
                    },
                    valueRange = 0f..24f,
                    steps = 23
                )
                
                Text("Límite: $limitMinutes min")
                Slider(
                    value = limitMinutes.toFloat(),
                    onValueChange = { limitMinutes = it.toInt() },
                    valueRange = 5f..60f
                )
                
                Text("Días:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("L" to 1, "M" to 2, "X" to 3, "J" to 4, "V" to 5, "S" to 6, "D" to 7)
                        .forEach { (label, day) ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    selectedDays = if (day in selectedDays)
                                        selectedDays - day
                                    else
                                        selectedDays + day
                                },
                                label = { Text(label) }
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ruleName.isNotBlank()) {
                        val rule = ContextBlockRule(
                            ruleName = ruleName,
                            scheduleStartHour = startHour,
                            scheduleEndHour = endHour,
                            contextDailyLimitMinutes = limitMinutes,
                            scheduleDaysOfWeek = selectedDays.sorted().joinToString(",")
                        )
                        onConfirm(rule)
                    }
                },
                enabled = ruleName.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// === UTILIDADES ===

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}

private fun getDaysText(daysString: String): String {
    val dayNames = mapOf(
        1 to "Lun", 2 to "Mar", 3 to "Mié",
        4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom"
    )
    val days = daysString.split(",").mapNotNull { it.trim().toIntOrNull() }
    
    return if (days.containsAll(listOf(1, 2, 3, 4, 5)) && days.size == 5) {
        "L-V"
    } else if (days.containsAll(listOf(1, 2, 3, 4, 5, 6, 7))) {
        "Todos los días"
    } else {
        days.mapNotNull { dayNames[it] }.joinToString(", ")
    }
}

@Composable
private fun QuickStat(
    value: String,
    label: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Help,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { 
            Text(
                "¿Cómo funciona el Bloqueo Inteligente?",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpItem(
                    emoji = "⏱️",
                    title = "Timer Flotante",
                    description = "Muestra un contador siempre visible sobre todas las apps para que veas cuánto tiempo llevas."
                )
                HelpItem(
                    emoji = "😴",
                    title = "Ventana de Sueño",
                    description = "El tiempo que uses el celular durante tus horas de sueño no se cuenta contra tus límites."
                )
                HelpItem(
                    emoji = "🥗",
                    title = "Ayuno Digital",
                    description = "Establece horarios específicos con límites muy estrictos, ideal para trabajo o estudio."
                )
                HelpItem(
                    emoji = "☢️",
                    title = "Modo Nuclear",
                    description = "El bloqueo más extremo. Una vez activado, NO puedes desactivarlo hasta que termine el periodo."
                )
                HelpItem(
                    emoji = "🔥",
                    title = "Protección de Rachas",
                    description = "Usa días de gracia para proteger tu racha cuando superas tus límites ocasionalmente."
                )
                HelpItem(
                    emoji = "📍",
                    title = "Bloqueo por Contexto",
                    description = "Crea reglas personalizadas por horario y día de la semana."
                )
                HelpItem(
                    emoji = "💬",
                    title = "Solo Comunicación",
                    description = "Permite DMs y mensajes pero bloquea feeds, reels y stories."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("¡Entendido!")
            }
        }
    )
}

@Composable
private fun HelpItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

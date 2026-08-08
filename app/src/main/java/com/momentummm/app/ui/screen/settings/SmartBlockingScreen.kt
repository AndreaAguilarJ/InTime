package com.momentummm.app.ui.screen.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.momentummm.app.R
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.service.FloatingTimerService
import com.momentummm.app.service.NuclearModeService
import com.momentummm.app.service.ContextBlockingService
import com.momentummm.app.ui.system.*
import java.util.*
import com.momentummm.app.ui.theme.*
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.momentummm.app.ui.system.MomentumDesign
import com.momentummm.app.ui.theme.momentum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBlockingScreen(
    onBackClick: () -> Unit,
    viewModel: SmartBlockingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val contextRules by viewModel.contextRules.collectAsStateWithLifecycle()
    val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Lifecycle observer para verificar permiso cuando la app vuelve a primer plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission.value = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Dialogs
    var showSleepTimeDialog by remember { mutableStateOf(false) }
    var showFastingDialog by remember { mutableStateOf(false) }
    var showNuclearModeDialog by remember { mutableStateOf(false) }
    var showContextRuleDialog by remember { mutableStateOf(false) }
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
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
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
                                    label = "Timer",
                                    isActive = config.floatingTimerEnabled
                                )
                                QuickStat(
                                    label = "Sueño",
                                    isActive = config.sleepModeEnabled
                                )
                                QuickStat(
                                    label = "Ayuno",
                                    isActive = config.digitalFastingEnabled
                                )
                                QuickStat(
                                    label = "Nuclear",
                                    isActive = config.isNuclearModeActive(),
                                    // El modo nuclear activo es el único estado que
                                    // conviene señalar en rojo: es irreversible.
                                    activeColor = MaterialTheme.momentum.danger
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
                    title = "Timer Flotante",
                    subtitle = "Siempre visible sobre todas las apps",
                    isEnabled = config.floatingTimerEnabled,
                    onToggle = { enabled ->
                        viewModel.setFloatingTimerEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, "⏱️ Timer flotante activado", Toast.LENGTH_SHORT).show()
                        } else {
                            // Detener el timer flotante si se deshabilita
                            FloatingTimerService.stop(context)
                        }
                    },
                    accentColor = Sky500,
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
                                Column {
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
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Botón de prueba del timer flotante
                                    OutlinedButton(
                                        onClick = {
                                            if (Settings.canDrawOverlays(context)) {
                                                android.util.Log.d("SmartBlocking", "Starting floating timer test")
                                                FloatingTimerService.start(
                                                    context = context,
                                                    appName = "App de Prueba",
                                                    packageName = "com.test.app",
                                                    remainingMinutes = 25,
                                                    totalMinutes = 30
                                                )
                                                Toast.makeText(context, "Timer flotante iniciado - mira en la esquina superior derecha", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Necesitas el permiso de superposición - presiona 'Conceder' primero", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Probar Timer Flotante")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    TextButton(
                                        onClick = { FloatingTimerService.stop(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Detener Timer")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        "ℹ️ El timer aparecerá automáticamente al usar apps con límite",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else null
                )
            }
            
            // === VENTANA DE SUEÑO ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Bedtime,
                    title = "Ventana de Sueño",
                    subtitle = "No contar uso durante horas de sueño",
                    isEnabled = config.sleepModeEnabled,
                    onToggle = { enabled ->
                        viewModel.setSleepModeEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, "😴 Modo sueño activado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = Violet600,
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
                    title = "Ayuno Digital",
                    subtitle = "Límites estrictos en horario laboral",
                    isEnabled = config.digitalFastingEnabled,
                    onToggle = { enabled ->
                        viewModel.setDigitalFastingEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, "🥗 Ayuno digital activado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = Mint500,
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
                    title = "Modo Nuclear",
                    subtitle = "Bloqueo extremo de 1-3 meses",
                    isEnabled = config.nuclearModeEnabled && config.isNuclearModeActive(),
                    onToggle = { enabled ->
                        if (enabled) {
                            showNuclearModeDialog = true
                        } else {
                            viewModel.deactivateNuclearMode()
                            NuclearModeService.stop(context)
                        }
                    },
                    accentColor = Rose500,
                    extraContent = if (config.isNuclearModeActive()) {
                        {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Rose500.copy(alpha = 0.1f)
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
                                                color = Rose500
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
                                        progress = if (config.nuclearModeDurationDays > 0) {
                                            1f - (viewModel.getNuclearModeRemainingDays().toFloat() / config.nuclearModeDurationDays)
                                        } else 0f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Rose500
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
                    title = "Protección de Rachas",
                    subtitle = "Días de gracia para no perder tu racha",
                    isEnabled = config.streakProtectionEnabled,
                    onToggle = { enabled ->
                        viewModel.setStreakProtectionEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, "🔥 Protección de rachas activada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    accentColor = Coral500,
                    extraContent = if (config.streakProtectionEnabled) {
                        {
                            Column {
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
                                            color = if (config.graceDaysUsedThisWeek >= config.graceDaysPerWeek) 
                                                Rose500 
                                            else 
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Row {
                                        (0..3).forEach { days ->
                                            FilterChip(
                                                selected = config.graceDaysPerWeek == days,
                                                onClick = { 
                                                    viewModel.setGraceDaysPerWeek(days)
                                                    Toast.makeText(context, "Días de gracia: $days por semana", Toast.LENGTH_SHORT).show()
                                                },
                                                label = { Text("$days") },
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                
                                // Slider para minutos de advertencia
                                AnimatedVisibility(visible = config.warningBeforeStreakBreak) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text(
                                            "Avisar ${config.warningMinutesBeforeLimit} min antes del límite",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = config.warningMinutesBeforeLimit.toFloat(),
                                            onValueChange = { viewModel.setWarningMinutesBeforeLimit(it.toInt()) },
                                            valueRange = 1f..15f,
                                            steps = 13
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "ℹ️ Un día de gracia significa que tu racha no se rompe aunque superes el límite ese día",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                    title = "Bloqueo por Contexto",
                    subtitle = "Reglas por horario, ubicación o WiFi",
                    isEnabled = config.contextBlockingEnabled,
                    onToggle = { enabled ->
                        viewModel.setContextBlockingEnabled(enabled)
                        if (enabled) {
                            // startIfPossible comprueba el permiso de ubicación.
                            // Antes se arrancaba a ciegas y el aviso decía
                            // "servicio iniciado" incluso cuando Android lo
                            // mataba de inmediato por falta de permiso.
                            val started = ContextBlockingService.startIfPossible(context)
                            val message = if (started) {
                                "📍 Servicio de contexto iniciado"
                            } else {
                                "Concede el permiso de ubicación para usar el bloqueo por contexto"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } else {
                            // Detener servicio
                            ContextBlockingService.stop(context)
                        }
                    },
                    extraContent = if (config.contextBlockingEnabled) {
                        {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                contextRules.forEach { rule ->
                                    key(rule.id) {
                                        ContextRuleCard(
                                            rule = rule,
                                            onToggle = { viewModel.toggleContextRule(rule.id, it) },
                                            onDelete = { viewModel.deleteContextRule(rule.id) }
                                        )
                                    }
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
                    title = "Modo Solo Comunicación",
                    subtitle = "Permite mensajes, bloquea feeds y reels",
                    isEnabled = config.communicationOnlyModeEnabled,
                    onToggle = { enabled ->
                        viewModel.setCommunicationOnlyMode(enabled)
                        if (enabled) {
                            Toast.makeText(context, "💬 Modo comunicación activado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    extraContent = if (config.communicationOnlyModeEnabled) {
                        {
                            Column {
                                Text(
                                    "Apps en modo comunicación:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Lista expandida de apps de redes sociales
                                val apps = listOf(
                                    Triple("📸 Instagram", "com.instagram.android", "instagram"),
                                    Triple("📘 Facebook", "com.facebook.katana", "facebook"),
                                    Triple("🐦 Twitter/X", "com.twitter.android", "twitter"),
                                    Triple("🎵 TikTok", "com.zhiliaoapp.musically", "tiktok"),
                                    Triple("▶️ YouTube", "com.google.android.youtube", "youtube"),
                                    Triple("👻 Snapchat", "com.snapchat.android", "snapchat"),
                                    Triple("💬 WhatsApp", "com.whatsapp", "whatsapp"),
                                    Triple("✈️ Telegram", "org.telegram.messenger", "telegram")
                                )
                                
                                apps.forEach { (name, packageName, _) ->
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
                                
                                SwitchRow("✉️ Permitir DMs/Mensajes", config.communicationOnlyAllowDMs) {
                                    viewModel.setCommunicationOnlyAllowDMs(it)
                                }
                                SwitchRow("📰 Bloquear Feed", config.communicationOnlyBlockFeed) {
                                    viewModel.setCommunicationOnlyBlockFeed(it)
                                }
                                SwitchRow("📖 Bloquear Stories", config.communicationOnlyBlockStories) {
                                    viewModel.setCommunicationOnlyBlockStories(it)
                                }
                                SwitchRow("🎬 Bloquear Reels/Shorts", config.communicationOnlyBlockReels) {
                                    viewModel.setCommunicationOnlyBlockReels(it)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "ℹ️ Las apps de mensajería (WhatsApp, Telegram) ya permiten comunicación por defecto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
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
                // Iniciar el servicio del modo nuclear
                NuclearModeService.start(context)
                Toast.makeText(context, "☢️ Modo Nuclear activado por $days días", Toast.LENGTH_LONG).show()
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
    
    if (showFloatingTimerSettings) {
        FloatingTimerSettingsDialog(
            currentPosition = config.floatingTimerPosition,
            currentSize = config.floatingTimerSize,
            onDismiss = { showFloatingTimerSettings = false },
            onConfirm = { position, size ->
                viewModel.setFloatingTimerPosition(position)
                viewModel.setFloatingTimerSize(size)
                showFloatingTimerSettings = false
            }
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
        shape = MomentumDesign.Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.momentum.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isEnabled) 1.5.dp else 1.dp,
            color = if (isEnabled) {
                accentColor.copy(alpha = MomentumDesign.Alpha.strong)
            } else {
                MaterialTheme.momentum.border
            }
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
                        modifier = Modifier.size(MomentumDesign.Size.iconTile),
                        shape = RoundedCornerShape(MomentumDesign.CornerRadius.small),
                        color = accentColor.copy(alpha = MomentumDesign.Alpha.soft)
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
    var editingStart by remember { mutableStateOf(true) }
    var selectedDays by remember { 
        mutableStateOf(
            config.fastingDaysOfWeek
                .takeIf { it.isNotBlank() }
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: setOf(1, 2, 3, 4, 5) // Días por defecto: Lunes a Viernes
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
                    TimePickerButton("Inicio", startHour, startMinute, editingStart) { editingStart = true }
                    Text(" → ", modifier = Modifier.align(Alignment.CenterVertically))
                    TimePickerButton("Fin", endHour, endMinute, !editingStart) { editingStart = false }
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
                color = Rose500
            )
        },
        text = {
            Column {
                Text(
                    "⚠️ ADVERTENCIA: Este modo es EXTREMO",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Rose500
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
                    containerColor = Rose500
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
private fun FloatingTimerSettingsDialog(
    currentPosition: String,
    currentSize: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedPosition by remember { mutableStateOf(currentPosition) }
    var selectedSize by remember { mutableStateOf(currentSize) }
    
    val positions = listOf(
        "TOP_LEFT" to "Arriba Izquierda",
        "TOP_RIGHT" to "Arriba Derecha",
        "BOTTOM_LEFT" to "Abajo Izquierda",
        "BOTTOM_RIGHT" to "Abajo Derecha"
    )
    
    val sizes = listOf(
        "SMALL" to "Pequeño",
        "MEDIUM" to "Mediano",
        "LARGE" to "Grande"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
        title = { Text("Configurar Timer Flotante") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Posición en pantalla:", fontWeight = FontWeight.Medium)
                Column {
                    positions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPosition = value }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPosition == value,
                                onClick = { selectedPosition = value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                Divider()
                
                Text("Tamaño:", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    sizes.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedSize == value,
                            onClick = { selectedSize = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPosition, selectedSize) }) {
                Text("Guardar")
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
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(18) }
    var endMinute by remember { mutableStateOf(0) }
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
                
                Text("Horario: ${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}")
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
                            scheduleStartMinute = startMinute,
                            scheduleEndHour = endHour,
                            scheduleEndMinute = endMinute,
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
    label: String,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    // Un "✓/✗" en texto se lee como dato; un punto relleno se lee como estado.
    // En una fila de cuatro indicadores, lo segundo se escanea de un vistazo.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) {
                        activeColor.copy(alpha = MomentumDesign.Alpha.soft)
                    } else {
                        MaterialTheme.momentum.surfaceSunken
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else Icons.Default.Remove,
                contentDescription = null,
                tint = if (isActive) activeColor else MaterialTheme.momentum.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.momentum.textSecondary
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

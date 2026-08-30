package com.momentummm.app.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.pluralStringResource
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
import com.momentummm.app.util.BlockingCapabilities
import com.momentummm.app.util.ContextSnapshot
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
    val selectableApps by viewModel.selectableApps.collectAsStateWithLifecycle()
    val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Estado de los permisos que las funciones activas necesitan de verdad.
    // Se recomprueban en ON_RESUME, junto con el de superposición, porque el
    // usuario los concede fuera de la app.
    val hasUsageAccess = remember {
        mutableStateOf(BlockingCapabilities.hasUsageStatsPermission(context))
    }
    val hasAccessibility = remember {
        mutableStateOf(BlockingCapabilities.isAccessibilityEnabled(context))
    }
    val hasLocation = remember {
        mutableStateOf(ContextSnapshot.hasLocationPermission(context))
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasLocation.value = granted.values.any { it }
        // Si acaba de concederlo con el bloqueo por contexto ya encendido, el
        // servicio puede arrancar ahora: antes no se podía conceder desde
        // ningún punto de la app y la función quedaba muerta para siempre.
        if (hasLocation.value && config.contextBlockingEnabled) {
            ContextBlockingService.startIfPossible(context)
        }
    }

    /** Un permiso que falta, con su texto y su forma de concederlo. */
    data class MissingPermission(val labelRes: Int, val grant: () -> Unit)

    val missingPermissions = buildList {
        // El acceso al uso es la base de todo: sin él el monitor no puede
        // detectar qué app está delante y ninguna de las siete funciona.
        if (!hasUsageAccess.value) {
            add(
                MissingPermission(R.string.smart_perm_usage) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }
            )
        }
        if (!hasOverlayPermission.value) {
            add(
                MissingPermission(R.string.smart_perm_overlay) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    }
                }
            )
        }
        // La accesibilidad sólo hace falta para bloquear contenido DENTRO de las
        // apps permitidas; se pide únicamente si esa función está encendida.
        if (config.communicationOnlyModeEnabled && !hasAccessibility.value) {
            add(
                MissingPermission(R.string.smart_perm_accessibility) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            )
        }
        if (config.contextBlockingEnabled && !hasLocation.value) {
            add(
                MissingPermission(R.string.smart_perm_location) {
                    locationLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }
    
    // Lifecycle observer para verificar permiso cuando la app vuelve a primer plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission.value = Settings.canDrawOverlays(context)
                hasUsageAccess.value = BlockingCapabilities.hasUsageStatsPermission(context)
                hasAccessibility.value = BlockingCapabilities.isAccessibilityEnabled(context)
                hasLocation.value = ContextSnapshot.hasLocationPermission(context)
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
    var showNuclearUnlockDialog by remember { mutableStateOf(false) }
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
                        stringResource(R.string.smart_title),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, stringResource(R.string.smart_help))
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
                                    Icon(Icons.Default.Close, stringResource(R.string.a11y_close), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.smart_advanced_control),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.smart_advanced_control_desc),
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
                                    label = stringResource(R.string.smart_tab_timer),
                                    isActive = config.floatingTimerEnabled
                                )
                                QuickStat(
                                    label = stringResource(R.string.smart_tab_sleep),
                                    isActive = config.sleepModeEnabled
                                )
                                QuickStat(
                                    label = stringResource(R.string.smart_tab_fasting),
                                    isActive = config.digitalFastingEnabled
                                )
                                QuickStat(
                                    label = stringResource(R.string.smart_tab_nuclear),
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
            
            // === AVISO DE PERMISOS QUE FALTAN ===
            //
            // La pantalla sólo comprobaba el permiso de superposición, y sólo
            // dentro del Timer flotante. Todas las funciones se aplican en el
            // monitor, que sin acceso al uso no puede detectar nada, y el
            // bloqueo dentro de apps necesita accesibilidad. Sin este aviso los
            // interruptores quedaban encendidos sin efecto y sin explicación.
            if (missingPermissions.isNotEmpty()) {
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
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.smart_perm_missing_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            missingPermissions.forEach { permission ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(permission.labelRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { permission.grant() }) {
                                        Text(stringResource(R.string.smart_grant))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === TIMER FLOTANTE ===
            item {
                SmartBlockingSection(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.smart_floating_timer),
                    subtitle = stringResource(R.string.smart_floating_timer_desc),
                    isEnabled = config.floatingTimerEnabled,
                    onToggle = { enabled ->
                        viewModel.setFloatingTimerEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, context.getString(R.string.smart_b_floating_on), Toast.LENGTH_SHORT).show()
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
                                    message = stringResource(R.string.smart_overlay_permission_needed),
                                    actionLabel = stringResource(R.string.smart_grant),
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
                                            stringResource(R.string.smart_opacity, (config.floatingTimerOpacity * 100).toInt()),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        TextButton(onClick = { showFloatingTimerSettings = true }) {
                                            Text(stringResource(R.string.smart_configure))
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
                                                    appName = context.getString(R.string.smart_test_app),
                                                    packageName = "com.test.app",
                                                    remainingMinutes = 25,
                                                    totalMinutes = 30
                                                )
                                                Toast.makeText(context, context.getString(R.string.smart_timer_started), Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.smart_timer_needs_permission), Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.smart_test_floating_timer))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    TextButton(
                                        onClick = { FloatingTimerService.stop(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.smart_stop_timer))
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        stringResource(R.string.smart_b_floating_info),
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
                    title = stringResource(R.string.smart_sleep_window),
                    subtitle = stringResource(R.string.smart_sleep_window_desc),
                    isEnabled = config.sleepModeEnabled,
                    onToggle = { enabled ->
                        viewModel.setSleepModeEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, context.getString(R.string.smart_b_sleep_on), Toast.LENGTH_SHORT).show()
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
                                            stringResource(R.string.smart_sleep_schedule),
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
                                    stringResource(R.string.smart_sleep_ignore_tracking),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = config.sleepModeIgnoreTracking,
                                    onCheckedChange = { viewModel.setSleepIgnoreTracking(it) }
                                )
                            }

                            // El bloqueo nocturno es ahora una decisión propia.
                            // Antes se activaba solo, como efecto colateral de
                            // desactivar el recuento, y dejaba el teléfono
                            // bloqueado sin que nadie lo hubiera pedido.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.smart_sleep_block_apps),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        stringResource(R.string.smart_sleep_block_apps_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = config.sleepModeBlockApps,
                                    onCheckedChange = { viewModel.setSleepBlockApps(it) }
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
                    title = stringResource(R.string.smart_digital_fasting),
                    subtitle = stringResource(R.string.smart_digital_fasting_desc),
                    isEnabled = config.digitalFastingEnabled,
                    onToggle = { enabled ->
                        viewModel.setDigitalFastingEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, context.getString(R.string.smart_b_fasting_on), Toast.LENGTH_SHORT).show()
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
                                                stringResource(R.string.smart_fasting_schedule),
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
                                            stringResource(R.string.smart_fasting_limit),
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
                                        stringResource(R.string.smart_days, getDaysText(context, config.fastingDaysOfWeek)),
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
                    title = stringResource(R.string.smart_nuclear_mode),
                    subtitle = stringResource(R.string.smart_nuclear_mode_desc),
                    isEnabled = config.nuclearModeEnabled && config.isNuclearModeActive(),
                    onToggle = { enabled ->
                        if (enabled) {
                            showNuclearModeDialog = true
                        } else {
                            // NO se desactiva aquí. El texto promete que no se
                            // puede apagar hasta que termine, y antes el
                            // interruptor lo apagaba al instante, dejando la
                            // función entera en un adorno.
                            showNuclearUnlockDialog = true
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
                                                stringResource(R.string.smart_nuclear_active),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Rose500
                                            )
                                            val remaining = viewModel.getNuclearModeRemainingDays()
                                            Text(
                                                pluralStringResource(R.plurals.smart_b_days_remaining, remaining, remaining),
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
                                            stringResource(R.string.smart_b_nuclear_warn_foreground),
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
                    title = stringResource(R.string.smart_streak_protection),
                    subtitle = stringResource(R.string.smart_streak_protection_desc),
                    isEnabled = config.streakProtectionEnabled,
                    onToggle = { enabled ->
                        viewModel.setStreakProtectionEnabled(enabled)
                        if (enabled) {
                            Toast.makeText(context, context.getString(R.string.smart_b_streak_on), Toast.LENGTH_SHORT).show()
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
                                            stringResource(R.string.smart_grace_days_per_week),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            stringResource(R.string.smart_b_grace_used, config.graceDaysUsedThisWeek, config.graceDaysPerWeek),
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
                                                    Toast.makeText(context, context.getString(R.string.smart_grace_days_value, days), Toast.LENGTH_SHORT).show()
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
                                        stringResource(R.string.smart_warn_before_break),
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
                                            stringResource(R.string.smart_warn_minutes, config.warningMinutesBeforeLimit),
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
                                    stringResource(R.string.smart_b_grace_info),
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
                    title = stringResource(R.string.smart_context_blocking),
                    subtitle = stringResource(R.string.smart_context_blocking_desc),
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
                                context.getString(R.string.smart_context_service_started)
                            } else {
                                context.getString(R.string.smart_location_permission_needed)
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
                                    Text(stringResource(R.string.smart_add_context_rule))
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
                    title = stringResource(R.string.smart_communication_only),
                    subtitle = stringResource(R.string.smart_communication_only_desc),
                    isEnabled = config.communicationOnlyModeEnabled,
                    onToggle = { enabled ->
                        viewModel.setCommunicationOnlyMode(enabled)
                        if (enabled) {
                            Toast.makeText(context, context.getString(R.string.smart_b_comm_on), Toast.LENGTH_SHORT).show()
                        }
                    },
                    extraContent = if (config.communicationOnlyModeEnabled) {
                        {
                            Column {
                                // Sin apps elegidas el modo no puede hacer nada
                                // útil, y antes el monitor lo interpretaba como
                                // «bloquea todo». Se dice en pantalla.
                                if (config.communicationOnlyApps.isBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            stringResource(R.string.smart_comm_no_apps),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text(
                                    stringResource(R.string.smart_communication_apps),
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
                                    stringResource(R.string.smart_blocking_config),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                SwitchRow(stringResource(R.string.smart_b_allow_dms), config.communicationOnlyAllowDMs) {
                                    viewModel.setCommunicationOnlyAllowDMs(it)
                                }
                                SwitchRow(stringResource(R.string.smart_b_block_feed), config.communicationOnlyBlockFeed) {
                                    viewModel.setCommunicationOnlyBlockFeed(it)
                                }
                                SwitchRow(stringResource(R.string.smart_b_block_stories), config.communicationOnlyBlockStories) {
                                    viewModel.setCommunicationOnlyBlockStories(it)
                                }
                                SwitchRow(stringResource(R.string.smart_b_block_reels), config.communicationOnlyBlockReels) {
                                    viewModel.setCommunicationOnlyBlockReels(it)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    stringResource(R.string.smart_b_comm_info),
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
            selectableApps = selectableApps,
            onDismiss = { showNuclearModeDialog = false },
            onConfirm = { days, apps, waitMinutes ->
                viewModel.activateNuclearMode(days, apps, waitMinutes)
                // Iniciar el servicio del modo nuclear
                NuclearModeService.start(context)
                Toast.makeText(context, context.resources.getQuantityString(R.plurals.smart_b_nuclear_on, days, days), Toast.LENGTH_LONG).show()
                showNuclearModeDialog = false
            }
        )
    }

    if (showNuclearUnlockDialog) {
        NuclearUnlockDialog(
            config = config,
            unlockAvailable = viewModel.isNuclearUnlockAvailable(),
            onDismiss = { showNuclearUnlockDialog = false },
            onRequestWait = {
                viewModel.requestNuclearUnlock()
                // El servicio es quien cuenta la espera con la app abierta.
                NuclearModeService.start(context)
                showNuclearUnlockDialog = false
            },
            onCancelRequest = {
                viewModel.cancelNuclearUnlockRequest()
                showNuclearUnlockDialog = false
            },
            onDeactivate = {
                viewModel.deactivateNuclearMode()
                NuclearModeService.stop(context)
                showNuclearUnlockDialog = false
            }
        )
    }
    
    if (showContextRuleDialog) {
        // Se lee al abrir el diálogo, no en cada recomposición.
        val snapshot = remember { viewModel.readContextSnapshot() }
        ContextRuleDialog(
            currentSsid = snapshot.first,
            currentLocation = snapshot.second,
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
                    stringResource(R.string.smart_rule_limit, rule.contextDailyLimitMinutes),
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
                        contentDescription = stringResource(R.string.a11y_delete),
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

    val isEmptyWindow = (startHour * 60 + startMinute) == (endHour * 60 + endMinute)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smart_sleep_config_title)) },
        text = {
            Column {
                Text(stringResource(R.string.smart_sleep_config_desc))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerButton(
                        label = stringResource(R.string.smart_start),
                        hour = startHour,
                        minute = startMinute,
                        isSelected = editingStart,
                        onClick = { editingStart = true }
                    )
                    TimePickerButton(
                        label = stringResource(R.string.smart_end),
                        hour = endHour,
                        minute = endMinute,
                        isSelected = !editingStart,
                        onClick = { editingStart = false }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector libre de hora. Antes sólo existían tres presets, así
                // que quien dormía de 00:30 a 06:45 no podía expresarlo y el
                // botón Guardar devolvía los mismos valores que ya tenía.
                Text(
                    stringResource(R.string.smart_sleep_pick_time),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                TimeStepperRow(
                    hour = if (editingStart) startHour else endHour,
                    minute = if (editingStart) startMinute else endMinute,
                    onHourChange = { newHour ->
                        if (editingStart) startHour = newHour else endHour = newHour
                    },
                    onMinuteChange = { newMinute ->
                        if (editingStart) startMinute = newMinute else endMinute = newMinute
                    }
                )
                
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

                // Inicio y fin iguales describen una ventana vacía: la función
                // quedaría encendida sin hacer nada. Se dice, en vez de
                // guardarlo en silencio.
                if (isEmptyWindow) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.smart_sleep_invalid_window),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startHour, startMinute, endHour, endMinute) },
                enabled = !isEmptyWindow
            ) {
                Text(stringResource(R.string.limit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.limit_dialog_cancel))
            }
        }
    )
}

/**
 * Ajuste de hora y minuto con botones.
 *
 * Se evita a propósito el TimePicker de Material3: aquí basta un control
 * compacto que quepa dentro del AlertDialog junto a los presets, y los minutos
 * avanzan de cinco en cinco porque nadie define su horario de sueño al minuto.
 */
@Composable
private fun TimeStepperRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeStepper(
            label = stringResource(R.string.smart_sleep_hours_label),
            value = hour,
            // Las horas dan la vuelta: subir desde 23 lleva a 00.
            onDecrease = { onHourChange((hour + 23) % 24) },
            onIncrease = { onHourChange((hour + 1) % 24) }
        )
        TimeStepper(
            label = stringResource(R.string.smart_sleep_minutes_label),
            value = minute,
            onDecrease = { onMinuteChange((minute + 55) % 60) },
            onIncrease = { onMinuteChange((minute + 5) % 60) }
        )
    }
}

@Composable
private fun TimeStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                // 48 dp es el mínimo táctil accesible; el tamaño por defecto de
                // IconButton ya lo cumple, pero se fija para que no dependa del
                // tema.
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.smart_sleep_decrease, label)
                )
            }
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.smart_sleep_increase, label)
                )
            }
        }
    }
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
        title = { Text(stringResource(R.string.smart_fasting_config_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.smart_fasting_config_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horario
                Text(stringResource(R.string.smart_fasting_schedule), fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerButton(stringResource(R.string.smart_start), startHour, startMinute, editingStart) { editingStart = true }
                    Text(" → ", modifier = Modifier.align(Alignment.CenterVertically))
                    TimePickerButton(stringResource(R.string.smart_end), endHour, endMinute, !editingStart) { editingStart = false }
                }

                // BUG CORREGIDO: los dos botones de arriba sólo cambiaban cuál
                // estaba "seleccionado"; no existía ningún control que alterara
                // la hora, así que Guardar devolvía siempre los mismos valores
                // y el horario de ayuno era imposible de cambiar.
                TimeStepperRow(
                    hour = if (editingStart) startHour else endHour,
                    minute = if (editingStart) startMinute else endMinute,
                    onHourChange = { newHour ->
                        if (editingStart) startHour = newHour else endHour = newHour
                    },
                    onMinuteChange = { newMinute ->
                        if (editingStart) startMinute = newMinute else endMinute = newMinute
                    }
                )
                
                // Límite durante ayuno
                Text(stringResource(R.string.smart_fasting_limit_value, limitMinutes), fontWeight = FontWeight.Medium)
                Slider(
                    value = limitMinutes.toFloat(),
                    onValueChange = { limitMinutes = it.toInt() },
                    valueRange = 5f..60f,
                    steps = 10
                )
                
                // Días de la semana
                Text(stringResource(R.string.smart_fasting_days), fontWeight = FontWeight.Medium)
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
                },
                // Sin días no hay ayuno posible, e inicio igual a fin describe
                // una franja vacía: guardarlo dejaría la función encendida sin
                // hacer nada.
                enabled = selectedDays.isNotEmpty() &&
                    (startHour * 60 + startMinute) != (endHour * 60 + endMinute)
            ) {
                Text(stringResource(R.string.limit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_dialog_cancel)) }
        }
    )
}

@Composable
private fun NuclearModeDialog(
    selectableApps: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Int, List<String>, Int) -> Unit
) {
    var durationDays by remember { mutableStateOf(30) }
    var waitMinutes by remember { mutableStateOf(30) }
    // El diálogo confirmaba con emptyList() y el modo se activaba sin bloquear
    // nada. Ahora hay que elegir al menos una app y el botón lo exige.
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("☢️", fontSize = 48.sp) },
        title = { 
            Text(
                stringResource(R.string.smart_nuclear_mode),
                fontWeight = FontWeight.Bold,
                color = Rose500
            )
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    Text(
                        stringResource(R.string.smart_b_nuclear_warn_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Rose500
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.smart_b_nuclear_rule1) +
                        stringResource(R.string.smart_b_nuclear_rule2) +
                        pluralStringResource(R.plurals.smart_b_nuclear_rule3, waitMinutes, waitMinutes),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.smart_nuclear_duration, durationDays), fontWeight = FontWeight.Medium)
                    Slider(
                        value = durationDays.toFloat(),
                        onValueChange = { durationDays = it.toInt() },
                        valueRange = 7f..90f,
                        steps = 11
                    )

                    Text(stringResource(R.string.smart_nuclear_wait, waitMinutes), fontWeight = FontWeight.Medium)
                    Slider(
                        value = waitMinutes.toFloat(),
                        onValueChange = { waitMinutes = it.toInt() },
                        valueRange = 15f..60f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.smart_nuclear_pick_apps, selectedApps.size),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (selectableApps.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.smart_nuclear_apps_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(selectableApps, key = { it.first }) { (packageName, appName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApps = if (packageName in selectedApps) {
                                    selectedApps - packageName
                                } else {
                                    selectedApps + packageName
                                }
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            appName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = packageName in selectedApps,
                            onCheckedChange = { checked ->
                                selectedApps = if (checked) {
                                    selectedApps + packageName
                                } else {
                                    selectedApps - packageName
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(durationDays, selectedApps.toList(), waitMinutes) },
                enabled = selectedApps.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rose500
                )
            ) {
                Text(stringResource(R.string.smart_b_nuclear_activate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_dialog_cancel)) }
        }
    )
}

/**
 * Diálogo de desbloqueo del Modo nuclear.
 *
 * Es lo que sustituye al apagado instantáneo. Explica la espera, la inicia y
 * —solo cuando se ha cumplido— ofrece desactivar de verdad.
 */
@Composable
private fun NuclearUnlockDialog(
    config: SmartBlockingConfig,
    unlockAvailable: Boolean,
    onDismiss: () -> Unit,
    onRequestWait: () -> Unit,
    onCancelRequest: () -> Unit,
    onDeactivate: () -> Unit
) {
    val requiredSeconds = config.nuclearModeUnlockWaitMinutes * 60
    val currentSeconds = config.nuclearModeCurrentWaitSeconds
    val requested = config.nuclearModeUnlockRequested

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("☢️", fontSize = 40.sp) },
        title = {
            Text(
                stringResource(
                    if (unlockAvailable) R.string.smart_nuclear_unlock_ready_title
                    else R.string.smart_nuclear_unlock_title
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when {
                    unlockAvailable -> Text(
                        stringResource(R.string.smart_nuclear_unlock_ready_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    requested -> {
                        Text(
                            stringResource(
                                R.string.smart_nuclear_unlock_progress,
                                currentSeconds / 60,
                                config.nuclearModeUnlockWaitMinutes
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = if (requiredSeconds > 0) {
                                (currentSeconds.toFloat() / requiredSeconds).coerceIn(0f, 1f)
                            } else 0f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Rose500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.smart_nuclear_unlock_keep_open),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> Text(
                        stringResource(
                            R.string.smart_nuclear_unlock_explain,
                            config.nuclearModeUnlockWaitMinutes
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            when {
                unlockAvailable -> Button(
                    onClick = onDeactivate,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text(stringResource(R.string.smart_nuclear_unlock_deactivate))
                }

                requested -> TextButton(onClick = onCancelRequest) {
                    Text(stringResource(R.string.smart_nuclear_unlock_cancel_request))
                }

                else -> Button(onClick = onRequestWait) {
                    Text(stringResource(R.string.smart_nuclear_unlock_start_wait))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_dialog_cancel)) }
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
        "TOP_LEFT" to stringResource(R.string.smart_pos_top_left),
        "TOP_RIGHT" to stringResource(R.string.smart_pos_top_right),
        "BOTTOM_LEFT" to stringResource(R.string.smart_pos_bottom_left),
        "BOTTOM_RIGHT" to stringResource(R.string.smart_pos_bottom_right)
    )
    
    val sizes = listOf(
        "SMALL" to stringResource(R.string.smart_size_small),
        "MEDIUM" to stringResource(R.string.smart_size_medium),
        "LARGE" to stringResource(R.string.smart_size_large)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
        title = { Text(stringResource(R.string.smart_timer_config_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.smart_position_on_screen), fontWeight = FontWeight.Medium)
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
                
                Text(stringResource(R.string.smart_size), fontWeight = FontWeight.Medium)
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
                Text(stringResource(R.string.limit_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_dialog_cancel)) }
        }
    )
}

@Composable
private fun ContextRuleDialog(
    currentSsid: String?,
    currentLocation: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onConfirm: (ContextBlockRule) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    // El diálogo sólo sabía crear reglas de horario, aunque el subtítulo de la
    // sección promete «reglas por horario, ubicación o Wi-Fi». Los otros dos
    // tipos existían en la base y en el servicio, pero eran inalcanzables.
    var ruleType by remember { mutableStateOf("SCHEDULE") }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(18) }
    var endMinute by remember { mutableStateOf(0) }
    var editingStart by remember { mutableStateOf(true) }
    var limitMinutes by remember { mutableStateOf(15) }
    var blockCompletely by remember { mutableStateOf(false) }
    var radiusMeters by remember { mutableStateOf(150) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }

    // Sin los datos del contexto no se puede crear la regla correspondiente, y
    // decirlo es mejor que ofrecer una opción que guardaría una regla vacía.
    val canUseLocation = currentLocation != null
    val canUseWifi = !currentSsid.isNullOrBlank()

    val isValid = ruleName.isNotBlank() && when (ruleType) {
        "LOCATION" -> canUseLocation
        "WIFI" -> canUseWifi
        else -> selectedDays.isNotEmpty() &&
            (startHour * 60 + startMinute) != (endHour * 60 + endMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smart_new_context_rule)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                item {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text(stringResource(R.string.smart_rule_name_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.smart_rule_type),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = ruleType == "SCHEDULE",
                            onClick = { ruleType = "SCHEDULE" },
                            label = { Text(stringResource(R.string.smart_rule_type_schedule)) }
                        )
                        FilterChip(
                            selected = ruleType == "LOCATION",
                            onClick = { ruleType = "LOCATION" },
                            enabled = canUseLocation,
                            label = { Text(stringResource(R.string.smart_rule_type_location)) }
                        )
                        FilterChip(
                            selected = ruleType == "WIFI",
                            onClick = { ruleType = "WIFI" },
                            enabled = canUseWifi,
                            label = { Text(stringResource(R.string.smart_rule_type_wifi)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (ruleType) {
                    "SCHEDULE" -> item {
                        Text(
                            stringResource(
                                R.string.smart_rule_schedule,
                                formatTime(startHour, startMinute),
                                formatTime(endHour, endMinute)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimePickerButton(
                                stringResource(R.string.smart_start),
                                startHour, startMinute, editingStart
                            ) { editingStart = true }
                            TimePickerButton(
                                stringResource(R.string.smart_end),
                                endHour, endMinute, !editingStart
                            ) { editingStart = false }
                        }
                        // BUG CORREGIDO: el RangeSlider anterior sólo permitía
                        // horas en punto y los minutos se guardaban siempre a 00.
                        TimeStepperRow(
                            hour = if (editingStart) startHour else endHour,
                            minute = if (editingStart) startMinute else endMinute,
                            onHourChange = { if (editingStart) startHour = it else endHour = it },
                            onMinuteChange = { if (editingStart) startMinute = it else endMinute = it }
                        )

                        Text(stringResource(R.string.smart_days_label))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("L" to 1, "M" to 2, "X" to 3, "J" to 4, "V" to 5, "S" to 6, "D" to 7)
                                .forEach { (label, day) ->
                                    FilterChip(
                                        selected = day in selectedDays,
                                        onClick = {
                                            selectedDays = if (day in selectedDays) {
                                                selectedDays - day
                                            } else {
                                                selectedDays + day
                                            }
                                        },
                                        label = { Text(label) }
                                    )
                                }
                        }
                    }

                    "LOCATION" -> item {
                        Text(
                            stringResource(R.string.smart_rule_location_current),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.smart_rule_radius, radiusMeters))
                        Slider(
                            value = radiusMeters.toFloat(),
                            onValueChange = { radiusMeters = it.toInt() },
                            valueRange = 50f..1000f
                        )
                    }

                    else -> item {
                        Text(
                            stringResource(
                                R.string.smart_rule_wifi_current,
                                currentSsid ?: ""
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // `blockCompletely` existía en la entidad y en el motor, pero
                    // la interfaz no permitía activarlo: toda regla nacía como
                    // simple reducción de límite.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.smart_rule_block_completely),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = blockCompletely,
                            onCheckedChange = { blockCompletely = it }
                        )
                    }

                    AnimatedVisibility(visible = !blockCompletely) {
                        Column {
                            Text(stringResource(R.string.smart_rule_limit_value, limitMinutes))
                            Slider(
                                value = limitMinutes.toFloat(),
                                onValueChange = { limitMinutes = it.toInt() },
                                valueRange = 5f..60f
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rule = ContextBlockRule(
                        ruleName = ruleName,
                        contextType = ruleType,
                        latitude = currentLocation?.first.takeIf { ruleType == "LOCATION" },
                        longitude = currentLocation?.second.takeIf { ruleType == "LOCATION" },
                        radiusMeters = radiusMeters,
                        wifiSsid = currentSsid.takeIf { ruleType == "WIFI" },
                        scheduleStartHour = startHour,
                        scheduleStartMinute = startMinute,
                        scheduleEndHour = endHour,
                        scheduleEndMinute = endMinute,
                        contextDailyLimitMinutes = limitMinutes,
                        blockCompletely = blockCompletely,
                        overrideDailyLimit = !blockCompletely,
                        scheduleDaysOfWeek = selectedDays.sorted().joinToString(",")
                    )
                    onConfirm(rule)
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.smart_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.limit_dialog_cancel)) }
        }
    )
}

// === UTILIDADES ===

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}

/**
 * Texto legible de los días seleccionados.
 *
 * Antes los nombres abreviados eran un mapa fijo en español ("Lun", "Mar", "Mié"...),
 * así que un usuario alemán veía abreviaturas españolas. Ahora se piden a java.time con
 * la configuración regional del dispositivo: salen correctos en cualquier idioma, no solo
 * en los cinco que esta app traduce.
 *
 * Necesita Context porque "L-V" y stringResource(R.string.smart_b_every_day) sí son textos propios de la app.
 */
private fun getDaysText(context: Context, daysString: String): String {
    val days = daysString.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()

    return when {
        days == listOf(1, 2, 3, 4, 5) -> context.getString(R.string.smart_days_weekdays)
        days == listOf(1, 2, 3, 4, 5, 6, 7) -> context.getString(R.string.smart_days_everyday)
        else -> days.mapNotNull { numero ->
            // DayOfWeek.of(1) es lunes, igual que la numeración que ya usaba la app.
            runCatching {
                java.time.DayOfWeek.of(numero).getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault()
                )
            }.getOrNull()
        }.joinToString(", ")
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
                stringResource(R.string.smart_help_title),
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
                    title = stringResource(R.string.smart_floating_timer),
                    description = stringResource(R.string.smart_help_timer)
                )
                HelpItem(
                    emoji = "😴",
                    title = stringResource(R.string.smart_sleep_window),
                    description = stringResource(R.string.smart_help_sleep)
                )
                HelpItem(
                    emoji = "🥗",
                    title = stringResource(R.string.smart_digital_fasting),
                    description = stringResource(R.string.smart_help_fasting)
                )
                HelpItem(
                    emoji = "☢️",
                    title = stringResource(R.string.smart_nuclear_mode),
                    description = stringResource(R.string.smart_help_nuclear)
                )
                HelpItem(
                    emoji = "🔥",
                    title = stringResource(R.string.smart_streak_protection),
                    description = stringResource(R.string.smart_help_streak)
                )
                HelpItem(
                    emoji = "📍",
                    title = stringResource(R.string.smart_context_blocking),
                    description = stringResource(R.string.smart_help_context)
                )
                HelpItem(
                    emoji = "💬",
                    title = stringResource(R.string.smart_help_communication_title),
                    description = stringResource(R.string.smart_help_communication)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.smart_understood))
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

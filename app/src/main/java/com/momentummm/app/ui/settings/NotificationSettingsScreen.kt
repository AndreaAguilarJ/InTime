package com.momentummm.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.data.notificationPrefs
import com.momentummm.app.util.PermissionUtils
import kotlinx.coroutines.launch

// El delegado del DataStore ya NO se declara aquí. Antes existía una segunda
// instancia `by preferencesDataStore(name = "notification_preferences")` que,
// junto con la de NotificationManager, hacía que Android lanzara
// IllegalStateException («multiple DataStores active for the same file») y
// tumbara la app al abrir esta pantalla. Ahora se importa el delegado único.

/** Claves de esta pantalla. Deben coincidir con las de SmartNotificationManager. */
object NotificationPrefsKeys {
    val APP_LIMITS_ENABLED = booleanPreferencesKey("app_limits_notifications")
    val DAILY_MOTIVATION_ENABLED = booleanPreferencesKey("daily_motivation")
    val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary")
    val ACHIEVEMENTS_ENABLED = booleanPreferencesKey("achievements_notifications")
    val SCREEN_TIME_REMINDERS_ENABLED = booleanPreferencesKey("screen_time_reminders")
    val STREAK_WARNINGS_ENABLED = booleanPreferencesKey("streak_warnings")
    val LIFE_WEEKS_ENABLED = booleanPreferencesKey("life_weeks_notifications")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
}

/**
 * Ajustes de notificaciones.
 *
 * ─── QUÉ CAMBIÓ ──────────────────────────────────────────────────────────
 * - Si Android bloquea los avisos (permiso denegado o app silenciada) lo dice
 *   y ofrece arreglarlo. Antes los interruptores salían encendidos y no
 *   llegaba nada, sin ninguna pista.
 * - Avisa de que sin acceso a uso no hay límites, resumen ni comparaciones.
 * - Expone «Avisos de racha» y «Horario silencioso», que existían en el
 *   código pero no tenían interruptor, y el nuevo «Mi vida en semanas».
 * - «Motivación diaria» se retiró (duplicaba el «Buenos días» de los mensajes
 *   motivacionales); en su lugar hay un acceso a esos mensajes con su horario.
 * - El botón de prueba dice si de verdad se envió.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenMotivationalSettings: () -> Unit = {},
    onOpenPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val application = context.applicationContext as MomentumApplication

    // Un único Flow memorizado: crear el Flow dentro de la composición abría
    // una suscripción nueva al DataStore en cada recomposición.
    val prefsFlow = remember(context) { context.notificationPrefs.data }
    val prefs by prefsFlow.collectAsStateWithLifecycle(initialValue = null)
    val motivationalFlow = remember(application) { application.motivationalMessagesRepository.getPreferences() }
    val motivational by motivationalFlow.collectAsStateWithLifecycle(initialValue = null)

    // El estado del sistema cambia FUERA de la app (el usuario vuelve de los
    // ajustes de Android), así que se relee cada vez que la pantalla vuelve.
    var systemEnabled by remember { mutableStateOf(areSystemNotificationsEnabled(context)) }
    var usageGranted by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemEnabled = areSystemNotificationsEnabled(context)
                usageGranted = PermissionUtils.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemEnabled = areSystemNotificationsEnabled(context)
        // Denegado de forma permanente: Android ya no muestra el diálogo, el
        // único camino son sus ajustes.
        if (!granted) openAppNotificationSettings(context)
    }
    val onFixSystemNotifications: () -> Unit = {
        if (needsRuntimePermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings(context)
        }
    }

    fun toggle(key: Preferences.Key<Boolean>): (Boolean) -> Unit = { checked ->
        scope.launch { context.notificationPrefs.edit { it[key] = checked } }
    }
    fun isOn(key: Preferences.Key<Boolean>, default: Boolean = true): Boolean =
        prefs?.get(key) ?: default

    val sentMessage = stringResource(R.string.notif_test_sent)
    val blockedMessage = stringResource(R.string.notif_test_blocked)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.notifications_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!systemEnabled) {
                Spacer(Modifier.height(8.dp))
                WarningBanner(
                    icon = Icons.Filled.NotificationsOff,
                    title = stringResource(R.string.notif_system_off_title),
                    body = stringResource(R.string.notif_system_off_body),
                    action = stringResource(R.string.notif_system_off_action),
                    onAction = onFixSystemNotifications
                )
            }
            if (!usageGranted) {
                Spacer(Modifier.height(8.dp))
                WarningBanner(
                    icon = Icons.Filled.QueryStats,
                    title = stringResource(R.string.notif_usage_missing_title),
                    body = stringResource(R.string.notif_usage_missing_body),
                    action = stringResource(R.string.notif_usage_missing_action),
                    onAction = onOpenPermissions
                )
            }

            NotificationSections(
                isOn = ::isOn,
                toggle = ::toggle,
                motivational = motivational,
                onOpenMotivationalSettings = onOpenMotivationalSettings
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val app = context.applicationContext as MomentumApplication
                    val delivered = app.smartNotificationManager.sendTestNotification()
                    systemEnabled = areSystemNotificationsEnabled(context)
                    scope.launch {
                        snackbarHostState.showSnackbar(if (delivered) sentMessage else blockedMessage)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.notification_send_test))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.notification_info_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationSections(
    isOn: (Preferences.Key<Boolean>, Boolean) -> Boolean,
    toggle: (Preferences.Key<Boolean>) -> (Boolean) -> Unit,
    motivational: MotivationalPreferences?,
    onOpenMotivationalSettings: () -> Unit
) {
    SectionHeader(stringResource(R.string.notif_section_protect))
    NotificationSettingItem(
        icon = Icons.Filled.Timer,
        title = stringResource(R.string.notification_app_limits_title),
        description = stringResource(R.string.notification_app_limits_desc),
        checked = isOn(NotificationPrefsKeys.APP_LIMITS_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.APP_LIMITS_ENABLED)
    )
    NotificationSettingItem(
        icon = Icons.Filled.LocalFireDepartment,
        title = stringResource(R.string.notif_streak_warnings_title),
        description = stringResource(R.string.notif_streak_warnings_desc),
        checked = isOn(NotificationPrefsKeys.STREAK_WARNINGS_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.STREAK_WARNINGS_ENABLED)
    )

    SectionHeader(stringResource(R.string.notif_section_progress))
    NotificationSettingItem(
        icon = Icons.Filled.EmojiEvents,
        title = stringResource(R.string.notification_achievements_title),
        description = stringResource(R.string.notification_achievements_desc),
        checked = isOn(NotificationPrefsKeys.ACHIEVEMENTS_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.ACHIEVEMENTS_ENABLED)
    )
    NotificationSettingItem(
        icon = Icons.Filled.BarChart,
        title = stringResource(R.string.notification_weekly_summary_title),
        description = stringResource(R.string.notification_weekly_summary_desc),
        checked = isOn(NotificationPrefsKeys.WEEKLY_SUMMARY_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.WEEKLY_SUMMARY_ENABLED)
    )
    NotificationSettingItem(
        icon = Icons.Filled.Cake,
        title = stringResource(R.string.notif_life_weeks_title),
        description = stringResource(R.string.notif_life_weeks_desc),
        checked = isOn(NotificationPrefsKeys.LIFE_WEEKS_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.LIFE_WEEKS_ENABLED)
    )

    SectionHeader(stringResource(R.string.notif_section_wellbeing))
    NotificationSettingItem(
        icon = Icons.Filled.Alarm,
        title = stringResource(R.string.notification_screen_time_reminders_title),
        description = stringResource(R.string.notification_screen_time_reminders_desc),
        checked = isOn(NotificationPrefsKeys.SCREEN_TIME_REMINDERS_ENABLED, true),
        onCheckedChange = toggle(NotificationPrefsKeys.SCREEN_TIME_REMINDERS_ENABLED)
    )
    MotivationalRow(motivational = motivational, onClick = onOpenMotivationalSettings)
    NotificationSettingItem(
        icon = Icons.Filled.Bedtime,
        title = stringResource(R.string.notif_quiet_hours_title),
        description = stringResource(R.string.notif_quiet_hours_desc),
        // Apagado por defecto, igual que en SmartNotificationManager.
        checked = isOn(NotificationPrefsKeys.QUIET_HOURS_ENABLED, false),
        onCheckedChange = toggle(NotificationPrefsKeys.QUIET_HOURS_ENABLED)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
    )
}

/** Acceso a los mensajes motivacionales con su estado real a la vista. */
@Composable
private fun MotivationalRow(motivational: MotivationalPreferences?, onClick: () -> Unit) {
    val summary = when {
        motivational == null -> ""
        !motivational.enabled -> stringResource(R.string.notif_motivational_row_off)
        else -> stringResource(
            R.string.notif_motivational_row_on,
            motivational.dailyFrequency,
            clock(motivational.startHour, motivational.startMinute),
            clock(motivational.endHour, motivational.endMinute)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(Icons.Filled.EmojiEmotions)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.notif_motivational_row_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WarningBanner(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(action) }
        }
    }
}

/**
 * Fila con interruptor. La fila ENTERA es el control (role = Switch): antes
 * solo el pequeño interruptor respondía y TalkBack leía tres elementos
 * sueltos en lugar de «Resumen semanal, activado».
 */
@Composable
private fun NotificationSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
    )
}

private fun clock(hour: Int, minute: Int): String =
    String.format(java.util.Locale.ROOT, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

private fun areSystemNotificationsEnabled(context: Context): Boolean =
    runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true)

/** En Android 13+ el permiso se pide en tiempo de ejecución mientras no esté concedido. */
private fun needsRuntimePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        // Algunos fabricantes no exponen esa pantalla: la ficha de la app sí existe siempre.
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

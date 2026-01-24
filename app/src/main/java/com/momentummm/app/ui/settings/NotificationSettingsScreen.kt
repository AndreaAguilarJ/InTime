package com.momentummm.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import android.content.Context
import com.momentummm.app.R

private val Context.notificationPrefs: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

object NotificationPrefsKeys {
    val APP_LIMITS_ENABLED = booleanPreferencesKey("app_limits_notifications")
    val DAILY_MOTIVATION_ENABLED = booleanPreferencesKey("daily_motivation")
    val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary")
    val ACHIEVEMENTS_ENABLED = booleanPreferencesKey("achievements_notifications")
    val SCREEN_TIME_REMINDERS_ENABLED = booleanPreferencesKey("screen_time_reminders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados para cada configuración
    val appLimitsEnabled by context.notificationPrefs.data
        .map { it[NotificationPrefsKeys.APP_LIMITS_ENABLED] ?: true }
        .collectAsState(initial = true)

    val dailyMotivationEnabled by context.notificationPrefs.data
        .map { it[NotificationPrefsKeys.DAILY_MOTIVATION_ENABLED] ?: true }
        .collectAsState(initial = true)

    val weeklySummaryEnabled by context.notificationPrefs.data
        .map { it[NotificationPrefsKeys.WEEKLY_SUMMARY_ENABLED] ?: true }
        .collectAsState(initial = true)

    val achievementsEnabled by context.notificationPrefs.data
        .map { it[NotificationPrefsKeys.ACHIEVEMENTS_ENABLED] ?: true }
        .collectAsState(initial = true)

    val screenTimeRemindersEnabled by context.notificationPrefs.data
        .map { it[NotificationPrefsKeys.SCREEN_TIME_REMINDERS_ENABLED] ?: true }
        .collectAsState(initial = true)

    Scaffold(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Encabezado
            Text(
                text = stringResource(R.string.notifications_header_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.notifications_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Límites de aplicaciones
            NotificationSettingItem(
                icon = Icons.Filled.Timer,
                title = stringResource(R.string.notification_app_limits_title),
                description = stringResource(R.string.notification_app_limits_desc),
                checked = appLimitsEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        context.notificationPrefs.edit { prefs ->
                            prefs[NotificationPrefsKeys.APP_LIMITS_ENABLED] = checked
                        }
                    }
                }
            )

            Divider()

            // Motivación diaria
            NotificationSettingItem(
                icon = Icons.Filled.EmojiEmotions,
                title = stringResource(R.string.notification_daily_motivation_title),
                description = stringResource(R.string.notification_daily_motivation_desc),
                checked = dailyMotivationEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        context.notificationPrefs.edit { prefs ->
                            prefs[NotificationPrefsKeys.DAILY_MOTIVATION_ENABLED] = checked
                        }
                    }
                }
            )

            Divider()

            // Resumen semanal
            NotificationSettingItem(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.notification_weekly_summary_title),
                description = stringResource(R.string.notification_weekly_summary_desc),
                checked = weeklySummaryEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        context.notificationPrefs.edit { prefs ->
                            prefs[NotificationPrefsKeys.WEEKLY_SUMMARY_ENABLED] = checked
                        }
                    }
                }
            )

            Divider()

            // Logros y metas
            NotificationSettingItem(
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.notification_achievements_title),
                description = stringResource(R.string.notification_achievements_desc),
                checked = achievementsEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        context.notificationPrefs.edit { prefs ->
                            prefs[NotificationPrefsKeys.ACHIEVEMENTS_ENABLED] = checked
                        }
                    }
                }
            )

            Divider()

            // Recordatorios de tiempo de pantalla
            NotificationSettingItem(
                icon = Icons.Filled.Alarm,
                title = stringResource(R.string.notification_screen_time_reminders_title),
                description = stringResource(R.string.notification_screen_time_reminders_desc),
                checked = screenTimeRemindersEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        context.notificationPrefs.edit { prefs ->
                            prefs[NotificationPrefsKeys.SCREEN_TIME_REMINDERS_ENABLED] = checked
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de prueba
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_test_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // Enviar notificación de prueba
                            val app = context.applicationContext as com.momentummm.app.MomentumApplication
                            app.smartNotificationManager.sendTestNotification()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.notification_send_test))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.notification_info_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


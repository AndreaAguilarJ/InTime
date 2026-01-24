package com.momentummm.app.ui.screen.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.entity.UserSettings
import com.momentummm.app.data.manager.GamificationManager
import com.momentummm.app.data.manager.GamificationState
import com.momentummm.app.ui.screen.PreferenceCategory
import com.momentummm.app.ui.screen.PreferenceItem
import com.momentummm.app.ui.screen.PreferenceSwitchItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as? MomentumApplication
    
    if (application == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.gamification_error_app_not_initialized))
        }
        return
    }
    
    val gamificationManager = application.gamificationManager
    val userDao = application.database.userDao()
    val scope = rememberCoroutineScope()
    
    var gamificationState by remember { mutableStateOf<GamificationState?>(null) }
    var gamificationEnabled by remember { mutableStateOf(true) }
    var showXpNotifications by remember { mutableStateOf(true) }
    var showStreakReminders by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        gamificationManager.getGamificationState().collect { state ->
            gamificationState = state
        }
    }
    
    LaunchedEffect(Unit) {
        userDao.getUserSettings().collect { settings ->
            settings?.let {
                gamificationEnabled = it.gamificationEnabled
                showXpNotifications = it.showXpNotifications
                showStreakReminders = it.showStreakReminders
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gamification_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con estadísticas
            item {
                GamificationStatsCard(gamificationState)
            }
            
            // Configuración principal
            item {
                PreferenceCategory(title = stringResource(R.string.gamification_category_settings)) {
                    PreferenceSwitchItem(
                        title = stringResource(R.string.gamification_active_title),
                        subtitle = stringResource(R.string.gamification_active_subtitle),
                        icon = Icons.Default.EmojiEvents,
                        checked = gamificationEnabled,
                        onCheckedChange = { enabled ->
                            gamificationEnabled = enabled
                            scope.launch {
                                userDao.updateGamificationEnabled(enabled)
                            }
                        }
                    )
                    
                    PreferenceSwitchItem(
                        title = stringResource(R.string.gamification_xp_notifications_title),
                        subtitle = stringResource(R.string.gamification_xp_notifications_subtitle),
                        icon = Icons.Default.Notifications,
                        checked = showXpNotifications,
                        enabled = gamificationEnabled,
                        onCheckedChange = { enabled ->
                            showXpNotifications = enabled
                            scope.launch {
                                userDao.updateShowXpNotifications(enabled)
                            }
                        }
                    )
                    
                    PreferenceSwitchItem(
                        title = stringResource(R.string.gamification_streak_reminders_title),
                        subtitle = stringResource(R.string.gamification_streak_reminders_subtitle),
                        icon = Icons.Default.LocalFireDepartment,
                        checked = showStreakReminders,
                        enabled = gamificationEnabled,
                        onCheckedChange = { enabled ->
                            showStreakReminders = enabled
                            scope.launch {
                                userDao.updateShowStreakReminders(enabled)
                            }
                        },
                        showDivider = false
                    )
                }
            }
            
            // Sincronización
            item {
                PreferenceCategory(title = stringResource(R.string.gamification_category_sync)) {
                    PreferenceItem(
                        title = stringResource(R.string.gamification_sync_title),
                        subtitle = stringResource(R.string.gamification_sync_subtitle),
                        icon = Icons.Default.CloudSync,
                        onClick = { showSyncDialog = true },
                        trailingContent = {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )
                    
                    PreferenceItem(
                        title = stringResource(R.string.gamification_restore_title),
                        subtitle = stringResource(R.string.gamification_restore_subtitle),
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                // TODO: Implementar restauración desde Appwrite
                                kotlinx.coroutines.delay(2000)
                                isSyncing = false
                            }
                        },
                        showDivider = false
                    )
                }
            }
            
            // Acciones
            item {
                PreferenceCategory(title = stringResource(R.string.gamification_category_actions)) {
                    PreferenceItem(
                        title = stringResource(R.string.gamification_reset_title),
                        subtitle = stringResource(R.string.gamification_reset_subtitle),
                        icon = Icons.Default.RestartAlt,
                        onClick = { showResetDialog = true },
                        showDivider = false
                    )
                }
            }
            
            // Información
            item {
                GamificationInfoCard()
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    
    // Diálogo de reinicio
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.gamification_reset_dialog_title)) },
            text = { 
                Text(
                    stringResource(R.string.gamification_reset_dialog_message)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            gamificationManager.resetProgress()
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.gamification_reset_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    
    // Diálogo de sincronización
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
            title = { Text(stringResource(R.string.gamification_sync_dialog_title)) },
            text = { 
                Text(stringResource(R.string.gamification_sync_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            showSyncDialog = false
                            gamificationManager.syncToCloud(application.appwriteUserRepository)
                            isSyncing = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.gamification_sync_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSyncDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun GamificationStatsCard(state: GamificationState?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            if (state == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nivel y título
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = state.levelEmoji,
                                        fontSize = 32.sp
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    text = stringResource(R.string.gamification_level_format, state.level),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = state.levelTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        // TimeCoins
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🪙", fontSize = 24.sp)
                                Text(
                                    text = "${state.timeCoins}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                    
                    // Estadísticas en grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            emoji = "⚡",
                            value = "${state.totalXp}",
                            label = stringResource(R.string.gamification_stat_total_xp)
                        )
                        StatItem(
                            emoji = "🔥",
                            value = "${state.currentStreak}",
                            label = stringResource(R.string.gamification_stat_current_streak)
                        )
                        StatItem(
                            emoji = "⏱️",
                            value = formatMinutes(state.totalFocusMinutes),
                            label = stringResource(R.string.gamification_stat_focus)
                        )
                        StatItem(
                            emoji = "⭐",
                            value = "${state.perfectDays}",
                            label = stringResource(R.string.gamification_stat_perfect_days)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun GamificationInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.gamification_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            InfoRow(
                emoji = "⚡",
                title = stringResource(R.string.gamification_info_xp_title),
                description = stringResource(R.string.gamification_info_xp_desc)
            )
            InfoRow(
                emoji = "🪙",
                title = stringResource(R.string.gamification_info_timecoins_title),
                description = stringResource(R.string.gamification_info_timecoins_desc)
            )
            InfoRow(
                emoji = "🔥",
                title = stringResource(R.string.gamification_info_streaks_title),
                description = stringResource(R.string.gamification_info_streaks_desc)
            )
            InfoRow(
                emoji = "⭐",
                title = stringResource(R.string.gamification_info_perfect_days_title),
                description = stringResource(R.string.gamification_info_perfect_days_desc)
            )
        }
    }
}

@Composable
private fun InfoRow(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

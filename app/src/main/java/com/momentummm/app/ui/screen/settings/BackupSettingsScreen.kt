package com.momentummm.app.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.R
import com.momentummm.app.data.manager.BackupSyncManager
import com.momentummm.app.data.manager.ExportManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBackClick: () -> Unit,
    backupSyncManager: BackupSyncManager,
    exportManager: ExportManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val syncStatus by backupSyncManager.syncStatus.collectAsState()
    val lastSyncTime by backupSyncManager.lastSyncTime.collectAsState()
    val backupProgress by backupSyncManager.backupProgress.collectAsState()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("usage") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.backup_settings_back_cd)
                    )
                }
                
                Text(
                    text = stringResource(R.string.backup_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud Backup Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.backup_settings_cloud_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = lastSyncTime?.let {
                                        stringResource(R.string.backup_settings_last_backup, it)
                                    } ?: stringResource(R.string.backup_settings_no_backups),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Backup progress
                        if (syncStatus == BackupSyncManager.SyncStatus.Syncing) {
                            LinearProgressIndicator(
                                progress = backupProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(
                                    R.string.backup_settings_syncing,
                                    (backupProgress * 100).toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        // Get current user ID (would come from app context)
                                        backupSyncManager.performFullBackup("current_user_id")
                                    }
                                },
                                enabled = syncStatus != BackupSyncManager.SyncStatus.Syncing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_settings_backup_button))
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        backupSyncManager.restoreFromBackup("current_user_id")
                                    }
                                },
                                enabled = syncStatus != BackupSyncManager.SyncStatus.Syncing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_settings_restore_button))
                            }
                        }
                    }
                }
            }
            
            // Export Data Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = stringResource(R.string.backup_settings_export_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Export options
                        val exportOptions = listOf(
                            ExportOption(
                                "usage",
                                R.string.backup_settings_export_usage_title,
                                R.string.backup_settings_export_usage_desc,
                                Icons.Default.Apps
                            ),
                            ExportOption(
                                "focus",
                                R.string.backup_settings_export_focus_title,
                                R.string.backup_settings_export_focus_desc,
                                Icons.Default.Psychology
                            ),
                            ExportOption(
                                "complete",
                                R.string.backup_settings_export_complete_title,
                                R.string.backup_settings_export_complete_desc,
                                Icons.Default.PictureAsPdf
                            )
                        )
                        
                        exportOptions.forEach { option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    exportType = option.type
                                    showExportDialog = true
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(option.titleRes),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Text(
                                            text = stringResource(option.descriptionRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Auto Backup Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.backup_settings_auto_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                Text(
                                    text = stringResource(R.string.backup_settings_auto_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            
                            var autoBackupEnabled by remember { mutableStateOf(false) }
                            
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { enabled ->
                                    autoBackupEnabled = enabled
                                    scope.launch {
                                        if (enabled) {
                                            backupSyncManager.scheduleAutoBackup("current_user_id")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Export confirmation dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.backup_settings_export_title)) },
            text = { 
                Text(stringResource(R.string.backup_settings_export_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (exportType) {
                                "usage" -> {
                                    // Export usage data - would need actual data from repository
                                    val result = exportManager.exportUsageDataToCsv(emptyList())
                                    result.onSuccess { uri ->
                                        exportManager.shareFile(uri, "usage_data.csv")
                                    }
                                }
                                "focus" -> {
                                    // Export focus sessions
                                    val result = exportManager.exportFocusSessionsToCsv(emptyList())
                                    result.onSuccess { uri ->
                                        exportManager.shareFile(uri, "focus_sessions.csv")
                                    }
                                }
                                "complete" -> {
                                    // Export complete report
                                    val result = exportManager.exportCompleteReportToPdf(
                                        emptyList(), emptyList(), emptyList()
                                    )
                                    result.onSuccess { uri ->
                                        exportManager.shareFile(uri, "complete_report.pdf")
                                    }
                                }
                            }
                        }
                        showExportDialog = false
                    }
                ) {
                    Text(stringResource(R.string.backup_settings_export_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.backup_settings_cancel_button))
                }
            }
        )
    }
}

data class ExportOption(
    val type: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
)
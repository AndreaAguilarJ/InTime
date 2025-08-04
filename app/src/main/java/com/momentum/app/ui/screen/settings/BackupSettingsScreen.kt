package com.momentum.app.ui.screen.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentum.app.data.manager.BackupSyncManager
import com.momentum.app.data.manager.ExportManager
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
                        contentDescription = "Volver"
                    )
                }
                
                Text(
                    text = "Respaldo y Exportación",
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
                                    text = "Respaldo en la Nube",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = lastSyncTime?.let { "Último respaldo: $it" } ?: "Sin respaldos",
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
                                text = "Respaldando... ${(backupProgress * 100).toInt()}%",
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
                                Text("Respaldar")
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
                                Text("Restaurar")
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
                                text = "Exportar Datos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Export options
                        val exportOptions = listOf(
                            ExportOption("usage", "Datos de Uso", "CSV con estadísticas de aplicaciones", Icons.Default.Apps),
                            ExportOption("focus", "Sesiones de Enfoque", "CSV con historial de sesiones", Icons.Default.Psychology),
                            ExportOption("complete", "Reporte Completo", "PDF con análisis detallado", Icons.Default.PictureAsPdf)
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
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Text(
                                            text = option.description,
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
                                    text = "Respaldo Automático",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                Text(
                                    text = "Respaldar datos automáticamente cada 24 horas",
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
            title = { Text("Exportar Datos") },
            text = { 
                Text("¿Deseas exportar los datos seleccionados? El archivo se guardará y podrás compartirlo.")
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
                    Text("Exportar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

data class ExportOption(
    val type: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)
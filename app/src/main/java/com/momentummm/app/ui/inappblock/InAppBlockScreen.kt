package com.momentummm.app.ui.inappblock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.data.entity.BlockType
import com.momentummm.app.data.entity.InAppBlockRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppBlockScreen(
    onNavigateBack: () -> Unit,
    viewModel: InAppBlockViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val groupedRules = rules.groupBy { it.appName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueo Dentro de Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Información sobre la función
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bloqueo de Funciones Específicas",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bloquea funciones como Reels, Shorts y más sin bloquear toda la app",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Lista de reglas agrupadas por app
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                groupedRules.forEach { (appName, appRules) ->
                    item {
                        AppBlockSection(
                            appName = appName,
                            rules = appRules,
                            onToggleRule = { ruleId, enabled ->
                                viewModel.toggleRule(ruleId, enabled)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AppBlockSection(
    appName: String,
    rules: List<InAppBlockRule>,
    onToggleRule: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con icono de la app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = getAppIcon(appName),
                    contentDescription = appName,
                    modifier = Modifier.size(32.dp),
                    tint = getAppColor(appName)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Lista de funciones bloqueables
            rules.forEach { rule ->
                InAppBlockRuleItem(
                    rule = rule,
                    onToggle = { enabled -> onToggleRule(rule.ruleId, enabled) }
                )
                if (rule != rules.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun InAppBlockRuleItem(
    rule: InAppBlockRule,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = getBlockTypeIcon(rule.blockType),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (rule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = rule.featureName,
                    fontWeight = FontWeight.Medium,
                    color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getBlockTypeDescription(rule.blockType),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
fun getAppIcon(appName: String) = when (appName) {
    "Instagram" -> Icons.Default.CameraAlt
    "YouTube" -> Icons.Default.PlayArrow
    "Facebook" -> Icons.Default.Group
    "Snapchat" -> Icons.Default.Chat
    "TikTok" -> Icons.Default.MusicNote
    "X" -> Icons.Default.Tag
    else -> Icons.Default.Apps
}

@Composable
fun getAppColor(appName: String) = when (appName) {
    "Instagram" -> MaterialTheme.colorScheme.secondary
    "YouTube" -> MaterialTheme.colorScheme.error
    "Facebook" -> MaterialTheme.colorScheme.primary
    "Snapchat" -> MaterialTheme.colorScheme.tertiary
    "TikTok" -> MaterialTheme.colorScheme.secondary
    "X" -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun getBlockTypeIcon(blockType: BlockType) = when (blockType) {
    BlockType.REELS -> Icons.Default.VideoLibrary
    BlockType.SHORTS -> Icons.Default.PlayCircle
    BlockType.EXPLORE -> Icons.Default.Explore
    BlockType.SEARCH -> Icons.Default.Search
    BlockType.FOR_YOU -> Icons.Default.Star
    BlockType.DISCOVER -> Icons.Default.Explore
    BlockType.STORIES -> Icons.Default.AutoStories
    BlockType.FEED -> Icons.Default.Feed
    BlockType.CUSTOM -> Icons.Default.Settings
}

fun getBlockTypeDescription(blockType: BlockType) = when (blockType) {
    BlockType.REELS -> "Videos cortos en formato vertical"
    BlockType.SHORTS -> "Videos cortos de YouTube"
    BlockType.EXPLORE -> "Descubre contenido nuevo"
    BlockType.SEARCH -> "Búsqueda dentro de la app"
    BlockType.FOR_YOU -> "Página personalizada de contenido"
    BlockType.DISCOVER -> "Descubre historias y contenido"
    BlockType.STORIES -> "Historias de 24 horas"
    BlockType.FEED -> "Feed principal de la app"
    BlockType.CUSTOM -> "Regla personalizada"
}


package com.momentummm.app.ui.inappblock

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.momentummm.app.accessibility.MomentumAccessibilityService
import com.momentummm.app.data.entity.BlockType
import com.momentummm.app.data.entity.InAppBlockRule
import com.momentummm.app.util.AccessibilityUtils
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppBlockScreen(
    onNavigateBack: () -> Unit,
    viewModel: InAppBlockViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredRules = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) {
            rules
        } else {
            rules.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.featureName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val groupedRules = filteredRules.groupBy { it.packageName }
    val context = LocalContext.current
    var isServiceEnabled by remember {
        mutableStateOf(
            AccessibilityUtils.isAccessibilityServiceEnabled(
                context,
                MomentumAccessibilityService::class.java
            )
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(
                    context,
                    MomentumAccessibilityService::class.java
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Bloqueador de funciones") },
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
            // Hero / Intro
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
                        imageVector = Icons.Default.Shield,
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

            if (!isServiceEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "El monitoreo está desactivado",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Toca aquí para activar",
                                fontSize = 13.sp,
                                color = Color(0xFFEF6C00)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sistema activo",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Lista de reglas agrupadas por app
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar apps o funciones") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (rules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay reglas todavía",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Agrega reglas para bloquear funciones dentro de tus apps favoritas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredRules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sin resultados",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "No encontramos coincidencias para tu búsqueda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    groupedRules.forEach { (packageName, appRules) ->
                        item {
                            AppBlockSection(
                                appName = appRules.firstOrNull()?.appName ?: "",
                                packageName = packageName,
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
}

@Composable
fun AppBlockSection(
    appName: String,
    packageName: String,
    rules: List<InAppBlockRule>,
    onToggleRule: (String, Boolean) -> Unit
) {
    val enabledCount = rules.count { it.isEnabled }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con icono de la app
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                AppIcon(
                    packageName = packageName,
                    appName = appName
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$enabledCount de ${rules.size} activas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text("${rules.size} reglas") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
    ListItem(
        leadingContent = {
            Icon(
                imageVector = getBlockTypeIcon(rule.blockType),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (rule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = rule.featureName,
                fontWeight = FontWeight.Medium,
                color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            Text(
                text = getBlockTypeDescription(rule.blockType),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle
            )
        }
    )
}

@Composable
private fun AppIcon(
    packageName: String,
    appName: String
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
                .padding(6.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = getAppIcon(appName),
            contentDescription = appName,
            modifier = Modifier.size(36.dp),
            tint = getAppColor(appName)
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


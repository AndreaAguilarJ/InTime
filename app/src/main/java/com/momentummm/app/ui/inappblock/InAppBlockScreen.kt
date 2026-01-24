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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.momentummm.app.R
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
                title = { Text(stringResource(R.string.in_app_block_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            stringResource(R.string.in_app_block_back_cd)
                        )
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
                            text = stringResource(R.string.in_app_block_hero_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.in_app_block_hero_subtitle),
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
                                text = stringResource(R.string.in_app_block_monitoring_disabled_title),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = stringResource(R.string.in_app_block_monitoring_disabled_subtitle),
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
                        text = stringResource(R.string.in_app_block_monitoring_active),
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
                placeholder = { Text(stringResource(R.string.in_app_block_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.in_app_block_clear_search)
                            )
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
                        text = stringResource(R.string.in_app_block_empty_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.in_app_block_empty_subtitle),
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
                        text = stringResource(R.string.in_app_block_no_results_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.in_app_block_no_results_subtitle),
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
                        text = stringResource(
                            R.string.in_app_block_enabled_count,
                            enabledCount,
                            rules.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = {
                        Text(stringResource(R.string.in_app_block_rules_count, rules.size))
                    },
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

@Composable
fun getBlockTypeDescription(blockType: BlockType) = when (blockType) {
    BlockType.REELS -> stringResource(R.string.block_type_reels_desc)
    BlockType.SHORTS -> stringResource(R.string.block_type_shorts_desc)
    BlockType.EXPLORE -> stringResource(R.string.block_type_explore_desc)
    BlockType.SEARCH -> stringResource(R.string.block_type_search_desc)
    BlockType.FOR_YOU -> stringResource(R.string.block_type_for_you_desc)
    BlockType.DISCOVER -> stringResource(R.string.block_type_discover_desc)
    BlockType.STORIES -> stringResource(R.string.block_type_stories_desc)
    BlockType.FEED -> stringResource(R.string.block_type_feed_desc)
    BlockType.CUSTOM -> stringResource(R.string.block_type_custom_desc)
}


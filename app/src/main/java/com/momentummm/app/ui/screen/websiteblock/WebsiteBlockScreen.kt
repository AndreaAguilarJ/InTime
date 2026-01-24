package com.momentummm.app.ui.screen.websiteblock

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.R
import com.momentummm.app.data.entity.WebsiteBlock
import com.momentummm.app.data.entity.WebsiteCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteBlockScreen(
    viewModel: WebsiteBlockViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedBlockForDelete by remember { mutableStateOf<WebsiteBlock?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.website_block_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.website_block_back_cd)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(
                            Icons.Filled.Category,
                            contentDescription = stringResource(R.string.website_block_add_category_cd)
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.website_block_add_site_cd)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Card
            item {
                WebsiteBlockStatsCard(stats = uiState.stats)
            }

            // Info Card
            item {
                InfoCard()
            }

            // Group by category
            val groupedBlocks = uiState.websiteBlocks.groupBy { it.category }

            groupedBlocks.forEach { (category, blocks) ->
                item {
                    CategoryHeader(
                        category = category,
                        count = blocks.count { it.isEnabled },
                        onDeleteCategory = {
                            viewModel.deleteBlocksByCategory(category)
                        }
                    )
                }

                items(blocks) { block ->
                    WebsiteBlockItem(
                        block = block,
                        onToggle = { viewModel.toggleBlock(block) },
                        onDelete = { selectedBlockForDelete = block }
                    )
                }
            }

            if (uiState.websiteBlocks.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState(onAddClick = { showAddDialog = true })
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddWebsiteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url, name, category ->
                viewModel.addWebsiteBlock(url, name, category)
                showAddDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onConfirm = { category ->
                viewModel.addPredefinedBlocks(category)
                showCategoryDialog = false
            }
        )
    }

    selectedBlockForDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { selectedBlockForDelete = null },
            title = { Text(stringResource(R.string.website_block_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.website_block_delete_message,
                        block.displayName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBlock(block)
                        selectedBlockForDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.website_block_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBlockForDelete = null }) {
                    Text(stringResource(R.string.website_block_cancel_button))
                }
            }
        )
    }
}

@Composable
private fun WebsiteBlockStatsCard(stats: WebsiteBlockStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = stringResource(R.string.website_block_stat_total),
                    value = stats.totalBlocks.toString(),
                    icon = Icons.Filled.Language
                )
                StatItem(
                    label = stringResource(R.string.website_block_stat_active),
                    value = stats.enabledBlocks.toString(),
                    icon = Icons.Filled.Block
                )
                StatItem(
                    label = stringResource(R.string.website_block_stat_adult),
                    value = stats.adultContentBlocked.toString(),
                    icon = Icons.Filled.Warning
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Column {
                Text(
                    text = stringResource(R.string.website_block_info_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.website_block_info_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: WebsiteCategory,
    count: Int,
    onDeleteCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getCategoryIcon(category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = getCategoryName(category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.website_block_active_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDeleteCategory) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.website_block_delete_category_cd),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun WebsiteBlockItem(
    block: WebsiteBlock,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = block.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = block.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.website_block_delete_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Filled.Language,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.website_block_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.website_block_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Button(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.website_block_add_site_button))
        }
    }
}

private fun getCategoryIcon(category: WebsiteCategory): ImageVector {
    return when (category) {
        WebsiteCategory.ADULT_CONTENT -> Icons.Filled.Warning
        WebsiteCategory.SOCIAL_MEDIA -> Icons.Filled.Group
        WebsiteCategory.ENTERTAINMENT -> Icons.Filled.Movie
        WebsiteCategory.GAMING -> Icons.Filled.SportsEsports
        WebsiteCategory.NEWS -> Icons.Filled.Newspaper
        WebsiteCategory.SHOPPING -> Icons.Filled.ShoppingCart
        WebsiteCategory.CUSTOM -> Icons.Filled.Language
    }
}

@Composable
private fun getCategoryName(category: WebsiteCategory): String {
    return when (category) {
        WebsiteCategory.ADULT_CONTENT -> stringResource(R.string.website_block_category_adult)
        WebsiteCategory.SOCIAL_MEDIA -> stringResource(R.string.website_block_category_social)
        WebsiteCategory.ENTERTAINMENT -> stringResource(R.string.website_block_category_entertainment)
        WebsiteCategory.GAMING -> stringResource(R.string.website_block_category_gaming)
        WebsiteCategory.NEWS -> stringResource(R.string.website_block_category_news)
        WebsiteCategory.SHOPPING -> stringResource(R.string.website_block_category_shopping)
        WebsiteCategory.CUSTOM -> stringResource(R.string.website_block_category_custom)
    }
}

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("Bloqueo de Sitios Web") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Filled.Category, contentDescription = "Agregar categoría")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar sitio")
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
            title = { Text("Eliminar bloqueo") },
            text = { Text("¿Estás seguro de que quieres eliminar el bloqueo para ${block.displayName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBlock(block)
                        selectedBlockForDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBlockForDelete = null }) {
                    Text("Cancelar")
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
                    label = "Total",
                    value = stats.totalBlocks.toString(),
                    icon = Icons.Filled.Language
                )
                StatItem(
                    label = "Activos",
                    value = stats.enabledBlocks.toString(),
                    icon = Icons.Filled.Block
                )
                StatItem(
                    label = "Adultos",
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
                    text = "Sobre el bloqueo de sitios",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Los sitios bloqueados serán interceptados cuando los abras en navegadores compatibles. Puedes desbloquearlos temporalmente desde la notificación de bloqueo.",
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
                text = "($count activos)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDeleteCategory) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar categoría",
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
                        contentDescription = "Eliminar",
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
            text = "No hay sitios bloqueados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Agrega sitios web para bloquear el acceso a contenido que te distrae",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Button(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar sitio")
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

private fun getCategoryName(category: WebsiteCategory): String {
    return when (category) {
        WebsiteCategory.ADULT_CONTENT -> "Contenido Adulto"
        WebsiteCategory.SOCIAL_MEDIA -> "Redes Sociales"
        WebsiteCategory.ENTERTAINMENT -> "Entretenimiento"
        WebsiteCategory.GAMING -> "Juegos"
        WebsiteCategory.NEWS -> "Noticias"
        WebsiteCategory.SHOPPING -> "Compras"
        WebsiteCategory.CUSTOM -> "Personalizado"
    }
}

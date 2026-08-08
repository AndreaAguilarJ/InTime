package com.momentummm.app.ui.screen.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.R
import com.momentummm.app.data.entity.AppCategory
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.ui.system.MomentumDesign
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategoriesScreen(
    viewModel: AppCategoriesViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showAddAppsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message if any
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_screen_title)) },
                navigationIcon = {
                    onBackClick?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    // Botón para inicializar categorías del sistema
                    IconButton(onClick = { viewModel.initializeSystemCategories() }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Auto-detectar categorías")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateCategoryDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.category_create_new)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.categories.isEmpty()) {
            // Estado vacío
            EmptyCategoriesContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreateCategory = { showCreateCategoryDialog = true },
                onAutoDetect = { viewModel.initializeSystemCategories() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info card
                item {
                    InfoCard()
                }
                
                // Categorías
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        usageTime = uiState.categoryUsageTimes[category.id] ?: 0L,
                        availableApps = uiState.availableApps,
                        onEditCategory = {
                            selectedCategory = category
                            showEditCategoryDialog = true
                        },
                        onAddApps = {
                            selectedCategory = category
                            showAddAppsDialog = true
                        },
                        onRemoveApp = { packageName ->
                            viewModel.removeAppFromCategory(category.id, packageName)
                        },
                        onToggleLimit = { enabled ->
                            viewModel.updateCategoryLimit(category.id, category.dailyLimitMinutes, enabled)
                        },
                        onDeleteCategory = {
                            viewModel.deleteCategory(category)
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Espacio para FAB
                }
            }
        }
    }

    // Diálogos
    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCreate = { name, iconName, colorHex, description ->
                viewModel.createCategory(name, iconName, colorHex, description)
                showCreateCategoryDialog = false
            }
        )
    }

    // Se captura la categoría en un `val` local en lugar de usar `!!` sobre el
    // estado: una recomposición que ponga selectedCategory a null entre la
    // comprobación y el uso provocaría NullPointerException.
    selectedCategory?.let { category ->
        if (showEditCategoryDialog) {
            EditCategoryDialog(
                category = category,
                onDismiss = {
                    showEditCategoryDialog = false
                    selectedCategory = null
                },
                onSave = { updatedCategory ->
                    viewModel.updateCategory(updatedCategory)
                    showEditCategoryDialog = false
                    selectedCategory = null
                },
                onUpdateSchedule = { hasSchedule, startHour, startMinute, endHour, endMinute, days ->
                    viewModel.updateCategorySchedule(
                        categoryId = category.id,
                        hasSchedule = hasSchedule,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        daysOfWeek = days
                    )
                }
            )
        }

        if (showAddAppsDialog) {
            AddAppsToCategoryDialog(
                category = category,
                availableApps = uiState.availableApps.filter { app ->
                    !category.containsApp(app.packageName)
                },
                onDismiss = {
                    showAddAppsDialog = false
                    selectedCategory = null
                },
                onAddApps = { packageNames ->
                    packageNames.forEach { packageName ->
                        viewModel.addAppToCategory(category.id, packageName)
                    }
                    showAddAppsDialog = false
                    selectedCategory = null
                }
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.categories_info_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.categories_info_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyCategoriesContent(
    modifier: Modifier = Modifier,
    onCreateCategory: () -> Unit,
    onAutoDetect: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.categories_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.categories_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MomentumButton(
            onClick = onAutoDetect,
            style = ButtonStyle.Primary
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.categories_auto_detect))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(onClick = onCreateCategory) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.category_create_manual))
        }
    }
}

@Composable
private fun CategoryCard(
    category: AppCategory,
    usageTime: Long,
    availableApps: List<AppUsageInfo>,
    onEditCategory: () -> Unit,
    onAddApps: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onToggleLimit: (Boolean) -> Unit,
    onDeleteCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    val usageMinutes = (usageTime / 60000).toInt()
    val progress = if (category.dailyLimitMinutes > 0 && category.isLimitEnabled) {
        (usageMinutes.toFloat() / category.dailyLimitMinutes).coerceIn(0f, 1f)
    } else 0f
    
    val isOverLimit = category.isLimitEnabled && usageMinutes >= category.dailyLimitMinutes
    val apps = category.getPackageNamesList()

    // Semáforo compartido con App Limits: la misma señal debe significar lo mismo
    // en toda la app, así que verde/ámbar/rojo se calcula igual aquí.
    val statusColor = when {
        !category.isLimitEnabled -> categoryColor
        progress >= 1f -> MaterialTheme.momentum.danger
        progress >= 0.8f -> MaterialTheme.momentum.warning
        else -> MaterialTheme.momentum.success
    }

    MomentumCard(
        modifier = modifier.animateContentSize(),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isOverLimit) 1.5.dp else 1.dp,
            color = if (isOverLimit) {
                MaterialTheme.momentum.danger.copy(alpha = MomentumDesign.Alpha.strong)
            } else {
                MaterialTheme.momentum.border
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.iconName),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (category.isSystemCategory) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "${apps.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Toggle límite
                Switch(
                    checked = category.isLimitEnabled,
                    onCheckedChange = onToggleLimit
                )
            }
            
            // Descripción si existe
            if (category.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Límite y progreso
            if (category.isLimitEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usado: ${usageMinutes} min",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Límite: ${category.dailyLimitMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                com.momentummm.app.ui.system.ProgressBar(
                    progress = progress,
                    color = statusColor,
                    height = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOverLimit) {
                    Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraSmall))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            MomentumDesign.Spacing.extraSmall
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.momentum.danger,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_limits_exceeded),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.momentum.danger
                        )
                    }
                }
            }
            
            // Horario de bloqueo
            if (category.hasScheduleLimit) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bloqueo: ${category.getScheduleFormatted()} (${category.getDaysAsText()})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Expansión para ver apps
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Ocultar apps" else "Ver apps (${apps.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Apps list
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (apps.isEmpty()) {
                        Text(
                            text = "No hay apps en esta categoría",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        apps.forEach { packageName ->
                            CategoryAppItem(
                                packageName = packageName,
                                onRemove = { onRemoveApp(packageName) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Botón añadir apps
                    OutlinedButton(
                        onClick = onAddApps,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir apps")
                    }
                }
            }
            
            // Botones de acción
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEditCategory) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                
                if (!category.isSystemCategory) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
    
    // Confirmación de eliminación
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar categoría") },
            text = { Text("¿Estás seguro de que quieres eliminar la categoría '${category.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CategoryAppItem(
    packageName: String,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val appInfo = remember(packageName) {
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            Pair(
                pm.getApplicationLabel(appInfo).toString(),
                pm.getApplicationIcon(appInfo)
            )
        } catch (e: Exception) {
            Pair(packageName, null)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (appInfo.second != null) {
            Image(
                painter = BitmapPainter(appInfo.second!!.toBitmap().asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appInfo.first,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Quitar",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun getCategoryIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "Games" -> Icons.Filled.SportsEsports
        "Social" -> Icons.Filled.People
        "Entertainment" -> Icons.Filled.Movie
        "Productivity" -> Icons.Filled.Work
        "Communication" -> Icons.Filled.Chat
        "Music" -> Icons.Filled.MusicNote
        "Video" -> Icons.Filled.VideoLibrary
        "Shopping" -> Icons.Filled.ShoppingCart
        "News" -> Icons.Filled.Newspaper
        "Health" -> Icons.Filled.FitnessCenter
        "Education" -> Icons.Filled.School
        "Finance" -> Icons.Filled.AccountBalance
        "Travel" -> Icons.Filled.Flight
        "Food" -> Icons.Filled.Restaurant
        else -> Icons.Filled.Category
    }
}

// Diálogos - CreateCategoryDialog, EditCategoryDialog, AddAppsToCategoryDialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, iconName: String, colorHex: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Category") }
    var selectedColor by remember { mutableStateOf("#6200EE") }
    
    val icons = listOf(
        "Category", "Games", "Social", "Entertainment", "Productivity",
        "Communication", "Music", "Video", "Shopping", "News",
        "Health", "Education", "Finance", "Travel", "Food"
    )
    
    val colors = listOf(
        "#6200EE", "#03DAC5", "#FF5722", "#4CAF50", "#2196F3",
        "#FFC107", "#E91E63", "#9C27B0", "#795548", "#607D8B"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = stringResource(R.string.category_create_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.category_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de icono
                Text(
                    text = stringResource(R.string.category_icon_label),
                    style = MaterialTheme.typography.labelLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { iconName ->
                        val isSelected = iconName == selectedIcon
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedIcon = iconName },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getCategoryIcon(iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de color
                Text(
                    text = stringResource(R.string.category_color_label),
                    style = MaterialTheme.typography.labelLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { colorHex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val isSelected = colorHex == selectedColor
                        
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { selectedColor = colorHex },
                            color = color,
                            shape = CircleShape,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                3.dp, MaterialTheme.colorScheme.onSurface
                            ) else null
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    
                    MomentumButton(
                        onClick = { onCreate(name, selectedIcon, selectedColor, description) },
                        enabled = name.isNotBlank(),
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDialog(
    category: AppCategory,
    onDismiss: () -> Unit,
    onSave: (AppCategory) -> Unit,
    onUpdateSchedule: (Boolean, Int, Int, Int, Int, Set<Int>) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var description by remember { mutableStateOf(category.description) }
    var limitMinutes by remember { mutableStateOf(category.dailyLimitMinutes.toString()) }
    var hasSchedule by remember { mutableStateOf(category.hasScheduleLimit) }
    var scheduleStartHour by remember { mutableIntStateOf(category.scheduleStartHour) }
    var scheduleStartMinute by remember { mutableIntStateOf(category.scheduleStartMinute) }
    var scheduleEndHour by remember { mutableIntStateOf(category.scheduleEndHour) }
    var scheduleEndMinute by remember { mutableIntStateOf(category.scheduleEndMinute) }
    var selectedDays by remember { 
        mutableStateOf(category.scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet())
    }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Categoría",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !category.isSystemCategory
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Límite de tiempo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Límite de Tiempo",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = limitMinutes,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            limitMinutes = it
                        }
                    },
                    label = { Text("Límite diario (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bloqueo por horario
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bloqueo por Horario",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hasSchedule,
                        onCheckedChange = { hasSchedule = it }
                    )
                }
                
                AnimatedVisibility(visible = hasSchedule) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Hora inicio
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hora de inicio:")
                                    TextButton(onClick = { showStartTimePicker = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", scheduleStartHour, scheduleStartMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                // Hora fin
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hora de fin:")
                                    TextButton(onClick = { showEndTimePicker = true }) {
                                        Text(
                                            text = String.format("%02d:%02d", scheduleEndHour, scheduleEndMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Días activos:", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        DayOfWeekSelector(
                            selectedDays = selectedDays,
                            onDaysChanged = { selectedDays = it }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    MomentumButton(
                        onClick = {
                            val newLimit = limitMinutes.toIntOrNull() ?: category.dailyLimitMinutes
                            val updatedCategory = category.copy(
                                name = name,
                                description = description,
                                dailyLimitMinutes = newLimit,
                                hasScheduleLimit = hasSchedule,
                                scheduleStartHour = scheduleStartHour,
                                scheduleStartMinute = scheduleStartMinute,
                                scheduleEndHour = scheduleEndHour,
                                scheduleEndMinute = scheduleEndMinute,
                                scheduleDaysOfWeek = selectedDays.joinToString(","),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updatedCategory)
                        },
                        enabled = name.isNotBlank(),
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
    
    // Time pickers
    if (showStartTimePicker) {
        ScheduleTimePickerDialog(
            title = "Hora de Inicio",
            initialHour = scheduleStartHour,
            initialMinute = scheduleStartMinute,
            onConfirm = { hour, minute ->
                scheduleStartHour = hour
                scheduleStartMinute = minute
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    
    if (showEndTimePicker) {
        ScheduleTimePickerDialog(
            title = "Hora de Fin",
            initialHour = scheduleEndHour,
            initialMinute = scheduleEndMinute,
            onConfirm = { hour, minute ->
                scheduleEndHour = hour
                scheduleEndMinute = minute
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val days = listOf(1 to "L", 2 to "M", 3 to "X", 4 to "J", 5 to "V", 6 to "S", 7 to "D")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val isSelected = dayNum in selectedDays
            
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val newDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                        onDaysChanged(newDays)
                    },
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAppsToCategoryDialog(
    category: AppCategory,
    availableApps: List<AppUsageInfo>,
    onDismiss: () -> Unit,
    onAddApps: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    
    val filteredApps = remember(availableApps, searchQuery) {
        if (searchQuery.isBlank()) availableApps
        else availableApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Añadir Apps",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "a ${category.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar app") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${selectedApps.size} apps seleccionadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Lista de apps
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps) { app ->
                        val isSelected = app.packageName in selectedApps
                        
                        AppSelectionItem(
                            app = app,
                            isSelected = isSelected,
                            onToggle = {
                                selectedApps = if (isSelected) {
                                    selectedApps - app.packageName
                                } else {
                                    selectedApps + app.packageName
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    MomentumButton(
                        onClick = { onAddApps(selectedApps.toList()) },
                        enabled = selectedApps.isNotEmpty(),
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Añadir (${selectedApps.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AppUsageInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MomentumDesign.Shapes.card)
            .clickable(onClick = onToggle)
            .padding(MomentumDesign.Spacing.compact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIcon != null) {
            Image(
                painter = BitmapPainter(appIcon.toBitmap().asImageBitmap()),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(MomentumDesign.CornerRadius.small))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(MomentumDesign.CornerRadius.small))
                    .background(MaterialTheme.momentum.surfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.momentum.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))

        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.momentum.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Check circular en vez de casilla cuadrada: el área táctil real es toda la
        // fila, así que el indicador solo tiene que comunicar estado, y un círculo
        // relleno lo hace de un vistazo en una lista larga de apps.
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.momentum.surfaceSunken
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.momentum.border
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

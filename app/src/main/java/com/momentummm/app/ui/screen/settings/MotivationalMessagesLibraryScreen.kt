package com.momentummm.app.ui.screen.settings

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.ui.viewmodel.MotivationalMessagesViewModel
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationalMessagesLibraryScreen(
    onBack: () -> Unit,
    viewModel: MotivationalMessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Todos", "Favoritos", "Personalizados")
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de Mensajes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddCustomDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.a11y_add_message))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddCustomDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.a11y_add_message))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = when (index) {
                                    0 -> "$title (${uiState.allMessages.size})"
                                    1 -> "$title (${uiState.favoriteMessages.size})"
                                    2 -> "$title (${uiState.customMessages.size})"
                                    else -> title
                                }
                            )
                        }
                    )
                }
            }
            
            // Filter chips
            FilterChipsRow(
                selectedCategory = uiState.selectedCategory,
                selectedTone = uiState.selectedTone,
                onCategorySelect = { viewModel.setSelectedCategory(it) },
                onToneSelect = { viewModel.setSelectedTone(it) },
                onClearFilters = { viewModel.clearFilters() }
            )
            
            // Messages list
            when (selectedTab) {
                0 -> MessagesList(
                    messages = uiState.allMessages,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onShare = { shareMessage(context, it) },
                    onDelete = null
                )
                1 -> MessagesList(
                    messages = uiState.favoriteMessages,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onShare = { shareMessage(context, it) },
                    onDelete = null
                )
                2 -> MessagesList(
                    messages = uiState.customMessages,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onShare = { shareMessage(context, it) },
                    onDelete = { viewModel.deleteCustomMessage(it) }
                )
            }
        }
        
        // Add custom message dialog
        if (uiState.showAddCustomDialog) {
            AddCustomMessageDialog(
                content = uiState.customMessageContent,
                emoji = uiState.customMessageEmoji,
                category = uiState.customMessageCategory,
                tone = uiState.customMessageTone,
                isSaving = uiState.isSaving,
                onContentChange = { viewModel.updateCustomMessageContent(it) },
                onEmojiChange = { viewModel.updateCustomMessageEmoji(it) },
                onCategoryChange = { viewModel.updateCustomMessageCategory(it) },
                onToneChange = { viewModel.updateCustomMessageTone(it) },
                onSave = { viewModel.saveCustomMessage() },
                onDismiss = { viewModel.hideAddCustomDialog() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar mensajes...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.a11y_clear))
                }
            }
        },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedCategory: MessageCategory?,
    selectedTone: MessageTone?,
    onCategorySelect: (MessageCategory?) -> Unit,
    onToneSelect: (MessageTone?) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Clear filters chip
        if (selectedCategory != null || selectedTone != null) {
            item {
                AssistChip(
                    onClick = onClearFilters,
                    label = { Text("Limpiar filtros") },
                    leadingIcon = {
                        Icon(Icons.Default.Clear, contentDescription = null, Modifier.size(18.dp))
                    }
                )
            }
        }
        
        // Category chips
        items(MessageCategory.entries.take(5)) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelect(if (selectedCategory == category) null else category)
                },
                label = { Text("${category.emoji} ${category.displayName}") }
            )
        }
    }
}

@Composable
private fun MessagesList(
    messages: List<MotivationalMessage>,
    onToggleFavorite: (String) -> Unit,
    onShare: (MotivationalMessage) -> Unit,
    onDelete: ((String) -> Unit)?
) {
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No hay mensajes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    onToggleFavorite = { onToggleFavorite(message.id) },
                    onShare = { onShare(message) },
                    onDelete = onDelete?.let { { it(message.id) } }
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: MotivationalMessage,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.category.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        message.category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (message.isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.a11y_favorite),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content
            Text(
                text = message.emoji?.let { "$it ${message.content}" } ?: message.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (message.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (message.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (message.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Share button
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.a11y_share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Delete button (only for custom messages)
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.a11y_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Tone badge
            // Etiqueta informativa del tono, no una acción: sin esto TalkBack la
            // anuncia como botón y al pulsarla no ocurre nada.
            val toneLabel = "${message.tone.emoji} ${message.tone.displayName}"
            SuggestionChip(
                onClick = { },
                label = {
                    Text(
                        toneLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clearAndSetSemantics {
                        text = AnnotatedString(toneLabel)
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddCustomMessageDialog(
    content: String,
    emoji: String,
    category: MessageCategory,
    tone: MessageTone,
    isSaving: Boolean,
    onContentChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onCategoryChange: (MessageCategory) -> Unit,
    onToneChange: (MessageTone) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar mensaje personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Message content
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text("Tu mensaje") },
                    placeholder = { Text("Escribe un mensaje motivacional...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                // Emoji selector
                Text("Emoji", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("✨", "🌟", "💪", "🎯", "🚀", "💡", "❤️", "🔥", "⭐", "🌈")) { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { onEmojiChange(e) },
                            label = { Text(e, fontSize = 20.sp) }
                        )
                    }
                }
                
                // Category selector
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MessageCategory.entries.take(6).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text("${cat.emoji} ${cat.displayName}") }
                        )
                    }
                }
                
                // Tone selector
                Text("Tono", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MessageTone.entries.take(4).forEach { t ->
                        FilterChip(
                            selected = tone == t,
                            onClick = { onToneChange(t) },
                            label = { Text("${t.emoji} ${t.displayName}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = content.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun shareMessage(context: android.content.Context, message: MotivationalMessage) {
    val shareText = "${message.emoji ?: ""} ${message.content}\n\n- Momentum 💫"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir mensaje"))
}

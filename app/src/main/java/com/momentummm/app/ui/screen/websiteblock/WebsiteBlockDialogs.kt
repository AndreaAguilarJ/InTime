package com.momentummm.app.ui.screen.websiteblock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.data.entity.WebsiteCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, displayName: String, category: WebsiteCategory) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(WebsiteCategory.CUSTOM) }
    var expandedCategory by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Sitio Web") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL o Dominio") },
                    placeholder = { Text("ejemplo: facebook.com") },
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre para mostrar") },
                    placeholder = { Text("Facebook") },
                    leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = getCategoryName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        WebsiteCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(getCategoryName(category)) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                },
                                leadingIcon = {
                                    Icon(getCategoryIcon(category), contentDescription = null)
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Ejemplos: facebook.com, pornhub.com, youtube.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank() && displayName.isNotBlank()) {
                        onConfirm(url, displayName, selectedCategory)
                    }
                },
                enabled = url.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (WebsiteCategory) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<WebsiteCategory?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Categoría Predefinida") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona una categoría para bloquear múltiples sitios comunes:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                CategoryOption(
                    category = WebsiteCategory.ADULT_CONTENT,
                    title = "Contenido para Adultos",
                    description = "Bloquea los 20+ sitios de contenido adulto más populares",
                    isSelected = selectedCategory == WebsiteCategory.ADULT_CONTENT,
                    onSelect = { selectedCategory = WebsiteCategory.ADULT_CONTENT }
                )

                CategoryOption(
                    category = WebsiteCategory.SOCIAL_MEDIA,
                    title = "Redes Sociales",
                    description = "Facebook, Instagram, Twitter, TikTok, etc.",
                    isSelected = selectedCategory == WebsiteCategory.SOCIAL_MEDIA,
                    onSelect = { selectedCategory = WebsiteCategory.SOCIAL_MEDIA }
                )

                CategoryOption(
                    category = WebsiteCategory.ENTERTAINMENT,
                    title = "Entretenimiento",
                    description = "YouTube, Netflix, streaming, etc.",
                    isSelected = selectedCategory == WebsiteCategory.ENTERTAINMENT,
                    onSelect = { selectedCategory = WebsiteCategory.ENTERTAINMENT }
                )

                CategoryOption(
                    category = WebsiteCategory.GAMING,
                    title = "Juegos",
                    description = "Sitios de juegos online y plataformas",
                    isSelected = selectedCategory == WebsiteCategory.GAMING,
                    onSelect = { selectedCategory = WebsiteCategory.GAMING }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategory?.let { onConfirm(it) }
                },
                enabled = selectedCategory != null
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CategoryOption(
    category: WebsiteCategory,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                getCategoryIcon(category),
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getCategoryIcon(category: WebsiteCategory) = when (category) {
    WebsiteCategory.ADULT_CONTENT -> Icons.Filled.Warning
    WebsiteCategory.SOCIAL_MEDIA -> Icons.Filled.Group
    WebsiteCategory.ENTERTAINMENT -> Icons.Filled.Movie
    WebsiteCategory.GAMING -> Icons.Filled.SportsEsports
    WebsiteCategory.NEWS -> Icons.Filled.Newspaper
    WebsiteCategory.SHOPPING -> Icons.Filled.ShoppingCart
    WebsiteCategory.CUSTOM -> Icons.Filled.Language
}

private fun getCategoryName(category: WebsiteCategory) = when (category) {
    WebsiteCategory.ADULT_CONTENT -> "Contenido Adulto"
    WebsiteCategory.SOCIAL_MEDIA -> "Redes Sociales"
    WebsiteCategory.ENTERTAINMENT -> "Entretenimiento"
    WebsiteCategory.GAMING -> "Juegos"
    WebsiteCategory.NEWS -> "Noticias"
    WebsiteCategory.SHOPPING -> "Compras"
    WebsiteCategory.CUSTOM -> "Personalizado"
}


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
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

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
        title = { Text(stringResource(R.string.web_dialog_add_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.web_dialog_url_label)) },
                    placeholder = { Text("ejemplo: facebook.com") },
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.web_dialog_name_label)) },
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
                        label = { Text(stringResource(R.string.web_dialog_category_label)) },
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
                    text = stringResource(R.string.web_dialog_examples),
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
                Text(stringResource(R.string.web_dialog_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.web_dialog_cancel))
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
        title = { Text(stringResource(R.string.web_dialog_preset_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.web_dialog_preset_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                CategoryOption(
                    category = WebsiteCategory.ADULT_CONTENT,
                    title = stringResource(R.string.web_dialog_preset_adult),
                    description = stringResource(R.string.web_dialog_preset_adult_desc),
                    isSelected = selectedCategory == WebsiteCategory.ADULT_CONTENT,
                    onSelect = { selectedCategory = WebsiteCategory.ADULT_CONTENT }
                )

                CategoryOption(
                    category = WebsiteCategory.SOCIAL_MEDIA,
                    title = stringResource(R.string.web_dialog_preset_social),
                    description = stringResource(R.string.web_dialog_preset_social_desc),
                    isSelected = selectedCategory == WebsiteCategory.SOCIAL_MEDIA,
                    onSelect = { selectedCategory = WebsiteCategory.SOCIAL_MEDIA }
                )

                CategoryOption(
                    category = WebsiteCategory.ENTERTAINMENT,
                    title = stringResource(R.string.web_dialog_preset_entertainment),
                    description = stringResource(R.string.web_dialog_preset_entertainment_desc),
                    isSelected = selectedCategory == WebsiteCategory.ENTERTAINMENT,
                    onSelect = { selectedCategory = WebsiteCategory.ENTERTAINMENT }
                )

                CategoryOption(
                    category = WebsiteCategory.GAMING,
                    title = stringResource(R.string.web_dialog_preset_gaming),
                    description = stringResource(R.string.web_dialog_preset_gaming_desc),
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
                Text(stringResource(R.string.web_dialog_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.web_dialog_cancel))
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

// Duplicado de la función que ya existía en WebsiteBlockScreen.kt: allí sí estaba
// traducida y aquí devolvía español fijo, así que el mismo desplegable mostraba
// categorías en dos idiomas distintos según por dónde se abriera.
// Se usan las mismas claves para que no puedan volver a divergir.
@Composable
private fun getCategoryName(category: WebsiteCategory) = when (category) {
    WebsiteCategory.ADULT_CONTENT -> stringResource(R.string.website_block_category_adult)
    WebsiteCategory.SOCIAL_MEDIA -> stringResource(R.string.website_block_category_social)
    WebsiteCategory.ENTERTAINMENT -> stringResource(R.string.website_block_category_entertainment)
    WebsiteCategory.GAMING -> stringResource(R.string.website_block_category_gaming)
    WebsiteCategory.NEWS -> stringResource(R.string.website_block_category_news)
    WebsiteCategory.SHOPPING -> stringResource(R.string.website_block_category_shopping)
    WebsiteCategory.CUSTOM -> stringResource(R.string.website_block_category_custom)
}


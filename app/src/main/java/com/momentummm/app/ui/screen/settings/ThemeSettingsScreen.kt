package com.momentummm.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R
import com.momentummm.app.data.manager.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBackClick: () -> Unit,
    themeManager: ThemeManager = ThemeManager(LocalContext.current)
) {
    val scope = rememberCoroutineScope()
    
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
    val useDynamicColor by themeManager.useDynamicColor.collectAsState(initial = true)
    val customPrimaryColor by themeManager.customPrimaryColor.collectAsState(initial = null)
    
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
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
                
                Text(
                    text = stringResource(R.string.theme_settings_title),
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
            // Theme Mode Section
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
                        Text(
                            text = stringResource(R.string.theme_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ThemeManager.ThemeMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        scope.launch {
                                            themeManager.setThemeMode(mode)
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = {
                                        scope.launch {
                                            themeManager.setThemeMode(mode)
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = when (mode) {
                                            ThemeManager.ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                            ThemeManager.ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                            ThemeManager.ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Text(
                                        text = when (mode) {
                                            ThemeManager.ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system_desc)
                                            ThemeManager.ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light_desc)
                                            ThemeManager.ThemeMode.DARK -> stringResource(R.string.theme_mode_dark_desc)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Dynamic Color Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme_dynamic_colors_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = stringResource(R.string.theme_dynamic_colors_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = useDynamicColor,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    themeManager.setUseDynamicColor(enabled)
                                }
                            }
                        )
                    }
                }
            }
            
            // Custom Color Section
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
                        Text(
                            text = stringResource(R.string.theme_custom_primary_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = stringResource(R.string.theme_custom_primary_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Color presets
                        val colorPresets = listOf(
                            "#6366F1" to stringResource(R.string.color_indigo),
                            "#8B5CF6" to stringResource(R.string.color_violet),
                            "#06B6D4" to stringResource(R.string.color_cyan),
                            "#10B981" to stringResource(R.string.color_emerald),
                            "#F59E0B" to stringResource(R.string.color_amber),
                            "#EF4444" to stringResource(R.string.color_red),
                            "#EC4899" to stringResource(R.string.color_pink),
                            "#8B5A2B" to stringResource(R.string.color_brown)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(colorPresets) { (colorHex, colorName) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                themeManager.setCustomPrimaryColor(colorHex)
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = colorName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    if (customPrimaryColor == colorHex) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.action_selected),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Reset to default button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    themeManager.setCustomPrimaryColor(null)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.theme_reset_default))
                        }
                    }
                }
            }
            
            // Preview Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.theme_preview_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.theme_preview_primary))
                            }
                            
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.theme_preview_secondary))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = 0.7f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
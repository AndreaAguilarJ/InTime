package com.momentummm.app.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.widget.LifeWeeksWidget
import com.momentummm.app.widget.QuoteWidget
import com.momentummm.app.widget.YearProgressWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSetupScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Colores para los widgets de vida en semanas
    val livedWeeksColor by UserPreferencesRepository.getLivedWeeksColorFlow(context)
        .collectAsState(initial = "#4CAF50")
    val futureWeeksColor by UserPreferencesRepository.getFutureWeeksColorFlow(context)
        .collectAsState(initial = "#E0E0E0")
    
    // Lista de colores disponibles
    val availableColors = remember {
        listOf(
            ColorOption("#4CAF50", "Verde", Color(0xFF4CAF50)),
            ColorOption("#2196F3", "Azul", Color(0xFF2196F3)),
            ColorOption("#9C27B0", "Púrpura", Color(0xFF9C27B0)),
            ColorOption("#FF5722", "Naranja", Color(0xFFFF5722)),
            ColorOption("#E91E63", "Rosa", Color(0xFFE91E63)),
            ColorOption("#00BCD4", "Cian", Color(0xFF00BCD4)),
            ColorOption("#FFC107", "Ámbar", Color(0xFFFFC107)),
            ColorOption("#795548", "Marrón", Color(0xFF795548)),
            ColorOption("#607D8B", "Gris Azulado", Color(0xFF607D8B)),
            ColorOption("#3F51B5", "Índigo", Color(0xFF3F51B5)),
            ColorOption("#009688", "Teal", Color(0xFF009688)),
            ColorOption("#F44336", "Rojo", Color(0xFFF44336))
        )
    }
    
    val grayColors = remember {
        listOf(
            ColorOption("#E0E0E0", "Gris Claro", Color(0xFFE0E0E0)),
            ColorOption("#BDBDBD", "Gris Medio", Color(0xFFBDBDBD)),
            ColorOption("#9E9E9E", "Gris", Color(0xFF9E9E9E)),
            ColorOption("#757575", "Gris Oscuro", Color(0xFF757575)),
            ColorOption("#F5F5F5", "Casi Blanco", Color(0xFFF5F5F5)),
            ColorOption("#EEEEEE", "Gris muy claro", Color(0xFFEEEEEE))
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Widgets") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información sobre widgets
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Widgets Disponibles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Mantén presionada la pantalla de inicio para agregar widgets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Widgets disponibles
            item {
                Text(
                    text = "Widgets Disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = "Mi Vida en Semanas",
                    description = "Visualización de tu vida dividida en semanas",
                    icon = Icons.Default.CalendarMonth
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = "Cita Motivacional",
                    description = "Una frase inspiradora que cambia cada día",
                    icon = Icons.Default.FormatQuote
                )
            }
            
            item {
                WidgetPreviewCard(
                    title = "Progreso del Año",
                    description = "Porcentaje del año transcurrido",
                    icon = Icons.Default.TrendingUp
                )
            }
            
            // Personalización de colores - Semanas vividas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Color de Semanas Vividas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Color para las semanas que ya has vivido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableColors) { colorOption ->
                        ColorCircle(
                            colorOption = colorOption,
                            isSelected = livedWeeksColor == colorOption.hex,
                            onClick = {
                                coroutineScope.launch {
                                    UserPreferencesRepository.setWidgetColors(
                                        context,
                                        colorOption.hex,
                                        futureWeeksColor
                                    )
                                    // Actualizar widgets
                                    LifeWeeksWidget().updateAll(context)
                                }
                            }
                        )
                    }
                }
            }
            
            // Personalización de colores - Semanas futuras
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Color de Semanas Futuras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Color para las semanas que aún no has vivido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(grayColors + availableColors) { colorOption ->
                        ColorCircle(
                            colorOption = colorOption,
                            isSelected = futureWeeksColor == colorOption.hex,
                            onClick = {
                                coroutineScope.launch {
                                    UserPreferencesRepository.setWidgetColors(
                                        context,
                                        livedWeeksColor,
                                        colorOption.hex
                                    )
                                    // Actualizar widgets
                                    LifeWeeksWidget().updateAll(context)
                                }
                            }
                        )
                    }
                }
            }
            
            // Botón para actualizar widgets manualmente
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            LifeWeeksWidget().updateAll(context)
                            QuoteWidget().updateAll(context)
                            YearProgressWidget().updateAll(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualizar Todos los Widgets")
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class ColorOption(
    val hex: String,
    val name: String,
    val color: Color
)

@Composable
private fun ColorCircle(
    colorOption: ColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colorOption.color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seleccionado",
                tint = if (colorOption.color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Extensión para calcular luminancia
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

package com.momentummm.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.R
import com.momentummm.app.ui.viewmodel.LifeWeeksViewModel
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeWeeksScreen(
    viewModel: LifeWeeksViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.my_life_in_weeks),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Show error if no birth date is configured
        if (!uiState.hasBirthDate) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Fecha de nacimiento no configurada",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Para ver tu vida en semanas, necesitas configurar tu fecha de nacimiento en la configuración de tu cuenta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Navigate to settings */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Ir a Configuración")
                        }
                    }
                }
            }
        } else {
            // Show life weeks data if birth date is configured
            uiState.lifeWeeksData?.let { data ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${data.weeksLived}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.weeks_lived),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${data.weeksRemaining}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.weeks_remaining),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider()

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Edad actual: ${data.currentAge} años",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "Progreso de vida: ${String.format("%.1f", data.progressPercentage)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = data.progressPercentage / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    // Life grid visualization
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Tu Vida en Semanas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.paddingFromBaseline(bottom = 16.dp)
                            )

                            Text(
                                text = "Cada cuadrito representa una semana de vida. Las semanas vividas están en color, las futuras en gris.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Protección contra parsing de color inválido
                            val livedColorParsed = try {
                                Color(android.graphics.Color.parseColor(uiState.userSettings?.livedWeeksColor ?: "#6366F1"))
                            } catch (e: Exception) { Indigo500 }
                            val futureColorParsed = try {
                                Color(android.graphics.Color.parseColor(uiState.userSettings?.futureWeeksColor ?: "#E5E7EB"))
                            } catch (e: Exception) { Neutral200 }

                            LifeWeeksGrid(
                                weeksLived = data.weeksLived,
                                livedColor = livedColorParsed,
                                futureColor = futureColorParsed
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showColorPicker() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.customize_colors))
                        }

                        OutlinedButton(
                            onClick = {
                                uiState.userSettings?.let { settings ->
                                    try {
                                        val bitmap = com.momentummm.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                            context = context,
                                            weeksLived = data.weeksLived,
                                            livedColor = android.graphics.Color.parseColor(settings.livedWeeksColor ?: "#6366F1"),
                                            futureColor = android.graphics.Color.parseColor(settings.futureWeeksColor ?: "#E5E7EB"),
                                            backgroundColor = android.graphics.Color.parseColor(settings.backgroundColor ?: "#1F2937")
                                        )
                                        com.momentummm.app.util.WallpaperGenerator.saveToGallery(context, bitmap)
                                    } catch (e: Exception) {
                                        android.util.Log.e("LifeWeeksScreen", "Error generating wallpaper", e)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_to_gallery))
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            uiState.userSettings?.let { settings ->
                                try {
                                    val bitmap = com.momentummm.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                        context = context,
                                        weeksLived = data.weeksLived,
                                        livedColor = android.graphics.Color.parseColor(settings.livedWeeksColor ?: "#6366F1"),
                                        futureColor = android.graphics.Color.parseColor(settings.futureWeeksColor ?: "#E5E7EB"),
                                        backgroundColor = android.graphics.Color.parseColor(settings.backgroundColor ?: "#1F2937")
                                    )
                                    com.momentummm.app.util.WallpaperGenerator.setAsWallpaper(context, bitmap)
                                } catch (e: Exception) {
                                    android.util.Log.e("LifeWeeksScreen", "Error setting wallpaper", e)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.set_as_wallpaper))
                    }
                }
            }
        }
    }
}

@Composable
fun LifeWeeksGrid(
    weeksLived: Int,
    livedColor: Color,
    futureColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(52f / 80f) // 52 weeks wide, 80 years tall
    ) {
        drawLifeWeeksGrid(
            weeksLived = weeksLived,
            livedColor = livedColor,
            futureColor = futureColor
        )
    }
}

private fun DrawScope.drawLifeWeeksGrid(
    weeksLived: Int,
    livedColor: Color,
    futureColor: Color
) {
    val totalWeeks = 4160 // 80 years * 52 weeks
    val weeksPerRow = 52
    val totalRows = 80
    
    val cellWidth = size.width / weeksPerRow
    val cellHeight = size.height / totalRows
    val padding = 1.dp.toPx()
    
    for (week in 0 until totalWeeks) {
        val row = week / weeksPerRow
        val col = week % weeksPerRow
        
        val x = col * cellWidth + padding
        val y = row * cellHeight + padding
        val cellSize = minOf(cellWidth - padding * 2, cellHeight - padding * 2)
        
        val color = if (week < weeksLived) livedColor else futureColor
        
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x, y),
            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
        )
    }
}
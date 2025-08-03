package com.momentum.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentum.app.R
import com.momentum.app.ui.viewmodel.LifeWeeksViewModel
import com.momentum.app.util.LifeWeeksCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeWeeksScreen(
    viewModel: LifeWeeksViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

        item {
            uiState.lifeWeeksData?.let { data ->
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
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = stringResource(R.string.weeks_lived),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${data.weeksRemaining}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = stringResource(R.string.weeks_remaining),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Edad actual: ${data.currentAge} años",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Progreso: ${String.format("%.1f", data.progressPercentage)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            // Life grid visualization
            uiState.lifeWeeksData?.let { data ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tu Vida en Semanas",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.paddingFromBaseline(bottom = 16.dp)
                        )
                        
                        LifeWeeksGrid(
                            weeksLived = data.weeksLived,
                            livedColor = Color(android.graphics.Color.parseColor(uiState.userSettings?.livedWeeksColor ?: "#6366F1")),
                            futureColor = Color(android.graphics.Color.parseColor(uiState.userSettings?.futureWeeksColor ?: "#E5E7EB"))
                        )
                    }
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
                        uiState.lifeWeeksData?.let { data ->
                            uiState.userSettings?.let { settings ->
                                val bitmap = com.momentum.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                    context = context,
                                    weeksLived = data.weeksLived,
                                    livedColor = android.graphics.Color.parseColor(settings.livedWeeksColor),
                                    futureColor = android.graphics.Color.parseColor(settings.futureWeeksColor),
                                    backgroundColor = android.graphics.Color.parseColor(settings.backgroundColor)
                                )
                                com.momentum.app.util.WallpaperGenerator.saveToGallery(context, bitmap)
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
                    uiState.lifeWeeksData?.let { data ->
                        uiState.userSettings?.let { settings ->
                            val bitmap = com.momentum.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                context = context,
                                weeksLived = data.weeksLived,
                                livedColor = android.graphics.Color.parseColor(settings.livedWeeksColor),
                                futureColor = android.graphics.Color.parseColor(settings.futureWeeksColor),
                                backgroundColor = android.graphics.Color.parseColor(settings.backgroundColor)
                            )
                            com.momentum.app.util.WallpaperGenerator.setAsWallpaper(context, bitmap)
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
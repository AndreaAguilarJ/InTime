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
import com.momentummm.app.ui.component.LifeWeeksGrid
import com.momentummm.app.R
import com.momentummm.app.ui.viewmodel.LifeWeeksViewModel
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.util.WallpaperGenerator
import com.momentummm.app.ui.theme.*
import android.widget.Toast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeWeeksScreen(
    viewModel: LifeWeeksViewModel,
    onNavigateToAccountSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    /** Antes estos botones no decían nada y generaban la imagen en el hilo principal. */
    fun runWallpaperAction(data: com.momentummm.app.util.LifeWeeksData, setAsWallpaper: Boolean) {
        val settings = uiState.userSettings ?: return
        val spec = WallpaperGenerator.specFor(
            data,
            settings.livedWeeksColor,
            settings.futureWeeksColor,
            settings.backgroundColor,
        )
        coroutineScope.launch {
            val ok = if (setAsWallpaper) {
                WallpaperGenerator.setAsWallpaper(context, spec)
            } else {
                WallpaperGenerator.saveToGallery(context, spec)
            }
            val message = when {
                setAsWallpaper && ok -> R.string.life_wallpaper_set
                setAsWallpaper -> R.string.life_wallpaper_error
                ok -> R.string.life_weeks_saved_gallery
                else -> R.string.life_weeks_gallery_error
            }
            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
        }
    }

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
                            text = stringResource(R.string.life_weeks_need_birthdate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToAccountSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.life_weeks_go_settings))
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
                                text = stringResource(R.string.life_weeks_current_age, data.currentAge),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = stringResource(R.string.life_weeks_progress, String.format("%.1f", data.progressPercentage)),
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
                                text = stringResource(R.string.life_weeks_legend),
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
                                futureColor = futureColorParsed,
                                totalWeeks = data.totalWeeks
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
                            onClick = { runWallpaperAction(data, setAsWallpaper = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_to_gallery))
                        }
                    }
                }

                item {
                    Button(
                        onClick = { runWallpaperAction(data, setAsWallpaper = true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.set_as_wallpaper))
                    }
                }
            }
        }
    }
}

// La cuadrícula la dibuja ahora el componente compartido con iOS
// (com.momentummm.app.ui.component.LifeWeeksGrid), para que las dos plataformas
// pinten la misma rejilla y respeten la expectativa de vida configurada.
package com.momentummm.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.LocalContext
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.momentummm.app.MainActivity
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.util.LifeWeeksCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.momentummm.app.R

/**
 * Widget de Vida en Semanas - Muestra una cuadrícula visual de las semanas vividas
 * con los colores configurados por el usuario
 */
class LifeWeeksWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "LifeWeeksWidget"
        
        /**
         * Actualiza todos los widgets de vida en semanas
         */
        suspend fun updateAllWidgets(context: Context) {
            try {
                LifeWeeksWidget().updateAll(context)
                Log.d(TAG, "All LifeWeeksWidgets updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
            }
        }
        
        /**
         * Notifica al sistema que hay que actualizar los widgets
         */
        fun requestUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LifeWeeksWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, LifeWeeksWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    data class WidgetData(
        val lifeWeeksData: LifeWeeksCalculator.LifeWeeksData?,
        val livedWeeksColor: Color,
        val futureWeeksColor: Color,
        val backgroundColor: Color
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data before providing content with timeout to prevent ANR
        val widgetData = withTimeoutOrNull(3000L) {
            try {
                // Obtener datos de semanas de vida
                val database = AppDatabase.getDatabase(context)
                val userSettings = withContext(Dispatchers.IO) {
                    database.userDao().getUserSettingsSync()
                }

                val lifeWeeksData = userSettings?.birthDate?.let { birthDate ->
                    LifeWeeksCalculator.calculateLifeWeeks(birthDate)
                }

                // Obtener colores configurados por el usuario
                val (livedColorHex, futureColorHex) = withContext(Dispatchers.IO) {
                    UserPreferencesRepository.getWidgetColors(context)
                }

                val livedColor = parseColorSafe(livedColorHex ?: userSettings?.livedWeeksColor ?: "#10B981")
                val futureColor = parseColorSafe(futureColorHex ?: userSettings?.futureWeeksColor ?: "#E5E7EB")
                val backgroundColor = Color(0xFF1A1A2E) // Fondo oscuro elegante

                WidgetData(
                    lifeWeeksData = lifeWeeksData,
                    livedWeeksColor = livedColor,
                    futureWeeksColor = futureColor,
                    backgroundColor = backgroundColor
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading widget data", e)
                null
            }
        }

        provideContent {
            LifeWeeksWidgetContent(widgetData ?: WidgetData(
                lifeWeeksData = null,
                livedWeeksColor = Color(0xFF10B981),
                futureWeeksColor = Color(0xFFE5E7EB),
                backgroundColor = Color(0xFF1A1A2E)
            ))
        }
    }

    private fun parseColorSafe(colorHex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF10B981) // Color por defecto
        }
    }

    @Composable
    private fun LifeWeeksWidgetContent(data: WidgetData) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(data.backgroundColor)
                .padding(12.dp)
                .clickable(actionStartActivity(Intent().apply {
                    setClassName("com.momentummm.app", "com.momentummm.app.MainActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (data.lifeWeeksData != null) {
                // Título
                Text(
                    text = context.getString(R.string.widget_life_title),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Cuadrícula de semanas (visualización compacta)
                LifeWeeksGrid(
                    weeksLived = data.lifeWeeksData.weeksLived,
                    livedColor = data.livedWeeksColor,
                    futureColor = data.futureWeeksColor
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Estadísticas en fila
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Semanas vividas
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.lifeWeeksData.weeksLived}",
                            style = TextStyle(
                                color = ColorProvider(data.livedWeeksColor),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "vividas",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFAAAAAA)),
                                fontSize = 9.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Separador
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFF333333))
                    ) {}

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Semanas restantes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.lifeWeeksData.weeksRemaining}",
                            style = TextStyle(
                                color = ColorProvider(data.futureWeeksColor),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "restantes",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFAAAAAA)),
                                fontSize = 9.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Separador
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFF333333))
                    ) {}

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // Edad
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.lifeWeeksData.currentAge}",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = context.getString(R.string.widget_years),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFAAAAAA)),
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Barra de progreso
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(4.dp)
                            .background(data.futureWeeksColor)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxHeight()
                                .background(data.livedWeeksColor)
                        ) {}
                    }
                }
                
                Text(
                    text = context.getString(R.string.widget_percent_of_life, String.format("%.1f", data.lifeWeeksData.progressPercentage)),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF888888)),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                )

            } else {
                // Estado cuando no hay fecha configurada
                Spacer(modifier = GlanceModifier.height(16.dp))
                
                Text(
                    text = "📅",
                    style = TextStyle(
                        fontSize = 48.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Configura tu fecha",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = "de nacimiento en la app",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFAAAAAA)),
                        fontSize = 12.sp
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = context.getString(R.string.widget_tap_to_open),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF10B981)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }

    /**
     * Cuadrícula visual de semanas de vida
     * Muestra una representación compacta de las 4160 semanas (80 años)
     */
    @Composable
    private fun LifeWeeksGrid(
        weeksLived: Int,
        livedColor: Color,
        futureColor: Color
    ) {
        // Mostrar una cuadrícula compacta (10 filas x 26 columnas = 260 puntos representativos)
        // Cada punto representa ~16 semanas
        val rows = 10
        val cols = 26
        val weeksPerDot = 16 // Cada punto representa ~16 semanas
        
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(rows) { row ->
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    repeat(cols) { col ->
                        val dotIndex = row * cols + col
                        val weeksForThisDot = dotIndex * weeksPerDot
                        val isLived = weeksForThisDot < weeksLived
                        
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .padding(1.dp)
                                .background(if (isLived) livedColor else futureColor)
                        ) {}
                    }
                }
            }
        }
    }
}
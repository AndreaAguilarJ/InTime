package com.momentummm.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.LocalSize
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.ui.BirthDateActivity
import com.momentummm.app.ui.WidgetLaunchActivity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

class LifeWeeksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeWeeksWidgetProvider()
}

class LifeWeeksWidgetProvider : GlanceAppWidget() {

    // Habilitar soporte para múltiples tamaños
    override val sizeMode = SizeMode.Responsive(
        setOf(
            // Pequeño (2x2)
            androidx.glance.appwidget.LocalSize(120.dp, 120.dp),
            // Mediano (3x3)
            androidx.glance.appwidget.LocalSize(180.dp, 180.dp),
            // Grande (4x4)
            androidx.glance.appwidget.LocalSize(250.dp, 280.dp),
            // Extra grande (4x5+)
            androidx.glance.appwidget.LocalSize(280.dp, 400.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Obtener DOB y colores desde DataStore (si existen)
        val dobIso: String? = UserPreferencesRepository.getDobIso(context)
        val dob: LocalDate? = dobIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val (livedHex, futureHex) = UserPreferencesRepository.getWidgetColors(context)
        val livedColorInt = parseColorOrDefault(livedHex, 0xFF10B981.toInt())
        val futureColorInt = parseColorOrDefault(futureHex, 0xFF374151.toInt())

        val totalWeeks = 80 * 52 // 80 años
        val today = LocalDate.now()
        val weeksLived = dob?.let { ChronoUnit.WEEKS.between(it, today).toInt().coerceAtLeast(0) } ?: 0
        val weeksRemaining = (totalWeeks - weeksLived).coerceAtLeast(0)
        val ageYears = dob?.let { ChronoUnit.YEARS.between(it, today).toInt() } ?: 0
        val progressPercent = if (totalWeeks > 0) (weeksLived.toFloat() / totalWeeks * 100).toInt() else 0

        provideContent {
            val size = LocalSize.current
            val isSmall = size.width < 150.dp || size.height < 150.dp
            val isMedium = size.width < 240.dp || size.height < 240.dp

            LifeWeeksWidgetContent(
                hasDob = dob != null,
                weeksLived = weeksLived,
                weeksRemaining = weeksRemaining,
                ageYears = ageYears,
                progressPercent = progressPercent,
                livedColorInt = livedColorInt,
                futureColorInt = futureColorInt,
                isSmall = isSmall,
                isMedium = isMedium && !isSmall,
                totalWeeks = totalWeeks
            )
        }
    }

    @Composable
    private fun LifeWeeksWidgetContent(
        hasDob: Boolean,
        weeksLived: Int,
        weeksRemaining: Int,
        ageYears: Int,
        progressPercent: Int,
        livedColorInt: Int,
        futureColorInt: Int,
        isSmall: Boolean,
        isMedium: Boolean,
        totalWeeks: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    ColorProvider(
                        day = Color(0xFFFAFAFA),
                        night = Color(0xFF0A0A0A)
                    )
                )
                .padding(if (isSmall) 8.dp else 12.dp)
                .let { base ->
                    if (hasDob) base.clickable(actionStartActivity<WidgetLaunchActivity>())
                    else base.clickable(actionStartActivity<BirthDateActivity>())
                },
            contentAlignment = Alignment.Center
        ) {
            if (!hasDob) {
                // Vista sin configurar
                NoDataView(isSmall = isSmall)
            } else {
                // Vista con datos según el tamaño
                when {
                    isSmall -> SmallWidgetView(weeksLived, weeksRemaining, progressPercent)
                    isMedium -> MediumWidgetView(weeksLived, weeksRemaining, ageYears, progressPercent)
                    else -> LargeWidgetView(
                        weeksLived, weeksRemaining, ageYears, progressPercent,
                        livedColorInt, futureColorInt, totalWeeks
                    )
                }
            }
        }
    }

    @Composable
    private fun NoDataView(isSmall: Boolean) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Text(
                text = "⏳",
                style = TextStyle(
                    fontSize = if (isSmall) 32.sp else 48.sp
                )
            )

            if (!isSmall) {
                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Configura tu",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(
                            day = Color(0xFF374151),
                            night = Color(0xFFD1D5DB)
                        )
                    )
                )

                Text(
                    text = "fecha de nacimiento",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(
                            day = Color(0xFF6B7280),
                            night = Color(0xFF9CA3AF)
                        )
                    )
                )
            }
        }
    }

    @Composable
    private fun SmallWidgetView(
        weeksLived: Int,
        weeksRemaining: Int,
        progressPercent: Int
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Text(
                text = "$progressPercent%",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(
                        day = Color(0xFF10B981),
                        night = Color(0xFF34D399)
                    )
                )
            )

            Text(
                text = "vivido",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(
                        day = Color(0xFF6B7280),
                        night = Color(0xFF9CA3AF)
                    )
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Mini barra de progreso
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        ColorProvider(
                            day = Color(0xFFE5E7EB),
                            night = Color(0xFF374151)
                        )
                    )
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth(progressPercent / 100f)
                        .height(8.dp)
                        .background(
                            ColorProvider(
                                day = Color(0xFF10B981),
                                night = Color(0xFF34D399)
                            )
                        )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${weeksLived / 52}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(
                            day = Color(0xFF111827),
                            night = Color(0xFFF9FAFB)
                        )
                    )
                )
                Text(
                    text = " años",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(
                            day = Color(0xFF6B7280),
                            night = Color(0xFF9CA3AF)
                        )
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumWidgetView(
        weeksLived: Int,
        weeksRemaining: Int,
        ageYears: Int,
        progressPercent: Int
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Text(
                text = "Tu Vida en Semanas",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(
                        day = Color(0xFF111827),
                        night = Color(0xFFF9FAFB)
                    ),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Número principal de semanas vividas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(
                        ColorProvider(
                            day = Color(0xFFECFDF5),
                            night = Color(0xFF064E3B)
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = weeksLived.toString(),
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(
                            day = Color(0xFF10B981),
                            night = Color(0xFF34D399)
                        )
                    )
                )
                Text(
                    text = "semanas vividas",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(
                            day = Color(0xFF059669),
                            night = Color(0xFF6EE7B7)
                        )
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = weeksRemaining.toString(),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(
                                day = Color(0xFF6B7280),
                                night = Color(0xFF9CA3AF)
                            )
                        )
                    )
                    Text(
                        text = "restantes",
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(
                                day = Color(0xFF9CA3AF),
                                night = Color(0xFF6B7280)
                            )
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(
                            ColorProvider(
                                day = Color(0xFFE5E7EB),
                                night = Color(0xFF374151)
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = "$ageYears años",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(
                                day = Color(0xFF111827),
                                night = Color(0xFFF9FAFB)
                            )
                        )
                    )
                    Text(
                        text = "$progressPercent%",
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(
                                day = Color(0xFF9CA3AF),
                                night = Color(0xFF6B7280)
                            )
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun LargeWidgetView(
        weeksLived: Int,
        weeksRemaining: Int,
        ageYears: Int,
        progressPercent: Int,
        livedColorInt: Int,
        futureColorInt: Int,
        totalWeeks: Int
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Text(
                text = "Tu Vida en Semanas",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(
                        day = Color(0xFF111827),
                        night = Color(0xFFF9FAFB)
                    )
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Stats cards
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatCard(
                    value = weeksLived.toString(),
                    label = "vividas",
                    color = ColorProvider(
                        day = Color(0xFF10B981),
                        night = Color(0xFF34D399)
                    ),
                    bgColor = ColorProvider(
                        day = Color(0xFFECFDF5),
                        night = Color(0xFF064E3B)
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                StatCard(
                    value = weeksRemaining.toString(),
                    label = "restantes",
                    color = ColorProvider(
                        day = Color(0xFF6B7280),
                        night = Color(0xFF9CA3AF)
                    ),
                    bgColor = ColorProvider(
                        day = Color(0xFFF3F4F6),
                        night = Color(0xFF1F2937)
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                StatCard(
                    value = "$ageYears",
                    label = "años",
                    color = ColorProvider(
                        day = Color(0xFF3B82F6),
                        night = Color(0xFF60A5FA)
                    ),
                    bgColor = ColorProvider(
                        day = Color(0xFFEFF6FF),
                        night = Color(0xFF1E3A8A)
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Barra de progreso mejorada
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            ColorProvider(
                                day = Color(0xFFE5E7EB),
                                night = Color(0xFF374151)
                            )
                        )
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(progressPercent / 100f)
                            .height(12.dp)
                            .background(
                                ColorProvider(
                                    day = Color(0xFF10B981),
                                    night = Color(0xFF34D399)
                                )
                            )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "$progressPercent% de tu vida vivida",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(
                            day = Color(0xFF6B7280),
                            night = Color(0xFF9CA3AF)
                        )
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Grid de semanas optimizado
            val gridBitmap = drawLifeWeeksBitmap(
                weeksLived = weeksLived,
                totalWeeks = totalWeeks,
                cols = 52,
                rows = 80,
                cellSizePx = 4,
                gapPx = 1,
                livedColor = livedColorInt,
                futureColor = futureColorInt
            )

            Image(
                provider = ImageProvider(gridBitmap),
                contentDescription = "Grilla de semanas de vida",
                modifier = GlanceModifier
                    .size(260.dp, 200.dp)
            )
        }
    }

    @Composable
    private fun StatCard(
        value: String,
        label: String,
        color: ColorProvider,
        bgColor: ColorProvider
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier
                .defaultWeight()
                .background(bgColor)
                .padding(8.dp)
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = color
                )
            )
        }
    }
}

private fun parseColorOrDefault(hex: String?, defaultColor: Int): Int {
    return try {
        if (hex.isNullOrBlank()) defaultColor else android.graphics.Color.parseColor(hex)
    } catch (_: Exception) {
        defaultColor
    }
}

private fun drawLifeWeeksBitmap(
    weeksLived: Int,
    totalWeeks: Int,
    cols: Int,
    rows: Int,
    cellSizePx: Int,
    gapPx: Int,
    livedColor: Int,
    futureColor: Int
): Bitmap {
    val width = cols * cellSizePx + (cols - 1) * gapPx
    val height = rows * cellSizePx + (rows - 1) * gapPx
    val bitmap = Bitmap.createBitmap(max(width, 1), max(height, 1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val livedPaint = Paint().apply {
        color = livedColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val remainingPaint = Paint().apply {
        color = futureColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    var weekIndex = 0
    outer@ for (r in 0 until rows) {
        for (c in 0 until cols) {
            val left = c * (cellSizePx + gapPx).toFloat()
            val top = r * (cellSizePx + gapPx).toFloat()
            val rect = RectF(left, top, left + cellSizePx, top + cellSizePx)
            canvas.drawRoundRect(rect, 1f, 1f, if (weekIndex < weeksLived) livedPaint else remainingPaint)
            weekIndex++
            if (weekIndex >= totalWeeks) break@outer
        }
    }

    return bitmap
}

data class LifeWeeksData(
    val totalWeeks: Int,
    val weeksLived: Int,
    val weeksRemaining: Int,
    val currentAge: Int,
    val progressPercentage: Float
)
package com.momentummm.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.momentummm.app.MainActivity
import java.time.LocalDate

class YearProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = calculateYearProgress()

        provideContent {
            YearProgressContent(data)
        }
    }

    private data class YearProgressData(
        val year: Int,
        val dayOfYear: Int,
        val daysRemaining: Int,
        val percentComplete: Float
    )

    private fun calculateYearProgress(): YearProgressData {
        val today = LocalDate.now()
        val year = today.year
        val dayOfYear = today.dayOfYear
        val lengthOfYear = today.lengthOfYear()
        // Ensure percent is between 0.0 and 1.0
        val percent = (dayOfYear.toFloat() / lengthOfYear.toFloat()).coerceIn(0f, 1f)
        
        return YearProgressData(
            year = year,
            dayOfYear = dayOfYear,
            daysRemaining = lengthOfYear - dayOfYear,
            percentComplete = percent
        )
    }

    @Composable
    private fun YearProgressContent(data: YearProgressData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
                .clickable(actionStartActivity(Intent().apply {
                    setClassName("com.momentummm.app", "com.momentummm.app.MainActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header: Year and label
            Text(
                text = "${data.year} EN PROGRESO",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFAAAAAA)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Percentage Text
            Text(
                text = "${(data.percentComplete * 100).toInt()}%",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = data.percentComplete,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = ColorProvider(Color(0xFF00E5FF)), // Cyan
                backgroundColor = ColorProvider(Color(0xFF333333)) // Dark Gray
            )

            Spacer(modifier = GlanceModifier.height(20.dp))

            // Footer Statistics
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Passed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${data.dayOfYear}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "pasados",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(32.dp))

                // Divider (Vertical Line)
                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFF333333))
                ) {}

                Spacer(modifier = GlanceModifier.width(32.dp))

                // Remaining
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${data.daysRemaining}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "restantes",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

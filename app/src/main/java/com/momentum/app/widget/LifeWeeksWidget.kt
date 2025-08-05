package com.momentum.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LifeWeeksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LifeWeeksWidgetContent()
        }
    }

    @Composable
    private fun LifeWeeksWidgetContent() {
        val weeksLived = calculateWeeksLived()
        val totalWeeks = 80 * 52 // 80 years in weeks
        val remainingWeeks = totalWeeks - weeksLived

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tu Vida en Semanas",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = "$weeksLived",
                style = TextStyle(
                    color = ColorProvider(Color.Green),
                    fontSize = 24.sp
                )
            )
            
            Text(
                text = "semanas vividas",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            Text(
                text = "$remainingWeeks",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 18.sp
                )
            )
            
            Text(
                text = "semanas restantes",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp
                )
            )
        }
    }

    private fun calculateWeeksLived(): Long {
        // Default birth date if not configured (this should come from user preferences)
        val birthDate = LocalDate.of(1990, 1, 1)
        val today = LocalDate.now()
        return ChronoUnit.WEEKS.between(birthDate, today)
    }
}
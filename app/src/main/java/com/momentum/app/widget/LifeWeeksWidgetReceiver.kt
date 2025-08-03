package com.momentum.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.momentum.app.MainActivity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LifeWeeksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeWeeksWidget()
}

class LifeWeeksWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LifeWeeksWidgetContent()
        }
    }
    
    @Composable
    private fun LifeWeeksWidgetContent() {
        val lifeData = calculateLifeWeeks()
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tu Vida en Semanas",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.glance.color.ColorProvider(Color.Black)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                // Progress visualization
                Row {
                    repeat(10) { week ->
                        Box(
                            modifier = GlanceModifier
                                .size(4.dp)
                                .background(
                                    if (week < 3) Color.Red else Color.Gray
                                )
                        )
                        if (week < 9) {
                            Spacer(modifier = GlanceModifier.size(2.dp))
                        }
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = "${lifeData.weeksLived} semanas vividas",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = androidx.glance.color.ColorProvider(Color.Gray)
                    )
                )
                
                Text(
                    text = "${lifeData.weeksRemaining} semanas restantes",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = androidx.glance.color.ColorProvider(Color.Gray)
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Button(
                    text = "Abrir App",
                    onClick = actionStartActivity<MainActivity>()
                )
            }
        }
    }
    
    private fun calculateLifeWeeks(): LifeWeeksData {
        // Sample calculation - in real app, this would come from user settings
        val birthDate = LocalDate.of(1990, 1, 1)
        val currentDate = LocalDate.now()
        val totalWeeks = 4160 // 80 years * 52 weeks
        val weeksLived = ChronoUnit.WEEKS.between(birthDate, currentDate).toInt()
        val weeksRemaining = totalWeeks - weeksLived
        val currentAge = currentDate.year - birthDate.year
        val progressPercentage = (weeksLived.toFloat() / totalWeeks) * 100
        
        return LifeWeeksData(
            totalWeeks = totalWeeks,
            weeksLived = weeksLived,
            weeksRemaining = weeksRemaining,
            currentAge = currentAge,
            progressPercentage = progressPercentage
        )
    }
}

data class LifeWeeksData(
    val totalWeeks: Int,
    val weeksLived: Int,
    val weeksRemaining: Int,
    val currentAge: Int,
    val progressPercentage: Float
)
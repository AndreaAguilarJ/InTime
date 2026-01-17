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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.momentummm.app.MainActivity
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.util.LifeWeeksCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LifeWeeksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data before providing content
        val database = AppDatabase.getDatabase(context)
        val userSettings = withContext(Dispatchers.IO) {
            database.userDao().getUserSettingsSync()
        }

        val lifeWeeksData = userSettings?.birthDate?.let { birthDate ->
            LifeWeeksCalculator.calculateLifeWeeks(birthDate)
        }

        provideContent {
            LifeWeeksWidgetContent(context, lifeWeeksData)
        }
    }

    @Composable
    private fun LifeWeeksWidgetContent(
        context: Context,
        lifeWeeksData: LifeWeeksCalculator.LifeWeeksData?
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lifeWeeksData != null) {
                Text(
                    text = "Tu Vida en Semanas",
                    style = TextStyle(
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "${lifeWeeksData.weeksLived}",
                    style = TextStyle(
                        fontSize = 32.sp
                    )
                )

                Text(
                    text = "semanas vividas",
                    style = TextStyle(
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "${lifeWeeksData.weeksRemaining}",
                    style = TextStyle(
                        fontSize = 24.sp
                    )
                )

                Text(
                    text = "semanas restantes",
                    style = TextStyle(
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "Edad: ${lifeWeeksData.currentAge} años",
                    style = TextStyle(
                        fontSize = 10.sp
                    )
                )
            } else {
                // Show message if no birth date configured
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
                        fontSize = 14.sp
                    )
                )

                Text(
                    text = "de nacimiento en la app",
                    style = TextStyle(
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
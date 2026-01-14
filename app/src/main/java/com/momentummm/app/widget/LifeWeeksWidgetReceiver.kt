package com.momentummm.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment

class LifeWeeksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LifeWeeksWidgetProvider()
}

class LifeWeeksWidgetProvider : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Widget en mantenimiento")
            }
        }
    }
}
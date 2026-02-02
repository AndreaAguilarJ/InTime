package com.momentummm.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for the Motivational Message Widget.
 * This receiver is registered in AndroidManifest.xml to handle widget updates.
 */
class MotivationalMessageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MotivationalMessageWidget()
}

package com.momentummm.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.momentummm.app.MainActivity
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MotivationalMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Widget that displays the motivational message of the day.
 * 
 * Features:
 * - Shows a random motivational message each day
 * - Category-based emoji and theming
 * - Opens the app when tapped
 * - Auto-updates daily
 */
class MotivationalMessageWidget : GlanceAppWidget() {

    companion object {
        // Key for storing message of the day ID to show same message all day
        private const val PREFS_NAME = "motivational_widget_prefs"
        private const val KEY_MESSAGE_ID = "message_of_day_id"
        private const val KEY_MESSAGE_DATE = "message_date"
        
        /**
         * Update all motivational message widgets.
         */
        suspend fun updateAllWidgets(context: Context) {
            MotivationalMessageWidget().updateAll(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val message = withContext(Dispatchers.IO) {
            getMessageOfTheDay(context)
        }

        provideContent {
            MotivationalWidgetContent(message)
        }
    }
    
    /**
     * Get the message of the day, caching it for the whole day.
     */
    private suspend fun getMessageOfTheDay(context: Context): MotivationalMessage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_MESSAGE_DATE, "")
        val savedMessageId = prefs.getString(KEY_MESSAGE_ID, null)
        
        // If we have a cached message for today, try to use it
        if (savedDate == today && savedMessageId != null) {
            val db = AppDatabase.getDatabase(context)
            val cachedMessage = db.motivationalMessageDao().getMessageById(savedMessageId)
            if (cachedMessage != null) {
                return cachedMessage
            }
        }
        
        // Get a new message for today
        val db = AppDatabase.getDatabase(context)
        val newMessage = db.motivationalMessageDao().getMessageOfTheDay(java.util.Date())
            ?: db.motivationalMessageDao().getSmartRandomMessage(
                MessageCategory.entries,
                com.momentummm.app.data.entity.MessageTone.entries
            )
            ?: getDefaultMessage()
        
        // Cache it
        prefs.edit()
            .putString(KEY_MESSAGE_DATE, today)
            .putString(KEY_MESSAGE_ID, newMessage.id)
            .apply()
        
        return newMessage
    }
    
    private fun getDefaultMessage(): MotivationalMessage {
        return MotivationalMessage(
            id = "default_widget_message",
            content = "Hoy es un gran día para lograr tus metas. ¡Tú puedes con todo lo que te propongas!",
            category = MessageCategory.MOTIVATION,
            tone = com.momentummm.app.data.entity.MessageTone.FRIENDLY,
            emoji = "💪"
        )
    }

    @Composable
    private fun MotivationalWidgetContent(message: MotivationalMessage) {
        val emoji = message.emoji ?: message.category.emoji
        val backgroundColor = getCategoryColor(message.category)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(Intent().apply {
                    setClassName("com.momentummm.app", "com.momentummm.app.MainActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("source", "widget")
                    putExtra("message_id", message.id)
                })),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji
            Text(
                text = emoji,
                style = TextStyle(
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            // Message content
            Text(
                text = message.content,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 4
            )
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            // Category label
            Text(
                text = "✨ ${message.category.displayName}",
                style = TextStyle(
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
    
    /**
     * Get a background color based on the message category.
     */
    private fun getCategoryColor(category: MessageCategory): Color {
        return when (category) {
            MessageCategory.MORNING -> Color(0xFFFFE4B5) // Moccasin
            MessageCategory.EVENING -> Color(0xFF6A5ACD) // Slate blue
            MessageCategory.ACHIEVEMENT -> Color(0xFFFFD700) // Gold
            MessageCategory.FOCUS -> Color(0xFF87CEEB) // Sky blue
            MessageCategory.GRATITUDE -> Color(0xFFDDA0DD) // Plum
            MessageCategory.CHALLENGE -> Color(0xFFFF6347) // Tomato
            MessageCategory.STREAK -> Color(0xFFFF7F50) // Coral
            MessageCategory.CELEBRATION -> Color(0xFFFFB6C1) // Light pink
            MessageCategory.MOTIVATION -> Color(0xFF90EE90) // Light green
            MessageCategory.PRODUCTIVITY -> Color(0xFF4682B4) // Steel blue
            MessageCategory.MINDFULNESS -> Color(0xFF98FB98) // Pale green
            MessageCategory.SELF_CARE -> Color(0xFFE6E6FA) // Lavender
            MessageCategory.GROWTH -> Color(0xFF32CD32) // Lime green
            MessageCategory.RESILIENCE -> Color(0xFFCD853F) // Peru
            MessageCategory.WEEKEND -> Color(0xFF87CEFA) // Light sky blue
            MessageCategory.MONDAY -> Color(0xFFFFD700) // Gold
            MessageCategory.MILESTONE -> Color(0xFFFFD700) // Gold
            MessageCategory.COMEBACK -> Color(0xFF9370DB) // Medium purple
        }
    }
}

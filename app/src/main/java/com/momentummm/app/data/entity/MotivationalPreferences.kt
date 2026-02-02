package com.momentummm.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity to store user preferences for motivational messages.
 * This is a singleton table (always id = 1) that stores all motivational message settings.
 */
@Entity(tableName = "motivational_preferences")
data class MotivationalPreferences(
    @PrimaryKey
    val id: Int = 1, // Singleton pattern - always 1

    // Main toggle
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    // Frequency settings (3-8 notifications per day)
    @ColumnInfo(name = "daily_frequency")
    val dailyFrequency: Int = 5,

    // Time range for notifications
    @ColumnInfo(name = "start_hour")
    val startHour: Int = 8, // 8 AM

    @ColumnInfo(name = "start_minute")
    val startMinute: Int = 0,

    @ColumnInfo(name = "end_hour")
    val endHour: Int = 22, // 10 PM

    @ColumnInfo(name = "end_minute")
    val endMinute: Int = 0,

    // Categories preferences (stored as comma-separated string)
    @ColumnInfo(name = "enabled_categories")
    val enabledCategories: String = MessageCategory.entries.joinToString(",") { it.name },

    // Tones preferences (stored as comma-separated string)
    @ColumnInfo(name = "enabled_tones")
    val enabledTones: String = MessageTone.entries.joinToString(",") { it.name },

    // AI generation toggle
    @ColumnInfo(name = "ai_generation_enabled")
    val aiGenerationEnabled: Boolean = false,

    // AI daily limit
    @ColumnInfo(name = "ai_daily_limit")
    val aiDailyLimit: Int = 10,

    @ColumnInfo(name = "ai_messages_generated_today")
    val aiMessagesGeneratedToday: Int = 0,

    @ColumnInfo(name = "ai_last_generation_date")
    val aiLastGenerationDate: Date? = null,

    // Smart timing options
    @ColumnInfo(name = "respect_focus_mode")
    val respectFocusMode: Boolean = true,

    @ColumnInfo(name = "respect_dnd")
    val respectDnd: Boolean = true,

    @ColumnInfo(name = "smart_timing_enabled")
    val smartTimingEnabled: Boolean = true,

    // Show messages when user is less active
    @ColumnInfo(name = "send_when_inactive")
    val sendWhenInactive: Boolean = true,

    // Weekend-specific settings
    @ColumnInfo(name = "weekend_frequency")
    val weekendFrequency: Int = 3,

    @ColumnInfo(name = "different_weekend_schedule")
    val differentWeekendSchedule: Boolean = false,

    @ColumnInfo(name = "weekend_start_hour")
    val weekendStartHour: Int = 10,

    @ColumnInfo(name = "weekend_end_hour")
    val weekendEndHour: Int = 22,

    // AI Personality/Avatar selection
    @ColumnInfo(name = "ai_personality")
    val aiPersonality: AIPersonality = AIPersonality.FRIENDLY_COACH,

    // Voice notes feature
    @ColumnInfo(name = "voice_notes_enabled")
    val voiceNotesEnabled: Boolean = false,

    // Community messages
    @ColumnInfo(name = "community_messages_enabled")
    val communityMessagesEnabled: Boolean = false,

    // Widget settings
    @ColumnInfo(name = "widget_auto_refresh")
    val widgetAutoRefresh: Boolean = true,

    @ColumnInfo(name = "widget_show_category")
    val widgetShowCategory: Boolean = true,

    // Analytics consent
    @ColumnInfo(name = "analytics_enabled")
    val analyticsEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
) {
    /**
     * Get enabled categories as a list
     */
    fun getEnabledCategoriesList(): List<MessageCategory> {
        return enabledCategories.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { name ->
                try { MessageCategory.valueOf(name) } catch (e: Exception) { null }
            }
    }

    /**
     * Get enabled tones as a list
     */
    fun getEnabledTonesList(): List<MessageTone> {
        return enabledTones.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { name ->
                try { MessageTone.valueOf(name) } catch (e: Exception) { null }
            }
    }
}

/**
 * AI Personality/Avatar options for message generation.
 * Each personality adjusts the tone and style of AI-generated messages.
 */
enum class AIPersonality(val displayName: String, val description: String, val emoji: String) {
    FRIENDLY_COACH("Coach Amigable", "Motivador y cercano, como tu mejor amigo fitness", "🏃"),
    STRICT_COACH("Coach Estricto", "Directo y exigente, sin excusas", "💪"),
    WISE_MENTOR("Mentor Sabio", "Reflexivo y filosófico, comparte sabiduría", "🧙"),
    EMPATHETIC_FRIEND("Amigo Empático", "Comprensivo y cálido, siempre te apoya", "🤗"),
    CHEERLEADER("Animador Entusiasta", "Ultra positivo y lleno de energía", "📣"),
    ZEN_MASTER("Maestro Zen", "Calmado y mindful, enfocado en el presente", "🧘"),
    SCIENTIST("Científico Curioso", "Basado en datos y evidencia", "🔬"),
    ADVENTURER("Aventurero", "Te reta a salir de tu zona de confort", "🌍")
}

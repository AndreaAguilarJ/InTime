package com.momentummm.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a motivational message in the database.
 * Messages can be predefined (seed data), AI-generated, or user-created.
 */
@Entity(tableName = "motivational_messages")
data class MotivationalMessage(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "category")
    val category: MessageCategory,

    @ColumnInfo(name = "tone")
    val tone: MessageTone,

    @ColumnInfo(name = "emoji")
    val emoji: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "times_shown")
    val timesShown: Int = 0,

    @ColumnInfo(name = "last_shown_at")
    val lastShownAt: Date? = null,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,

    @ColumnInfo(name = "is_ai_generated")
    val isAIGenerated: Boolean = false,

    @ColumnInfo(name = "love_count")
    val loveCount: Int = 0,

    @ColumnInfo(name = "share_count")
    val shareCount: Int = 0,

    @ColumnInfo(name = "language")
    val language: String = "es",

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date()
)

/**
 * Categories for motivational messages.
 * Each category represents a specific context or time when the message is most appropriate.
 */
enum class MessageCategory(val displayName: String, val emoji: String) {
    MORNING("Buenos días", "🌅"),
    EVENING("Buenas noches", "🌙"),
    ACHIEVEMENT("Logros", "🏆"),
    FOCUS("Concentración", "🧘"),
    GRATITUDE("Gratitud", "💚"),
    CHALLENGE("Desafíos", "💪"),
    STREAK("Rachas", "🔥"),
    CELEBRATION("Celebración", "🎉"),
    MOTIVATION("Motivación general", "✨"),
    PRODUCTIVITY("Productividad", "📈"),
    MINDFULNESS("Mindfulness", "🧠"),
    SELF_CARE("Autocuidado", "❤️"),
    GROWTH("Crecimiento personal", "🌱"),
    RESILIENCE("Resiliencia", "🦋"),
    WEEKEND("Fin de semana", "🎊"),
    MONDAY("Lunes motivacional", "💼"),
    MILESTONE("Hitos", "🎯"),
    COMEBACK("Volver a empezar", "🔄")
}

/**
 * Tones for motivational messages.
 * Each tone represents a different communication style.
 */
enum class MessageTone(val displayName: String, val description: String, val emoji: String) {
    FRIENDLY("Amigable", "Como un amigo cercano", "😊"),
    COACH("Coach", "Directo y motivador", "💪"),
    WISE("Sabio", "Reflexivo y profundo", "🦉"),
    ENERGETIC("Energético", "Lleno de entusiasmo", "⚡"),
    CALM("Calmado", "Sereno y reconfortante", "🧘"),
    HUMOROUS("Humorístico", "Ligero y divertido", "😄"),
    INSPIRATIONAL("Inspiracional", "Elevador y soñador", "🌟"),
    PRACTICAL("Práctico", "Consejos concretos", "📝")
}

package com.momentummm.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity to track user reactions and interactions with motivational messages.
 * This enables analytics on message effectiveness and user preferences.
 */
@Entity(
    tableName = "message_reactions",
    foreignKeys = [
        ForeignKey(
            entity = MotivationalMessage::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["message_id"]),
        Index(value = ["reaction_type"]),
        Index(value = ["created_at"])
    ]
)
data class MessageReaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "reaction_type")
    val reactionType: ReactionType,

    @ColumnInfo(name = "notification_opened")
    val notificationOpened: Boolean = false,

    @ColumnInfo(name = "notification_shown_at")
    val notificationShownAt: Date? = null,

    @ColumnInfo(name = "notification_opened_at")
    val notificationOpenedAt: Date? = null,

    @ColumnInfo(name = "context")
    val context: String? = null, // e.g., "goal_25%", "streak_7_days", "morning_notification"

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)

/**
 * Types of reactions users can have to messages.
 */
enum class ReactionType {
    SHOWN,          // Message was shown to the user (notification or in-app)
    LOVED,          // User tapped "❤️ Me encantó"
    DISMISSED,      // User dismissed the notification
    SHARED,         // User shared the message
    FAVORITED,      // User marked as favorite
    UNFAVORITED,    // User unmarked as favorite
    REQUESTED_NEW,  // User tapped "🔄 Otro mensaje"
    OPENED_SETTINGS // User tapped "⚙️ Configurar"
}

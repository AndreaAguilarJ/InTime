package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.MessageReaction
import com.momentummm.app.data.entity.ReactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MessageReactionDao {

    // ============== INSERT OPERATIONS ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: MessageReaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReactions(reactions: List<MessageReaction>)

    // ============== QUERY OPERATIONS ==============

    @Query("SELECT * FROM message_reactions ORDER BY created_at DESC")
    fun getAllReactions(): Flow<List<MessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE message_id = :messageId ORDER BY created_at DESC")
    fun getReactionsForMessage(messageId: String): Flow<List<MessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE reaction_type = :reactionType ORDER BY created_at DESC")
    fun getReactionsByType(reactionType: ReactionType): Flow<List<MessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE created_at >= :startDate AND created_at <= :endDate ORDER BY created_at DESC")
    suspend fun getReactionsInDateRange(startDate: Date, endDate: Date): List<MessageReaction>

    @Query("SELECT * FROM message_reactions WHERE notification_opened = 1 ORDER BY notification_opened_at DESC")
    fun getOpenedNotifications(): Flow<List<MessageReaction>>

    // ============== ANALYTICS QUERIES ==============

    /**
     * Get count of reactions by type
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE reaction_type = :reactionType")
    suspend fun getReactionCountByType(reactionType: ReactionType): Int

    /**
     * Get notification open rate (opened / shown)
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE reaction_type = 'SHOWN'")
    suspend fun getTotalNotificationsShown(): Int

    @Query("SELECT COUNT(*) FROM message_reactions WHERE notification_opened = 1")
    suspend fun getTotalNotificationsOpened(): Int

    /**
     * Get reactions count per day for analytics
     */
    @Query("""
        SELECT COUNT(*) FROM message_reactions 
        WHERE created_at >= :startOfDay AND created_at < :endOfDay
        AND reaction_type = :reactionType
    """)
    suspend fun getReactionCountForDay(startOfDay: Date, endOfDay: Date, reactionType: ReactionType): Int

    /**
     * Get the most reacted message IDs (by love count)
     */
    @Query("""
        SELECT message_id, COUNT(*) as count 
        FROM message_reactions 
        WHERE reaction_type = 'LOVED'
        GROUP BY message_id 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getMostLovedMessageIds(limit: Int = 10): List<MessageReactionCount>

    /**
     * Get reactions by context (e.g., "goal_25%", "streak_7_days")
     */
    @Query("SELECT * FROM message_reactions WHERE context = :context ORDER BY created_at DESC")
    suspend fun getReactionsByContext(context: String): List<MessageReaction>

    /**
     * Get hourly distribution of opened notifications (for smart timing)
     */
    @Query("""
        SELECT COUNT(*) FROM message_reactions 
        WHERE notification_opened = 1 
        AND strftime('%H', notification_opened_at / 1000, 'unixepoch', 'localtime') = :hour
    """)
    suspend fun getOpenCountForHour(hour: String): Int

    /**
     * Get best hours for notifications (hours with highest open rate)
     */
    @Query("""
        SELECT strftime('%H', notification_opened_at / 1000, 'unixepoch', 'localtime') as hour,
               COUNT(*) as count
        FROM message_reactions 
        WHERE notification_opened = 1
        GROUP BY hour
        ORDER BY count DESC
        LIMIT 5
    """)
    suspend fun getBestNotificationHours(): List<HourlyStats>

    /**
     * Get total reactions for today
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE created_at >= :todayStart")
    suspend fun getTodayReactionsCount(todayStart: Date): Int

    /**
     * Get count of notifications shown since a specific date
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE reaction_type = 'SHOWN' AND created_at >= :since")
    suspend fun getNotificationsShownSince(since: Date): Int

    /**
     * Get unique messages shown today
     */
    @Query("""
        SELECT COUNT(DISTINCT message_id) FROM message_reactions 
        WHERE reaction_type = 'SHOWN' AND created_at >= :todayStart
    """)
    suspend fun getUniqueMessagesShownToday(todayStart: Date): Int

    // ============== DELETE OPERATIONS ==============

    @Delete
    suspend fun deleteReaction(reaction: MessageReaction)

    @Query("DELETE FROM message_reactions WHERE message_id = :messageId")
    suspend fun deleteReactionsForMessage(messageId: String)

    @Query("DELETE FROM message_reactions WHERE created_at < :beforeDate")
    suspend fun deleteOldReactions(beforeDate: Date)

    @Query("DELETE FROM message_reactions")
    suspend fun deleteAllReactions()

    // ============== GAMIFICATION QUERIES ==============

    /**
     * Get consecutive days with notifications received (for badges)
     */
    @Query("""
        SELECT COUNT(DISTINCT date(created_at / 1000, 'unixepoch', 'localtime')) 
        FROM message_reactions 
        WHERE reaction_type = 'SHOWN' 
        AND created_at >= :since
    """)
    suspend fun getDaysWithNotificationsSince(since: Date): Int

    /**
     * Get total favorites count (for "Coleccionista" badge)
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE reaction_type = 'FAVORITED'")
    suspend fun getTotalFavoritedCount(): Int

    /**
     * Get total shares count (for "Compartidor" badge)
     */
    @Query("SELECT COUNT(*) FROM message_reactions WHERE reaction_type = 'SHARED'")
    suspend fun getTotalSharedCount(): Int
}

/**
 * Data class for message reaction count results
 */
data class MessageReactionCount(
    val message_id: String,
    val count: Int
)

/**
 * Data class for hourly statistics
 */
data class HourlyStats(
    val hour: String,
    val count: Int
)

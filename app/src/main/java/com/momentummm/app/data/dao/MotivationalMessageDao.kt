package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MotivationalMessageDao {

    // ============== INSERT OPERATIONS ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MotivationalMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MotivationalMessage>)

    // ============== QUERY OPERATIONS ==============

    @Query("SELECT * FROM motivational_messages ORDER BY created_at DESC")
    fun getAllMessages(): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MotivationalMessage?

    @Query("SELECT * FROM motivational_messages WHERE id = :messageId")
    fun getMessageByIdFlow(messageId: String): Flow<MotivationalMessage?>

    @Query("SELECT * FROM motivational_messages WHERE is_favorite = 1 ORDER BY updated_at DESC")
    fun getFavoriteMessages(): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE category = :category ORDER BY RANDOM()")
    fun getMessagesByCategory(category: MessageCategory): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE tone = :tone ORDER BY RANDOM()")
    fun getMessagesByTone(tone: MessageTone): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE is_custom = 1 ORDER BY created_at DESC")
    fun getCustomMessages(): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE is_ai_generated = 1 ORDER BY created_at DESC")
    fun getAIGeneratedMessages(): Flow<List<MotivationalMessage>>

    @Query("SELECT * FROM motivational_messages WHERE content LIKE '%' || :query || '%' ORDER BY is_favorite DESC, times_shown DESC")
    fun searchMessages(query: String): Flow<List<MotivationalMessage>>

    // ============== SMART SELECTION QUERIES ==============

    /**
     * Get a random message from specified categories and tones, 
     * excluding recently shown messages (last 50).
     * Favorites have 2x probability (included twice via UNION).
     */
    @Query("""
        SELECT * FROM (
            SELECT * FROM motivational_messages 
            WHERE category IN (:categories) 
            AND tone IN (:tones)
            AND id NOT IN (
                SELECT id FROM motivational_messages 
                ORDER BY last_shown_at DESC 
                LIMIT 50
            )
            UNION ALL
            SELECT * FROM motivational_messages 
            WHERE category IN (:categories) 
            AND tone IN (:tones)
            AND is_favorite = 1
            AND id NOT IN (
                SELECT id FROM motivational_messages 
                ORDER BY last_shown_at DESC 
                LIMIT 50
            )
        )
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getSmartRandomMessage(
        categories: List<MessageCategory>,
        tones: List<MessageTone>
    ): MotivationalMessage?

    /**
     * Get random message for specific category (e.g., MORNING, ACHIEVEMENT)
     */
    @Query("""
        SELECT * FROM motivational_messages 
        WHERE category = :category 
        AND id NOT IN (
            SELECT id FROM motivational_messages 
            ORDER BY last_shown_at DESC 
            LIMIT 20
        )
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomMessageForCategory(category: MessageCategory): MotivationalMessage?

    /**
     * Get the "message of the day" - a random message not shown today
     */
    @Query("""
        SELECT * FROM motivational_messages 
        WHERE (last_shown_at IS NULL OR last_shown_at < :todayStart)
        ORDER BY 
            CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END,
            RANDOM()
        LIMIT 1
    """)
    suspend fun getMessageOfTheDay(todayStart: Date): MotivationalMessage?

    /**
     * Get least shown messages (for variety)
     */
    @Query("""
        SELECT * FROM motivational_messages 
        WHERE category IN (:categories)
        ORDER BY times_shown ASC, RANDOM()
        LIMIT :limit
    """)
    suspend fun getLeastShownMessages(categories: List<MessageCategory>, limit: Int = 10): List<MotivationalMessage>

    // ============== UPDATE OPERATIONS ==============

    @Update
    suspend fun updateMessage(message: MotivationalMessage)

    @Query("UPDATE motivational_messages SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id = :messageId")
    suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_messages SET times_shown = times_shown + 1, last_shown_at = :shownAt, updated_at = :shownAt WHERE id = :messageId")
    suspend fun markAsShown(messageId: String, shownAt: Date = Date())

    @Query("UPDATE motivational_messages SET love_count = love_count + 1, updated_at = :updatedAt WHERE id = :messageId")
    suspend fun incrementLoveCount(messageId: String, updatedAt: Date = Date())

    @Query("UPDATE motivational_messages SET share_count = share_count + 1, updated_at = :updatedAt WHERE id = :messageId")
    suspend fun incrementShareCount(messageId: String, updatedAt: Date = Date())

    // ============== DELETE OPERATIONS ==============

    @Delete
    suspend fun deleteMessage(message: MotivationalMessage)

    @Query("DELETE FROM motivational_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM motivational_messages WHERE is_custom = 1")
    suspend fun deleteAllCustomMessages()

    @Query("DELETE FROM motivational_messages WHERE is_ai_generated = 1")
    suspend fun deleteAllAIGeneratedMessages()

    // ============== STATISTICS ==============

    @Query("SELECT COUNT(*) FROM motivational_messages")
    suspend fun getTotalMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM motivational_messages WHERE is_favorite = 1")
    suspend fun getFavoriteMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM motivational_messages WHERE is_custom = 1")
    suspend fun getCustomMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM motivational_messages WHERE is_ai_generated = 1")
    suspend fun getAIGeneratedMessagesCount(): Int

    @Query("SELECT COUNT(*) FROM motivational_messages WHERE times_shown > 0")
    suspend fun getShownMessagesCount(): Int

    @Query("SELECT SUM(times_shown) FROM motivational_messages")
    suspend fun getTotalTimesShown(): Int?

    @Query("SELECT SUM(love_count) FROM motivational_messages")
    suspend fun getTotalLoveCount(): Int?

    @Query("SELECT SUM(share_count) FROM motivational_messages")
    suspend fun getTotalShareCount(): Int?

    /**
     * Get most popular messages by love count
     */
    @Query("SELECT * FROM motivational_messages ORDER BY love_count DESC LIMIT :limit")
    suspend fun getMostLovedMessages(limit: Int = 10): List<MotivationalMessage>

    /**
     * Get most shared messages
     */
    @Query("SELECT * FROM motivational_messages ORDER BY share_count DESC LIMIT :limit")
    suspend fun getMostSharedMessages(limit: Int = 10): List<MotivationalMessage>

    /**
     * Check if seed data has been populated
     */
    @Query("SELECT COUNT(*) FROM motivational_messages WHERE is_custom = 0 AND is_ai_generated = 0")
    suspend fun getSeedMessagesCount(): Int
}

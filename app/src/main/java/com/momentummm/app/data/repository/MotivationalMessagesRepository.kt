package com.momentummm.app.data.repository

import android.content.Context
import android.util.Log
import com.momentummm.app.data.MotivationalMessagesSeed
import com.momentummm.app.data.dao.MessageReactionDao
import com.momentummm.app.data.dao.MotivationalMessageDao
import com.momentummm.app.data.dao.MotivationalPreferencesDao
import com.momentummm.app.data.entity.AIPersonality
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageReaction
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.data.entity.ReactionType
import com.momentummm.app.notification.MotivationalAlarmScheduler
import com.momentummm.app.notification.NotificationWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing motivational messages.
 * Handles message selection, reactions, preferences, and seed data initialization.
 */
@Singleton
class MotivationalMessagesRepository @Inject constructor(
    private val messageDao: MotivationalMessageDao,
    private val reactionDao: MessageReactionDao,
    private val preferencesDao: MotivationalPreferencesDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MotivationalMessagesRepo"
        private const val RECENT_MESSAGES_TO_EXCLUDE = 50
    }

    // ============== INITIALIZATION ==============

    /**
     * Initialize the database with seed data if not already populated.
     */
    suspend fun initializeSeedDataIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                val seedCount = messageDao.getSeedMessagesCount()
                if (seedCount == 0) {
                    Log.d(TAG, "Populating motivational messages seed data...")
                    val messages = MotivationalMessagesSeed.getMotivationalMessages()
                    messageDao.insertMessages(messages)
                    Log.d(TAG, "Inserted ${messages.size} seed messages")
                } else {
                    Log.d(TAG, "Seed data already exists: $seedCount messages")
                }

                // Los mensajes personalizados se añaden aparte y sólo si faltan.
                // La semilla original únicamente se inserta cuando la tabla está
                // vacía, así que sin esto los usuarios que ya tenían la app
                // nunca recibirían un mensaje con su nombre. Se comprueba id a
                // id porque el insert es REPLACE: reinsertar a ciegas borraría
                // los favoritos y las estadísticas del usuario.
                val personalized = MotivationalMessagesSeed.getPersonalizedMessages()
                val missing = personalized.filter { messageDao.getMessageById(it.id) == null }
                if (missing.isNotEmpty()) {
                    messageDao.insertMessages(missing)
                    Log.d(TAG, "Inserted ${missing.size} personalized messages")
                }
                
                // Ensure preferences exist
                preferencesDao.ensurePreferencesExist()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing seed data", e)
            }
        }
    }

    // ============== MESSAGE QUERIES ==============

    /**
     * Get all messages as a Flow.
     */
    fun getAllMessages(): Flow<List<MotivationalMessage>> = messageDao.getAllMessages()

    /**
     * Get a message by ID.
     */
    suspend fun getMessageById(messageId: String): MotivationalMessage? {
        return messageDao.getMessageById(messageId)
    }

    /**
     * Get favorite messages.
     */
    fun getFavoriteMessages(): Flow<List<MotivationalMessage>> = messageDao.getFavoriteMessages()

    /**
     * Get custom (user-created) messages.
     */
    fun getCustomMessages(): Flow<List<MotivationalMessage>> = messageDao.getCustomMessages()

    /**
     * Get AI-generated messages.
     */
    fun getAIGeneratedMessages(): Flow<List<MotivationalMessage>> = messageDao.getAIGeneratedMessages()

    /**
     * Search messages by content.
     */
    fun searchMessages(query: String): Flow<List<MotivationalMessage>> = messageDao.searchMessages(query)

    /**
     * Get messages filtered by category.
     */
    fun getMessagesByCategory(category: MessageCategory): Flow<List<MotivationalMessage>> {
        return messageDao.getMessagesByCategory(category)
    }

    /**
     * Get messages filtered by tone.
     */
    fun getMessagesByTone(tone: MessageTone): Flow<List<MotivationalMessage>> {
        return messageDao.getMessagesByTone(tone)
    }

    // ============== SMART MESSAGE SELECTION ==============

    /**
     * Get the next message to show based on user preferences.
     * Uses smart selection algorithm that:
     * 1. Excludes last 50 shown messages
     * 2. Gives 2x weight to favorite messages
     * 3. Respects enabled categories and tones
     * 4. Falls back gracefully if no matches found
     */
    suspend fun getNextMessageToShow(): MotivationalMessage? {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = preferencesDao.getPreferencesSync() ?: MotivationalPreferences()
                val enabledCategories = prefs.getEnabledCategoriesList()
                val enabledTones = prefs.getEnabledTonesList()
                
                if (enabledCategories.isEmpty() || enabledTones.isEmpty()) {
                    Log.w(TAG, "No enabled categories or tones, using defaults")
                    return@withContext getRandomFallbackMessage()
                }
                
                // Try smart selection first
                var message = messageDao.getSmartRandomMessage(enabledCategories, enabledTones)
                
                // Fallback to any message in enabled categories if smart selection fails
                if (message == null) {
                    Log.d(TAG, "Smart selection failed, trying category fallback")
                    message = messageDao.getLeastShownMessages(enabledCategories, 1).firstOrNull()
                }
                
                // Final fallback to any message
                if (message == null) {
                    Log.d(TAG, "Category fallback failed, using random message")
                    message = getRandomFallbackMessage()
                }
                
                message
            } catch (e: Exception) {
                Log.e(TAG, "Error getting next message", e)
                null
            }
        }
    }

    /**
     * Get a contextual message for a specific category (e.g., MORNING, ACHIEVEMENT).
     */
    suspend fun getContextualMessage(category: MessageCategory): MotivationalMessage? {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.getRandomMessageForCategory(category)
                    ?: messageDao.getMessagesByCategory(category).first().randomOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting contextual message for $category", e)
                null
            }
        }
    }

    /**
     * Get the "message of the day" for widgets.
     */
    suspend fun getMessageOfTheDay(): MotivationalMessage? {
        return withContext(Dispatchers.IO) {
            try {
                val todayStart = getTodayStart()
                messageDao.getMessageOfTheDay(todayStart)
                    ?: getNextMessageToShow()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting message of the day", e)
                null
            }
        }
    }

    /**
     * Get message for goal progress milestone (25%, 50%, 75%, 100%).
     */
    suspend fun getGoalProgressMessage(progressPercent: Int, goalTitle: String): MotivationalMessage? {
        return withContext(Dispatchers.IO) {
            val category = when {
                progressPercent >= 100 -> MessageCategory.ACHIEVEMENT
                progressPercent >= 75 -> MessageCategory.MILESTONE
                progressPercent >= 50 -> MessageCategory.MOTIVATION
                progressPercent >= 25 -> MessageCategory.CHALLENGE
                else -> MessageCategory.MOTIVATION
            }
            getContextualMessage(category)
        }
    }

    /**
     * Get message for streak celebration.
     */
    suspend fun getStreakMessage(streakDays: Int): MotivationalMessage? {
        return withContext(Dispatchers.IO) {
            val category = when {
                streakDays >= 100 -> MessageCategory.CELEBRATION
                streakDays >= 30 -> MessageCategory.MILESTONE
                streakDays >= 7 -> MessageCategory.STREAK
                else -> MessageCategory.STREAK
            }
            getContextualMessage(category)
        }
    }

    private suspend fun getRandomFallbackMessage(): MotivationalMessage? {
        return messageDao.getAllMessages().first().randomOrNull()
    }

    private fun getTodayStart(): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    // ============== MESSAGE ACTIONS ==============

    /**
     * Mark a message as shown (increment counter, update timestamp).
     */
    suspend fun markMessageAsShown(messageId: String, context: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                val now = Date()
                messageDao.markAsShown(messageId, now)
                
                // Record reaction
                reactionDao.insertReaction(
                    MessageReaction(
                        messageId = messageId,
                        reactionType = ReactionType.SHOWN,
                        notificationShownAt = now,
                        context = context
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error marking message as shown", e)
            }
        }
    }

    /**
     * Toggle favorite status for a message.
     */
    suspend fun toggleFavorite(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val message = messageDao.getMessageById(messageId)
                val newStatus = !(message?.isFavorite ?: false)
                messageDao.updateFavoriteStatus(messageId, newStatus)
                
                // Record reaction
                reactionDao.insertReaction(
                    MessageReaction(
                        messageId = messageId,
                        reactionType = if (newStatus) ReactionType.FAVORITED else ReactionType.UNFAVORITED
                    )
                )
                
                newStatus
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
                false
            }
        }
    }

    /**
     * Record a "love" reaction (from notification action).
     */
    suspend fun loveMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                messageDao.incrementLoveCount(messageId)
                reactionDao.insertReaction(
                    MessageReaction(
                        messageId = messageId,
                        reactionType = ReactionType.LOVED
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loving message", e)
            }
        }
    }

    /**
     * Record a share action.
     */
    suspend fun shareMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                messageDao.incrementShareCount(messageId)
                reactionDao.insertReaction(
                    MessageReaction(
                        messageId = messageId,
                        reactionType = ReactionType.SHARED
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recording share", e)
            }
        }
    }

    /**
     * Record notification opened.
     */
    suspend fun recordNotificationOpened(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Find the SHOWN reaction and update it
                val reaction = MessageReaction(
                    messageId = messageId,
                    reactionType = ReactionType.SHOWN,
                    notificationOpened = true,
                    notificationOpenedAt = Date()
                )
                reactionDao.insertReaction(reaction)
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification opened", e)
            }
        }
    }

    /**
     * Record "another message" request from notification.
     */
    suspend fun recordAnotherMessageRequested(messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                reactionDao.insertReaction(
                    MessageReaction(
                        messageId = messageId,
                        reactionType = ReactionType.REQUESTED_NEW
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recording another message request", e)
            }
        }
    }

    // ============== CUSTOM MESSAGES ==============

    /**
     * Create a custom message.
     */
    suspend fun createCustomMessage(
        content: String,
        category: MessageCategory = MessageCategory.MOTIVATION,
        tone: MessageTone = MessageTone.FRIENDLY,
        emoji: String? = null
    ): MotivationalMessage {
        return withContext(Dispatchers.IO) {
            val message = MotivationalMessage(
                id = "custom_${UUID.randomUUID()}",
                content = content,
                category = category,
                tone = tone,
                emoji = emoji,
                isCustom = true,
                isAIGenerated = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            messageDao.insertMessage(message)
            message
        }
    }

    /**
     * Delete a custom message.
     */
    suspend fun deleteCustomMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            val message = messageDao.getMessageById(messageId)
            if (message?.isCustom == true) {
                messageDao.deleteMessageById(messageId)
            }
        }
    }

    /**
     * Save an AI-generated message.
     */
    suspend fun saveAIGeneratedMessage(
        content: String,
        category: MessageCategory,
        tone: MessageTone
    ): MotivationalMessage {
        return withContext(Dispatchers.IO) {
            val message = MotivationalMessage(
                id = "ai_${UUID.randomUUID()}",
                content = content,
                category = category,
                tone = tone,
                isCustom = false,
                isAIGenerated = true,
                createdAt = Date(),
                updatedAt = Date()
            )
            messageDao.insertMessage(message)
            message
        }
    }

    // ============== PREFERENCES ==============

    /**
     * Get user preferences as Flow.
     */
    fun getPreferences(): Flow<MotivationalPreferences?> = preferencesDao.getPreferences()

    /**
     * Get user preferences synchronously.
     */
    suspend fun getPreferencesSync(): MotivationalPreferences? {
        return withContext(Dispatchers.IO) {
            preferencesDao.getPreferencesSync()
        }
    }

    /**
     * Update enabled status.
     */
    suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesDao.setEnabledSafe(enabled)
            applyScheduleChanges()
        }
    }

    /**
     * Update daily frequency (3-8 notifications per day).
     */
    suspend fun setDailyFrequency(frequency: Int) {
        withContext(Dispatchers.IO) {
            val clampedFrequency = frequency.coerceIn(1, 10)
            preferencesDao.setDailyFrequencySafe(clampedFrequency)
            applyScheduleChanges()
        }
    }

    /**
     * Update notification time range.
     */
    suspend fun setTimeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        withContext(Dispatchers.IO) {
            preferencesDao.setTimeRangeSafe(startHour, startMinute, endHour, endMinute)
            applyScheduleChanges()
        }
    }

    /**
     * Update enabled categories.
     */
    suspend fun setEnabledCategories(categories: List<MessageCategory>) {
        withContext(Dispatchers.IO) {
            val categoriesString = categories.joinToString(",") { it.name }
            preferencesDao.setEnabledCategoriesSafe(categoriesString)
        }
    }

    /**
     * Update enabled tones.
     */
    suspend fun setEnabledTones(tones: List<MessageTone>) {
        withContext(Dispatchers.IO) {
            val tonesString = tones.joinToString(",") { it.name }
            preferencesDao.setEnabledTonesSafe(tonesString)
        }
    }

    /**
     * Update AI generation settings.
     */
    suspend fun setAIGenerationEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesDao.setAIGenerationEnabled(enabled)
        }
    }

    /**
     * Update AI personality.
     */
    suspend fun setAIPersonality(personality: AIPersonality) {
        withContext(Dispatchers.IO) {
            preferencesDao.setAIPersonality(personality)
        }
    }

    /**
     * Update smart timing settings.
     */
    suspend fun setSmartTimingEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesDao.setSmartTimingEnabled(enabled)
        }
    }

    /**
     * Update respect focus mode setting.
     */
    suspend fun setRespectFocusMode(respectFocusMode: Boolean) {
        withContext(Dispatchers.IO) {
            preferencesDao.setRespectFocusMode(respectFocusMode)
        }
    }

    /**
     * Update full preferences object.
     */
    suspend fun updatePreferences(preferences: MotivationalPreferences) {
        withContext(Dispatchers.IO) {
            // upsert en lugar de UPDATE: funciona aunque la fila no exista aún.
            preferencesDao.upsertPreferences(preferences.copy(updatedAt = Date()))
            applyScheduleChanges()
        }
    }

    /**
     * Reprograma las alarmas de mensajes motivacionales con las preferencias
     * ya guardadas.
     *
     * BUG CORREGIDO: antes, cambiar la frecuencia o el rango horario sólo
     * escribía en la base de datos. La programación seguía usando los valores
     * antiguos (de hecho, valores fijos en el código), así que los ajustes de
     * la pantalla de configuración no tenían ningún efecto observable.
     */
    suspend fun applyScheduleChanges() {
        withContext(Dispatchers.IO) {
            try {
                val preferences = preferencesDao.getPreferencesSync() ?: MotivationalPreferences()
                MotivationalAlarmScheduler.scheduleAll(context, preferences)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron reprogramar las alarmas motivacionales", e)
            }
        }
    }

    // ============== AI RATE LIMITING ==============

    /**
     * Check if AI generation is allowed (under daily limit).
     */
    suspend fun canGenerateAIMessage(): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = preferencesDao.getPreferencesSync() ?: return@withContext false
            
            if (!prefs.aiGenerationEnabled) return@withContext false
            
            // Check if we need to reset the daily counter
            val today = getTodayStart()
            val lastGenDate = prefs.aiLastGenerationDate
            
            if (lastGenDate == null || lastGenDate.before(today)) {
                // New day, reset counter
                preferencesDao.resetAIGenerationCount()
                return@withContext true
            }
            
            // Check if under limit
            prefs.aiMessagesGeneratedToday < prefs.aiDailyLimit
        }
    }

    /**
     * Increment AI generation counter after generating a message.
     */
    suspend fun incrementAIGenerationCount() {
        withContext(Dispatchers.IO) {
            preferencesDao.incrementAIGenerationCount()
        }
    }

    // ============== ANALYTICS ==============

    /**
     * Get total messages count.
     */
    suspend fun getTotalMessagesCount(): Int = messageDao.getTotalMessagesCount()

    /**
     * Get favorite messages count.
     */
    suspend fun getFavoriteMessagesCount(): Int = messageDao.getFavoriteMessagesCount()

    /**
     * Get custom messages count.
     */
    suspend fun getCustomMessagesCount(): Int = messageDao.getCustomMessagesCount()

    /**
     * Get most loved messages.
     */
    suspend fun getMostLovedMessages(limit: Int = 10): List<MotivationalMessage> {
        return messageDao.getMostLovedMessages(limit)
    }

    /**
     * Get most shared messages.
     */
    suspend fun getMostSharedMessages(limit: Int = 10): List<MotivationalMessage> {
        return messageDao.getMostSharedMessages(limit)
    }

    /**
     * Get notification open rate.
     */
    suspend fun getNotificationOpenRate(): Float {
        return withContext(Dispatchers.IO) {
            val shown = reactionDao.getTotalNotificationsShown()
            val opened = reactionDao.getTotalNotificationsOpened()
            if (shown == 0) 0f else opened.toFloat() / shown.toFloat()
        }
    }

    /**
     * Get best hours for notifications.
     */
    suspend fun getBestNotificationHours(): List<Int> {
        return withContext(Dispatchers.IO) {
            reactionDao.getBestNotificationHours().map { it.hour.toInt() }
        }
    }

    /**
     * Get total love count.
     */
    suspend fun getTotalLoveCount(): Int = messageDao.getTotalLoveCount() ?: 0

    /**
     * Get total share count.
     */
    suspend fun getTotalShareCount(): Int = messageDao.getTotalShareCount() ?: 0

    /**
     * Get total favorited count (for gamification).
     */
    suspend fun getTotalFavoritedReactions(): Int = reactionDao.getTotalFavoritedCount()

    /**
     * Get total shared count (for gamification).
     */
    suspend fun getTotalSharedReactions(): Int = reactionDao.getTotalSharedCount()

    /**
     * Get days with notifications since a date (for streak/gamification).
     */
    suspend fun getDaysWithNotificationsSince(since: Date): Int {
        return reactionDao.getDaysWithNotificationsSince(since)
    }

    // ============== SMART TIMING ==============

    /**
     * Check if it's a good time to send a notification.
     * Considers: quiet hours, focus mode, user activity, weekend settings.
     */
    suspend fun isGoodTimeForNotification(graceMinutes: Int = 0): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = preferencesDao.getPreferencesSync() ?: return@withContext true
            
            if (!prefs.enabled) return@withContext false
            
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val isWeekend = calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            
            // Get applicable time range
            val (startHour, endHour) = if (isWeekend && prefs.differentWeekendSchedule) {
                prefs.weekendStartHour to prefs.weekendEndHour
            } else {
                prefs.startHour to prefs.endHour
            }
            
            // Check if within time range
            val currentTimeMinutes = currentHour * 60 + currentMinute
            val startTimeMinutes = startHour * 60 + prefs.startMinute
            val endTimeMinutes = endHour * 60 + prefs.endMinute

            // El margen amplía el final de la ventana. Sin permiso de alarmas
            // exactas el sistema puede retrasar una alarma hasta una hora, así
            // que un mensaje programado a las 21:40 llegaba a las 22:10 y se
            // descartaba por "fuera de horario". Con margen se entrega.
            NotificationWindow.isWithin(
                nowMinutes = currentTimeMinutes,
                startMinutes = startTimeMinutes,
                endMinutes = endTimeMinutes,
                graceMinutes = graceMinutes
            )
        }
    }

    /**
     * Get the appropriate category for the current time of day.
     */
    fun getTimeBasedCategory(): MessageCategory {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return when {
            dayOfWeek == Calendar.MONDAY && hour in 7..10 -> MessageCategory.MONDAY
            dayOfWeek in listOf(Calendar.SATURDAY, Calendar.SUNDAY) -> MessageCategory.WEEKEND
            hour in 5..9 -> MessageCategory.MORNING
            hour in 21..23 -> MessageCategory.EVENING
            hour in 10..14 -> MessageCategory.PRODUCTIVITY
            hour in 14..18 -> MessageCategory.FOCUS
            else -> MessageCategory.MOTIVATION
        }
    }

    // ============== NOTIFICATION CONTROL ==============

    /**
     * Check if motivational notifications are enabled.
     */
    suspend fun areNotificationsEnabled(): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = preferencesDao.getPreferencesSync() ?: return@withContext true
            prefs.enabled
        }
    }

    /**
     * Check if the daily notification limit has been reached.
     * Uses dailyFrequency from preferences.
     */
    suspend fun hasReachedDailyLimit(): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = preferencesDao.getPreferencesSync() ?: return@withContext false
            val todayStart = getTodayStart()
            val notificationsToday = reactionDao.getNotificationsShownSince(todayStart)
            notificationsToday >= prefs.dailyFrequency
        }
    }
}

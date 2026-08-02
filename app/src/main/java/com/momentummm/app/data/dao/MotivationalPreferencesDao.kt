package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.AIPersonality
import com.momentummm.app.data.entity.MotivationalPreferences
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MotivationalPreferencesDao {

    // ============== QUERY OPERATIONS ==============

    @Query("SELECT * FROM motivational_preferences WHERE id = 1")
    fun getPreferences(): Flow<MotivationalPreferences?>

    @Query("SELECT * FROM motivational_preferences WHERE id = 1")
    suspend fun getPreferencesSync(): MotivationalPreferences?

    // ============== INSERT/UPDATE OPERATIONS ==============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: MotivationalPreferences)

    @Update
    suspend fun updatePreferences(preferences: MotivationalPreferences)

    /**
     * Insert default preferences if none exist
     */
    @Transaction
    suspend fun ensurePreferencesExist() {
        val existing = getPreferencesSync()
        if (existing == null) {
            insertPreferences(MotivationalPreferences())
        }
    }

    // ============== SPECIFIC UPDATE OPERATIONS ==============
    //
    // BUG CORREGIDO: todos los setters de abajo son sentencias UPDATE sobre la
    // fila singleton `id = 1`. Si esa fila no existía todavía —por ejemplo
    // porque el seed inicial falló— el UPDATE afectaba a 0 filas y el ajuste
    // del usuario se perdía **en silencio**: cambiaba la frecuencia o el
    // horario en la interfaz y no ocurría nada.
    //
    // Los envoltorios `@Transaction` de esta sección garantizan que la fila
    // exista antes de escribir. Llama siempre a estas versiones (las usa
    // MotivationalMessagesRepository) en lugar de a los UPDATE crudos.

    @Transaction
    suspend fun setEnabledSafe(enabled: Boolean) {
        ensurePreferencesExist()
        setEnabled(enabled)
    }

    @Transaction
    suspend fun setDailyFrequencySafe(frequency: Int) {
        ensurePreferencesExist()
        setDailyFrequency(frequency)
    }

    @Transaction
    suspend fun setTimeRangeSafe(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        ensurePreferencesExist()
        setTimeRange(startHour, startMinute, endHour, endMinute)
    }

    @Transaction
    suspend fun setEnabledCategoriesSafe(categories: String) {
        ensurePreferencesExist()
        setEnabledCategories(categories)
    }

    @Transaction
    suspend fun setEnabledTonesSafe(tones: String) {
        ensurePreferencesExist()
        setEnabledTones(tones)
    }

    /**
     * Guarda el objeto completo. Usa INSERT-OR-REPLACE en lugar de UPDATE para
     * que funcione también cuando la fila todavía no existe.
     */
    @Transaction
    suspend fun upsertPreferences(preferences: MotivationalPreferences) {
        insertPreferences(preferences.copy(id = 1))
    }

    @Query("UPDATE motivational_preferences SET enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setEnabled(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET daily_frequency = :frequency, updated_at = :updatedAt WHERE id = 1")
    suspend fun setDailyFrequency(frequency: Int, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET start_hour = :startHour, start_minute = :startMinute, end_hour = :endHour, end_minute = :endMinute, updated_at = :updatedAt WHERE id = 1")
    suspend fun setTimeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET enabled_categories = :categories, updated_at = :updatedAt WHERE id = 1")
    suspend fun setEnabledCategories(categories: String, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET enabled_tones = :tones, updated_at = :updatedAt WHERE id = 1")
    suspend fun setEnabledTones(tones: String, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET ai_generation_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setAIGenerationEnabled(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET ai_personality = :personality, updated_at = :updatedAt WHERE id = 1")
    suspend fun setAIPersonality(personality: AIPersonality, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET respect_focus_mode = :respectFocusMode, updated_at = :updatedAt WHERE id = 1")
    suspend fun setRespectFocusMode(respectFocusMode: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET respect_dnd = :respectDnd, updated_at = :updatedAt WHERE id = 1")
    suspend fun setRespectDnd(respectDnd: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET smart_timing_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setSmartTimingEnabled(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET voice_notes_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setVoiceNotesEnabled(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET community_messages_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setCommunityMessagesEnabled(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET widget_auto_refresh = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setWidgetAutoRefresh(enabled: Boolean, updatedAt: Date = Date())

    @Query("UPDATE motivational_preferences SET analytics_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setAnalyticsEnabled(enabled: Boolean, updatedAt: Date = Date())

    // ============== AI RATE LIMITING ==============

    @Query("UPDATE motivational_preferences SET ai_messages_generated_today = :count, ai_last_generation_date = :date, updated_at = :date WHERE id = 1")
    suspend fun updateAIGenerationCount(count: Int, date: Date = Date())

    @Query("UPDATE motivational_preferences SET ai_messages_generated_today = ai_messages_generated_today + 1, ai_last_generation_date = :date, updated_at = :date WHERE id = 1")
    suspend fun incrementAIGenerationCount(date: Date = Date())

    @Query("UPDATE motivational_preferences SET ai_messages_generated_today = 0, updated_at = :date WHERE id = 1")
    suspend fun resetAIGenerationCount(date: Date = Date())

    // ============== WEEKEND SETTINGS ==============

    @Query("UPDATE motivational_preferences SET different_weekend_schedule = :enabled, weekend_frequency = :frequency, weekend_start_hour = :startHour, weekend_end_hour = :endHour, updated_at = :updatedAt WHERE id = 1")
    suspend fun setWeekendSettings(enabled: Boolean, frequency: Int, startHour: Int, endHour: Int, updatedAt: Date = Date())
}

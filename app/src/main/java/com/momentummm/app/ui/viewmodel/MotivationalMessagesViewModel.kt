package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.worker.MotivationalNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for motivational messages settings and library screens.
 */
data class MotivationalMessagesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val preferences: MotivationalPreferences = MotivationalPreferences(),
    val allMessages: List<MotivationalMessage> = emptyList(),
    val favoriteMessages: List<MotivationalMessage> = emptyList(),
    val customMessages: List<MotivationalMessage> = emptyList(),
    val messageOfTheDay: MotivationalMessage? = null,
    val selectedCategory: MessageCategory? = null,
    val selectedTone: MessageTone? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val totalMessagesShown: Int = 0,
    val totalLoved: Int = 0,
    val openRate: Float = 0f,
    val bestHours: List<Int> = emptyList(),
    val showTestNotification: Boolean = false,
    val showMessagePreview: MotivationalMessage? = null,
    val showAddCustomDialog: Boolean = false,
    val customMessageContent: String = "",
    val customMessageEmoji: String = "✨",
    val customMessageCategory: MessageCategory = MessageCategory.MOTIVATION,
    val customMessageTone: MessageTone = MessageTone.FRIENDLY
)

@HiltViewModel
class MotivationalMessagesViewModel @Inject constructor(
    private val repository: MotivationalMessagesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MotivationalMessagesUiState())
    val uiState: StateFlow<MotivationalMessagesUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.getPreferences()
                    .catch { e ->
                        _uiState.update { it.copy(error = "Error: ${e.message}") }
                    }
                    .collect { prefs ->
                        _uiState.update { it.copy(preferences = prefs ?: MotivationalPreferences()) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}") }
            }
        }
        
        viewModelScope.launch {
            try {
                val messageOfDay = repository.getMessageOfTheDay()
                val openRate = repository.getNotificationOpenRate()
                val bestHours = repository.getBestNotificationHours()
                val lovedCount = repository.getTotalLoveCount()
                val totalShown = repository.getTotalMessagesCount()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        messageOfTheDay = messageOfDay,
                        openRate = openRate,
                        bestHours = bestHours,
                        totalLoved = lovedCount,
                        totalMessagesShown = totalShown
                    )
                }
                loadMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            try {
                repository.getAllMessages()
                    .catch { e -> _uiState.update { it.copy(error = "Error: ${e.message}") } }
                    .collect { messages ->
                        _uiState.update {
                            it.copy(
                                allMessages = applyFilters(messages),
                                favoriteMessages = messages.filter { m -> m.isFavorite },
                                customMessages = messages.filter { m -> m.isCustom }
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}") }
            }
        }
    }
    
    private fun applyFilters(messages: List<MotivationalMessage>): List<MotivationalMessage> {
        val state = _uiState.value
        return messages.filter { message ->
            (state.selectedCategory == null || message.category == state.selectedCategory) &&
            (state.selectedTone == null || message.tone == state.selectedTone) &&
            (state.searchQuery.isEmpty() || message.content.contains(state.searchQuery, ignoreCase = true)) &&
            (!state.showFavoritesOnly || message.isFavorite)
        }
    }
    
    fun toggleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.setEnabled(enabled)
                _uiState.update { it.copy(isSaving = false, preferences = it.preferences.copy(enabled = enabled)) }
                if (enabled) MotivationalNotificationWorker.schedulePeriodicNotifications(context)
                else MotivationalNotificationWorker.cancelAllNotifications(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Error: ${e.message}") }
            }
        }
    }
    
    fun updateDailyFrequency(frequency: Int) {
        viewModelScope.launch {
            val f = frequency.coerceIn(1, 10)
            repository.setDailyFrequency(f)
            _uiState.update { it.copy(preferences = it.preferences.copy(dailyFrequency = f)) }
            if (_uiState.value.preferences.enabled) MotivationalNotificationWorker.schedulePeriodicNotifications(context)
        }
    }
    
    fun updateTimeRange(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            repository.setTimeRange(startHour, 0, endHour, 0)
            _uiState.update { it.copy(preferences = it.preferences.copy(startHour = startHour, endHour = endHour)) }
        }
    }
    
    fun toggleCategory(category: MessageCategory) {
        viewModelScope.launch {
            val current = _uiState.value.preferences.getEnabledCategoriesList().toMutableList()
            if (current.contains(category)) { if (current.size > 1) current.remove(category) }
            else current.add(category)
            repository.setEnabledCategories(current)
            _uiState.update { it.copy(preferences = it.preferences.copy(enabledCategories = current.joinToString(",") { c -> c.name })) }
        }
    }
    
    fun toggleTone(tone: MessageTone) {
        viewModelScope.launch {
            val current = _uiState.value.preferences.getEnabledTonesList().toMutableList()
            if (current.contains(tone)) { if (current.size > 1) current.remove(tone) }
            else current.add(tone)
            repository.setEnabledTones(current)
            _uiState.update { it.copy(preferences = it.preferences.copy(enabledTones = current.joinToString(",") { t -> t.name })) }
        }
    }
    
    fun toggleSmartTiming(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSmartTimingEnabled(enabled)
            _uiState.update { it.copy(preferences = it.preferences.copy(smartTimingEnabled = enabled)) }
        }
    }
    
    fun toggleWeekendSchedule(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(_uiState.value.preferences.copy(differentWeekendSchedule = enabled))
            _uiState.update { it.copy(preferences = it.preferences.copy(differentWeekendSchedule = enabled)) }
        }
    }
    
    fun toggleAIMessages(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAIGenerationEnabled(enabled)
            _uiState.update { it.copy(preferences = it.preferences.copy(aiGenerationEnabled = enabled)) }
        }
    }
    
    fun toggleFavorite(messageId: String) {
        viewModelScope.launch { repository.toggleFavorite(messageId) }
    }
    
    fun shareMessage(message: MotivationalMessage) {
        viewModelScope.launch { repository.shareMessage(message.id) }
        _uiState.update { it.copy(showMessagePreview = message) }
    }
    
    fun showAddCustomDialog() {
        _uiState.update { it.copy(showAddCustomDialog = true, customMessageContent = "", customMessageEmoji = "✨", customMessageCategory = MessageCategory.MOTIVATION, customMessageTone = MessageTone.FRIENDLY) }
    }
    
    fun hideAddCustomDialog() { _uiState.update { it.copy(showAddCustomDialog = false) } }
    fun updateCustomMessageContent(c: String) { _uiState.update { it.copy(customMessageContent = c) } }
    fun updateCustomMessageEmoji(e: String) { _uiState.update { it.copy(customMessageEmoji = e) } }
    fun updateCustomMessageCategory(c: MessageCategory) { _uiState.update { it.copy(customMessageCategory = c) } }
    fun updateCustomMessageTone(t: MessageTone) { _uiState.update { it.copy(customMessageTone = t) } }
    
    fun saveCustomMessage() {
        val state = _uiState.value
        if (state.customMessageContent.isBlank()) { _uiState.update { it.copy(error = "El mensaje no puede estar vacío") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.createCustomMessage(state.customMessageContent.trim(), state.customMessageCategory, state.customMessageTone, state.customMessageEmoji)
                _uiState.update { it.copy(isSaving = false, showAddCustomDialog = false, customMessageContent = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Error: ${e.message}") }
            }
        }
    }
    
    fun deleteCustomMessage(messageId: String) { viewModelScope.launch { repository.deleteCustomMessage(messageId) } }
    fun setSelectedCategory(c: MessageCategory?) { _uiState.update { it.copy(selectedCategory = c) }; loadMessages() }
    fun setSelectedTone(t: MessageTone?) { _uiState.update { it.copy(selectedTone = t) }; loadMessages() }
    fun setSearchQuery(q: String) { _uiState.update { it.copy(searchQuery = q) }; loadMessages() }
    fun toggleFavoritesOnly() { _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }; loadMessages() }
    fun clearFilters() { _uiState.update { it.copy(selectedCategory = null, selectedTone = null, searchQuery = "", showFavoritesOnly = false) }; loadMessages() }
    
    fun sendTestNotification() {
        _uiState.update { it.copy(showTestNotification = true) }
        viewModelScope.launch {
            val message = repository.getNextMessageToShow()
            if (message != null) _uiState.update { it.copy(showMessagePreview = message) }
        }
    }
    
    fun hideMessagePreview() { _uiState.update { it.copy(showMessagePreview = null, showTestNotification = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun refresh() { loadData() }
}

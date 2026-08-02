package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.data.manager.MotivationalNotificationManager
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.notification.MotivationalAlarmScheduler
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
    /** Texto a mostrar tras pulsar "Probar": el resultado real del envío. */
    val testNotificationResult: String? = null,
    val testNotificationFailed: Boolean = false,
    /** Nombre con el que el usuario quiere que le hablen los mensajes. */
    val displayName: String = "",
    /** false si el sistema puede retrasar las alarmas de los mensajes. */
    val canScheduleExactAlarms: Boolean = true,
    val showAddCustomDialog: Boolean = false,
    val customMessageContent: String = "",
    val customMessageEmoji: String = "✨",
    val customMessageCategory: MessageCategory = MessageCategory.MOTIVATION,
    val customMessageTone: MessageTone = MessageTone.FRIENDLY
)

@HiltViewModel
class MotivationalMessagesViewModel @Inject constructor(
    private val repository: MotivationalMessagesRepository,
    private val notificationManager: MotivationalNotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MotivationalMessagesUiState())
    val uiState: StateFlow<MotivationalMessagesUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
        loadPersonalization()
    }

    /**
     * Carga el nombre del usuario y si el sistema permite alarmas exactas.
     *
     * Lo segundo importa: en Android 14+ el permiso de alarmas exactas NO se
     * concede por defecto a las apps recién instaladas, así que los mensajes se
     * programan con alarmas aproximadas y pueden llegar con retraso. Antes esto
     * ocurría en silencio y el usuario sólo veía que "los mensajes no llegan a
     * su hora".
     */
    private fun loadPersonalization() {
        viewModelScope.launch {
            val name = runCatching { UserPreferencesRepository.getDisplayName(context) }
                .getOrNull()
                .orEmpty()
            val exact = MotivationalAlarmScheduler.canScheduleExactAlarms(context)
            _uiState.update { it.copy(displayName = name, canScheduleExactAlarms = exact) }
        }
    }

    /** Vuelve a comprobar el permiso de alarmas exactas (al volver de Ajustes). */
    fun refreshExactAlarmPermission() {
        _uiState.update {
            it.copy(canScheduleExactAlarms = MotivationalAlarmScheduler.canScheduleExactAlarms(context))
        }
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
                // setEnabled ya reprograma (o cancela) todas las alarmas.
                //
                // BUG CORREGIDO: antes, al reactivar el interruptor sólo se
                // llamaba a schedulePeriodicNotifications(); los mensajes de
                // mañana y noche se habían cancelado con el resto y no se
                // volvían a programar nunca.
                repository.setEnabled(enabled)
                _uiState.update { it.copy(isSaving = false, preferences = it.preferences.copy(enabled = enabled)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Error: ${e.message}") }
            }
        }
    }
    
    fun updateDailyFrequency(frequency: Int) {
        viewModelScope.launch {
            val f = frequency.coerceIn(1, 10)
            // setDailyFrequency reprograma las franjas con la nueva frecuencia.
            repository.setDailyFrequency(f)
            _uiState.update { it.copy(preferences = it.preferences.copy(dailyFrequency = f)) }
        }
    }
    
    fun updateTimeRange(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            // BUG CORREGIDO: este cambio se guardaba pero no se reprogramaba
            // nada, así que el rango horario elegido no tenía ningún efecto.
            // setTimeRange ahora reprograma las alarmas.
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
    
    /**
     * Envía de verdad una notificación de prueba.
     *
     * BUG CORREGIDO: esta función sólo ponía `showTestNotification = true` para
     * abrir un diálogo de vista previa dentro de la app, mientras la pantalla
     * mostraba el mensaje "Notificación de prueba enviada". Nunca se publicaba
     * ninguna notificación, así que era imposible comprobar si el sistema de
     * mensajes funcionaba —justo lo que el botón promete—.
     *
     * Ahora publica la notificación real y devuelve el resultado para que la
     * interfaz diga la verdad.
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            val result = notificationManager.showTestMessage()
            _uiState.update {
                it.copy(
                    testNotificationResult = result.error
                        ?: "Notificación enviada. Míralas en la barra de estado.",
                    testNotificationFailed = !result.delivered,
                    showMessagePreview = result.message
                )
            }
        }
    }

    fun clearTestNotificationResult() {
        _uiState.update { it.copy(testNotificationResult = null, testNotificationFailed = false) }
    }

    /** Nombre con el que el usuario quiere que le hablen los mensajes. */
    fun setDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
        viewModelScope.launch {
            try {
                UserPreferencesRepository.setDisplayName(context, name)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo guardar el nombre: ${e.message}") }
            }
        }
    }
    
    fun hideMessagePreview() { _uiState.update { it.copy(showMessagePreview = null, showTestNotification = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun refresh() { loadData() }
}

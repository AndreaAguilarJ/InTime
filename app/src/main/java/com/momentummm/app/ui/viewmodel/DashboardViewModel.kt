package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.Quote
import com.momentummm.app.data.manager.GamificationManager
import com.momentummm.app.data.manager.GamificationState
import com.momentummm.app.data.repository.QuotesRepository
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.data.repository.UserRepository
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalScreenTime: String = "0h 0m",
    val quoteOfTheDay: Quote? = null,
    val topApps: List<com.momentummm.app.data.repository.AppUsageInfo> = emptyList(),
    val hasUsagePermission: Boolean = false,
    // Gamification state
    val gamificationState: GamificationState? = null,
    val showGamificationEvent: Boolean = false,
    val gamificationEventMessage: String = "",
    val gamificationEventXp: Int = 0,
    val gamificationEventCoins: Int = 0,
    val isLevelUpEvent: Boolean = false
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quotesRepository: QuotesRepository,
    private val gamificationManager: GamificationManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeGamificationState()
    }

    private fun observeGamificationState() {
        viewModelScope.launch {
            gamificationManager.getGamificationState().collect { state ->
                _uiState.value = _uiState.value.copy(gamificationState = state)
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Check permissions first
                val hasPermission = PermissionUtils.hasUsageStatsPermission(context)
                
                // Load quote of the day en background
                val quote = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    quotesRepository.getRandomQuote()
                }
                
                if (hasPermission) {
                    // Load usage stats en background (Dispatchers.IO para operaciones I/O)
                    val (totalScreenTime, topApps) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val time = usageStatsRepository.getTotalScreenTime()
                        val apps = usageStatsRepository.getTodayUsageStats().take(5)
                        time to apps
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        totalScreenTime = LifeWeeksCalculator.formatTimeFromMillis(totalScreenTime),
                        quoteOfTheDay = quote,
                        topApps = topApps as List<com.momentummm.app.data.repository.AppUsageInfo>,
                        hasUsagePermission = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        quoteOfTheDay = quote,
                        hasUsagePermission = false,
                        totalScreenTime = "0h 0m",
                        topApps = emptyList()
                    )
                }

                // Update daily streak
                val streakEvent = gamificationManager.updateDailyStreak()
                if (streakEvent.xpGained != 0 || streakEvent.type == GamificationManager.EventType.STREAK_BROKEN) {
                    showGamificationEvent(
                        message = streakEvent.message,
                        xp = streakEvent.xpGained,
                        coins = streakEvent.coinsGained,
                        isLevelUp = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasUsagePermission = false
                )
            }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }

    fun showGamificationEvent(
        message: String,
        xp: Int = 0,
        coins: Int = 0,
        isLevelUp: Boolean = false
    ) {
        _uiState.value = _uiState.value.copy(
            showGamificationEvent = true,
            gamificationEventMessage = message,
            gamificationEventXp = xp,
            gamificationEventCoins = coins,
            isLevelUpEvent = isLevelUp
        )
    }

    fun dismissGamificationEvent() {
        _uiState.value = _uiState.value.copy(
            showGamificationEvent = false
        )
    }
}

class DashboardViewModelFactory(
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quotesRepository: QuotesRepository,
    private val gamificationManager: GamificationManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                userRepository,
                usageStatsRepository,
                quotesRepository,
                gamificationManager,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
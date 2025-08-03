package com.momentum.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.app.data.entity.Quote
import com.momentum.app.data.repository.QuotesRepository
import com.momentum.app.data.repository.UsageStatsRepository
import com.momentum.app.data.repository.UserRepository
import com.momentum.app.util.LifeWeeksCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalScreenTime: String = "0h 0m",
    val quoteOfTheDay: Quote? = null,
    val topApps: List<com.momentum.app.data.repository.AppUsageInfo> = emptyList(),
    val hasUsagePermission: Boolean = false
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quotesRepository: QuotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load quote of the day
                val quote = quotesRepository.getRandomQuote()
                
                // Load usage stats
                val totalScreenTime = usageStatsRepository.getTotalScreenTime()
                val topApps = usageStatsRepository.getTodayUsageStats().take(5)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalScreenTime = LifeWeeksCalculator.formatTimeFromMillis(totalScreenTime),
                    quoteOfTheDay = quote,
                    topApps = topApps,
                    hasUsagePermission = true // This should be checked properly
                )
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
}

class DashboardViewModelFactory(
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quotesRepository: QuotesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(userRepository, usageStatsRepository, quotesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
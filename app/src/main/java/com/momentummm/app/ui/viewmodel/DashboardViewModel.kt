package com.momentummm.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.Quote
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
    val hasUsagePermission: Boolean = false
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quotesRepository: QuotesRepository,
    private val context: Context
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
                // Check permissions first
                val hasPermission = PermissionUtils.hasUsageStatsPermission(context)
                
                // Load quote of the day (always available)
                val quote = quotesRepository.getRandomQuote()
                
                if (hasPermission) {
                    // Load usage stats
                    val totalScreenTime = usageStatsRepository.getTotalScreenTime()
                    val topApps = usageStatsRepository.getTodayUsageStats().take(5)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        totalScreenTime = LifeWeeksCalculator.formatTimeFromMillis(totalScreenTime),
                        quoteOfTheDay = quote,
                        topApps = topApps,
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
    private val quotesRepository: QuotesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(userRepository, usageStatsRepository, quotesRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
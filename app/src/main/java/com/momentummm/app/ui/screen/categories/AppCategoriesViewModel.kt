package com.momentummm.app.ui.screen.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.AppCategory
import com.momentummm.app.data.repository.AppCategoryRepository
import com.momentummm.app.data.repository.AppUsageInfo
import com.momentummm.app.data.repository.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext

data class AppCategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<AppCategory> = emptyList(),
    val availableApps: List<AppUsageInfo> = emptyList(),
    val categoryUsageTimes: Map<Int, Long> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class AppCategoriesViewModel @Inject constructor(
    private val appCategoryRepository: AppCategoryRepository,
    private val usageStatsRepository: UsageStatsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppCategoriesUiState())
    val uiState: StateFlow<AppCategoriesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
        }

        // Colección de categorías en tiempo real
        viewModelScope.launch {
            appCategoryRepository.getAllCategories()
                .catch { e -> 
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { categories ->
                    // Calcular tiempo de uso para cada categoría
                    val usageTimes = mutableMapOf<Int, Long>()
                    categories.forEach { category ->
                        try {
                            usageTimes[category.id] = appCategoryRepository.getCategoryUsageTime(category.id)
                        } catch (e: Exception) {
                            usageTimes[category.id] = 0L
                        }
                    }
                    
                    _uiState.update { it.copy(
                        categories = categories,
                        categoryUsageTimes = usageTimes,
                        isLoading = false
                    ) }
                }
        }

        // Cargar apps disponibles
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val availableApps = getInstalledApps()
                _uiState.update { it.copy(availableApps = availableApps) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun getInstalledApps(): List<AppUsageInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return packages
            .filter { app ->
                // Solo apps de usuario (no del sistema)
                (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (app.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            .map { app ->
                AppUsageInfo(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    totalTimeInMillis = 0L,
                    lastTimeUsed = 0L
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    fun createCategory(
        name: String,
        iconName: String = "Category",
        colorHex: String = "#6200EE",
        description: String = ""
    ) {
        viewModelScope.launch {
            try {
                appCategoryRepository.createCategory(name, iconName, colorHex, description)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCategory(category: AppCategory) {
        viewModelScope.launch {
            try {
                appCategoryRepository.updateCategory(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCategory(category: AppCategory) {
        viewModelScope.launch {
            try {
                appCategoryRepository.deleteCategory(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addAppToCategory(categoryId: Int, packageName: String) {
        viewModelScope.launch {
            try {
                appCategoryRepository.addAppToCategory(categoryId, packageName)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeAppFromCategory(categoryId: Int, packageName: String) {
        viewModelScope.launch {
            try {
                appCategoryRepository.removeAppFromCategory(categoryId, packageName)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCategoryLimit(categoryId: Int, limitMinutes: Int, enabled: Boolean) {
        viewModelScope.launch {
            try {
                appCategoryRepository.updateCategoryLimit(categoryId, limitMinutes, enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCategorySchedule(
        categoryId: Int,
        hasSchedule: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: Set<Int>
    ) {
        viewModelScope.launch {
            try {
                appCategoryRepository.updateCategorySchedule(
                    categoryId = categoryId,
                    hasSchedule = hasSchedule,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek.joinToString(",")
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun initializeSystemCategories() {
        viewModelScope.launch {
            try {
                appCategoryRepository.initializeSystemCategories()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

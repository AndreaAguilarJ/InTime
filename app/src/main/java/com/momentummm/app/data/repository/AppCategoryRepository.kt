package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.AppCategoryDao
import com.momentummm.app.data.entity.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio para manejar las categorías de apps.
 * 
 * Feature solicitada: "block apps via category" (bloquear todo "Entretenimiento")
 * 
 * Este repositorio permite:
 * - Crear categorías personalizadas (Trabajo, Entretenimiento, Social, etc.)
 * - Asignar apps a categorías
 * - Aplicar límites a categorías completas
 * - Calcular tiempo combinado de todas las apps en una categoría
 */
@Singleton
class AppCategoryRepository @Inject constructor(
    private val appCategoryDao: AppCategoryDao,
    @ApplicationContext private val context: Context,
    private val usageStatsRepository: UsageStatsRepository
) {

    /**
     * Obtiene todas las categorías
     */
    fun getAllCategories(): Flow<List<AppCategory>> = 
        appCategoryDao.getAllCategories()
    
    /**
     * Obtiene las categorías con límites activos
     */
    fun getEnabledCategories(): Flow<List<AppCategory>> = 
        appCategoryDao.getEnabledCategories()
    
    /**
     * Obtiene una categoría por ID
     */
    suspend fun getCategoryById(id: Int): AppCategory? = 
        appCategoryDao.getCategoryById(id)
    
    /**
     * Obtiene una categoría por nombre
     */
    suspend fun getCategoryByName(name: String): AppCategory? = 
        appCategoryDao.getCategoryByName(name)
    
    /**
     * Crea una nueva categoría
     */
    suspend fun createCategory(
        name: String,
        iconName: String = "Category",
        colorHex: String = "#6200EE",
        description: String = ""
    ): Long {
        val category = AppCategory(
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            description = description
        )
        return appCategoryDao.insertCategory(category)
    }
    
    /**
     * Actualiza una categoría existente
     */
    suspend fun updateCategory(category: AppCategory) {
        appCategoryDao.updateCategory(category.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Elimina una categoría
     */
    suspend fun deleteCategory(category: AppCategory) {
        appCategoryDao.deleteCategory(category)
    }
    
    /**
     * Añade una app a una categoría
     */
    suspend fun addAppToCategory(categoryId: Int, packageName: String) {
        val category = getCategoryById(categoryId) ?: return
        val updatedCategory = category.withApp(packageName)
        appCategoryDao.updateCategory(updatedCategory)
    }
    
    /**
     * Remueve una app de una categoría
     */
    suspend fun removeAppFromCategory(categoryId: Int, packageName: String) {
        val category = getCategoryById(categoryId) ?: return
        val updatedCategory = category.withoutApp(packageName)
        appCategoryDao.updateCategory(updatedCategory)
    }
    
    /**
     * Obtiene las categorías a las que pertenece un package
     */
    suspend fun getCategoriesForPackage(packageName: String): List<AppCategory> {
        return appCategoryDao.getCategoriesForPackage(packageName)
    }
    
    /**
     * Actualiza el límite de una categoría
     */
    suspend fun updateCategoryLimit(
        categoryId: Int, 
        limitMinutes: Int, 
        enabled: Boolean
    ) {
        appCategoryDao.updateCategoryLimit(categoryId, limitMinutes, enabled)
    }
    
    /**
     * Actualiza el horario de bloqueo de una categoría
     */
    suspend fun updateCategorySchedule(
        categoryId: Int,
        hasSchedule: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String
    ) {
        appCategoryDao.updateCategorySchedule(
            categoryId = categoryId,
            hasSchedule = hasSchedule,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeek = daysOfWeek
        )
    }
    
    /**
     * Calcula el tiempo de uso combinado de todas las apps en una categoría.
     * Este es el tiempo TOTAL que se compara contra el límite de la categoría.
     */
    suspend fun getCategoryUsageTime(categoryId: Int): Long {
        val category = getCategoryById(categoryId) ?: return 0L
        val todayUsage = usageStatsRepository.getTodayUsageStats()
        val categoryApps = category.getPackageNamesList()
        
        return todayUsage
            .filter { it.packageName in categoryApps }
            .sumOf { it.totalTimeInMillis }
    }
    
    /**
     * Verifica si una categoría ha excedido su límite
     */
    suspend fun isCategoryOverLimit(categoryId: Int): Boolean {
        val category = getCategoryById(categoryId) ?: return false
        if (!category.isLimitEnabled) return false
        
        val usageTimeMinutes = getCategoryUsageTime(categoryId) / (1000 * 60)
        return usageTimeMinutes >= category.dailyLimitMinutes
    }
    
    /**
     * Verifica si una categoría está bloqueada por horario
     */
    suspend fun isCategoryBlockedBySchedule(categoryId: Int): Boolean {
        val category = getCategoryById(categoryId) ?: return false
        return category.isWithinSchedule()
    }
    
    /**
     * Obtiene el tiempo restante para una categoría
     */
    suspend fun getCategoryRemainingTime(categoryId: Int): Int {
        val category = getCategoryById(categoryId) ?: return Int.MAX_VALUE
        if (!category.isLimitEnabled) return Int.MAX_VALUE
        
        val usageTimeMinutes = (getCategoryUsageTime(categoryId) / (1000 * 60)).toInt()
        return maxOf(0, category.dailyLimitMinutes - usageTimeMinutes)
    }
    
    /**
     * Verifica si una app debería estar bloqueada por categoría.
     * Esto verifica tanto el límite de tiempo como el bloqueo por horario.
     */
    suspend fun isAppBlockedByCategory(packageName: String): Boolean {
        val categories = getCategoriesForPackage(packageName)
        
        return categories.any { category ->
            // Verificar bloqueo por horario
            if (category.hasScheduleLimit && category.isWithinSchedule()) {
                return true
            }
            
            // Verificar límite de tiempo de categoría
            if (category.isLimitEnabled) {
                val usageTimeMinutes = getCategoryUsageTime(category.id) / (1000 * 60)
                if (usageTimeMinutes >= category.dailyLimitMinutes) {
                    return true
                }
            }
            
            false
        }
    }
    
    /**
     * Obtiene el motivo de bloqueo por categoría para una app
     */
    suspend fun getCategoryBlockReason(packageName: String): CategoryBlockReason? {
        val categories = getCategoriesForPackage(packageName)
        
        for (category in categories) {
            // Verificar bloqueo por horario primero
            if (category.hasScheduleLimit && category.isWithinSchedule()) {
                return CategoryBlockReason.ScheduleBlock(
                    categoryName = category.name,
                    startTime = category.getScheduleFormatted().split(" - ").first(),
                    endTime = category.getScheduleFormatted().split(" - ").last()
                )
            }
            
            // Verificar límite de tiempo
            if (category.isLimitEnabled) {
                val usageTimeMinutes = getCategoryUsageTime(category.id) / (1000 * 60)
                if (usageTimeMinutes >= category.dailyLimitMinutes) {
                    return CategoryBlockReason.LimitExceeded(
                        categoryName = category.name,
                        limitMinutes = category.dailyLimitMinutes
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * Inicializa las categorías predefinidas del sistema si no existen
     */
    suspend fun initializeSystemCategories() {
        val existingCount = appCategoryDao.getCategoryCount()
        if (existingCount == 0) {
            appCategoryDao.insertCategories(AppCategory.SYSTEM_CATEGORIES)
        }
    }
    
    /**
     * Auto-asigna un package a una categoría basándose en el mapeo conocido
     */
    suspend fun autoAssignCategory(packageName: String): AppCategory? {
        val suggestedCategoryName = AppCategory.KNOWN_APP_CATEGORIES[packageName] ?: return null
        val category = getCategoryByName(suggestedCategoryName) ?: return null
        
        // Solo añadir si no está ya en la categoría
        if (!category.containsApp(packageName)) {
            addAppToCategory(category.id, packageName)
        }
        
        return getCategoryById(category.id)
    }
    
    /**
     * Obtiene el resumen de categorías
     */
    suspend fun getCategoriesSummary(): CategorySummary {
        val allCategories = withTimeoutOrNull(5000L) {
            appCategoryDao.getAllCategories().first()
        } ?: emptyList()
        
        val enabledCategories = allCategories.filter { it.isLimitEnabled }
        val totalApps = allCategories.sumOf { it.getPackageNamesList().size }
        
        return CategorySummary(
            totalCategories = allCategories.size,
            enabledCategories = enabledCategories.size,
            totalAppsInCategories = totalApps
        )
    }
}

/**
 * Razón de bloqueo por categoría
 */
sealed class CategoryBlockReason {
    data class LimitExceeded(
        val categoryName: String,
        val limitMinutes: Int
    ) : CategoryBlockReason()
    
    data class ScheduleBlock(
        val categoryName: String,
        val startTime: String,
        val endTime: String
    ) : CategoryBlockReason()
}

/**
 * Resumen de categorías
 */
data class CategorySummary(
    val totalCategories: Int,
    val enabledCategories: Int,
    val totalAppsInCategories: Int
)

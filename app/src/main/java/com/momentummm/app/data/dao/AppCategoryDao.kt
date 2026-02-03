package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.AppCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    
    @Query("SELECT * FROM app_categories ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<AppCategory>>
    
    @Query("SELECT * FROM app_categories WHERE isLimitEnabled = 1 ORDER BY displayOrder ASC")
    fun getEnabledCategories(): Flow<List<AppCategory>>
    
    @Query("SELECT * FROM app_categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): AppCategory?
    
    @Query("SELECT * FROM app_categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): AppCategory?
    
    @Query("SELECT * FROM app_categories WHERE isSystemCategory = 1 ORDER BY displayOrder ASC")
    fun getSystemCategories(): Flow<List<AppCategory>>
    
    /**
     * Encuentra todas las categorías que contienen un package específico
     */
    @Query("SELECT * FROM app_categories WHERE packageNames LIKE '%' || :packageName || '%'")
    suspend fun getCategoriesForPackage(packageName: String): List<AppCategory>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AppCategory): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AppCategory>)
    
    @Update
    suspend fun updateCategory(category: AppCategory)
    
    @Delete
    suspend fun deleteCategory(category: AppCategory)
    
    @Query("DELETE FROM app_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)
    
    /**
     * Actualiza el límite de una categoría
     */
    @Query("UPDATE app_categories SET dailyLimitMinutes = :limitMinutes, isLimitEnabled = :enabled, updatedAt = :updatedAt WHERE id = :categoryId")
    suspend fun updateCategoryLimit(categoryId: Int, limitMinutes: Int, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Actualiza el horario de bloqueo de una categoría
     */
    @Query("""
        UPDATE app_categories 
        SET hasScheduleLimit = :hasSchedule, 
            scheduleStartHour = :startHour, 
            scheduleStartMinute = :startMinute,
            scheduleEndHour = :endHour,
            scheduleEndMinute = :endMinute,
            scheduleDaysOfWeek = :daysOfWeek,
            updatedAt = :updatedAt 
        WHERE id = :categoryId
    """)
    suspend fun updateCategorySchedule(
        categoryId: Int, 
        hasSchedule: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Actualiza los packages de una categoría
     */
    @Query("UPDATE app_categories SET packageNames = :packageNames, updatedAt = :updatedAt WHERE id = :categoryId")
    suspend fun updateCategoryPackages(categoryId: Int, packageNames: String, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Cuenta el total de categorías
     */
    @Query("SELECT COUNT(*) FROM app_categories")
    suspend fun getCategoryCount(): Int
    
    /**
     * Cuenta categorías con límites activos
     */
    @Query("SELECT COUNT(*) FROM app_categories WHERE isLimitEnabled = 1")
    suspend fun getEnabledCategoryCount(): Int
}

package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.AppLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getAllEnabledLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimitByPackage(packageName: String): AppLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(appLimit: AppLimit)

    @Delete
    suspend fun deleteLimit(appLimit: AppLimit)

    @Query("UPDATE app_limits SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateLimitEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE app_limits SET dailyLimitMinutes = :limitMinutes WHERE packageName = :packageName")
    suspend fun updateDailyLimit(packageName: String, limitMinutes: Int)
    
    // ============================================================
    // BLOQUEO POR HORARIO - Feature: "block certain apps... at a certain time"
    // ============================================================
    
    @Query("""
        UPDATE app_limits 
        SET hasScheduleLimit = :hasSchedule,
            scheduleStartHour = :startHour,
            scheduleStartMinute = :startMinute,
            scheduleEndHour = :endHour,
            scheduleEndMinute = :endMinute,
            scheduleDaysOfWeek = :daysOfWeek,
            updatedAt = :updatedAt
        WHERE packageName = :packageName
    """)
    suspend fun updateScheduleLimit(
        packageName: String,
        hasSchedule: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT * FROM app_limits WHERE hasScheduleLimit = 1 AND isEnabled = 1")
    fun getLimitsWithSchedule(): Flow<List<AppLimit>>
    
    // ============================================================
    // CATEGORÍAS - Feature: "block apps via category"
    // ============================================================
    
    @Query("SELECT * FROM app_limits WHERE categoryId = :categoryId")
    fun getLimitsByCategory(categoryId: Int): Flow<List<AppLimit>>
    
    @Query("UPDATE app_limits SET categoryId = :categoryId WHERE packageName = :packageName")
    suspend fun updateCategory(packageName: String, categoryId: Int?)
    
    // ============================================================
    // BLOQUEO DE EDICIÓN - Feature: No permitir editar límites excedidos
    // ============================================================
    
    @Query("UPDATE app_limits SET lastExceededAt = :timestamp WHERE packageName = :packageName")
    suspend fun markAsExceeded(packageName: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM app_limits WHERE lastExceededAt IS NOT NULL AND lastExceededAt > :todayStart")
    suspend fun getExceededToday(todayStart: Long): List<AppLimit>
}

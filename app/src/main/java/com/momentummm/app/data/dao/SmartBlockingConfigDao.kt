package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.SmartBlockingConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartBlockingConfigDao {
    
    @Query("SELECT * FROM smart_blocking_config WHERE id = 1")
    fun getConfig(): Flow<SmartBlockingConfig?>
    
    @Query("SELECT * FROM smart_blocking_config WHERE id = 1")
    suspend fun getConfigSync(): SmartBlockingConfig?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SmartBlockingConfig)
    
    @Update
    suspend fun updateConfig(config: SmartBlockingConfig)
    
    @Query("UPDATE smart_blocking_config SET sleepModeEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun setSleepModeEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET sleepStartHour = :hour, sleepStartMinute = :minute, updatedAt = :timestamp WHERE id = 1")
    suspend fun setSleepStartTime(hour: Int, minute: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET sleepEndHour = :hour, sleepEndMinute = :minute, updatedAt = :timestamp WHERE id = 1")
    suspend fun setSleepEndTime(hour: Int, minute: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET digitalFastingEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun setDigitalFastingEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET fastingStartHour = :hour, fastingStartMinute = :minute, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFastingStartTime(hour: Int, minute: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET fastingEndHour = :hour, fastingEndMinute = :minute, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFastingEndTime(hour: Int, minute: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET fastingDailyLimitMinutes = :minutes, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFastingDailyLimit(minutes: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET fastingDaysOfWeek = :days, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFastingDays(days: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET nuclearModeEnabled = :enabled, nuclearModeStartDate = :startDate, nuclearModeEndDate = :endDate, nuclearModeDurationDays = :days, nuclearModeApps = :apps, updatedAt = :timestamp WHERE id = 1")
    suspend fun setNuclearMode(
        enabled: Boolean, 
        startDate: java.util.Date?, 
        endDate: java.util.Date?, 
        days: Int, 
        apps: String,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE smart_blocking_config SET nuclearModeCurrentWaitSeconds = :seconds, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNuclearWaitProgress(seconds: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET floatingTimerEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFloatingTimerEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET floatingTimerOpacity = :opacity, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFloatingTimerOpacity(opacity: Float, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET floatingTimerPosition = :position, updatedAt = :timestamp WHERE id = 1")
    suspend fun setFloatingTimerPosition(position: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET streakProtectionEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun setStreakProtectionEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET graceDaysPerWeek = :days, updatedAt = :timestamp WHERE id = 1")
    suspend fun setGraceDaysPerWeek(days: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET graceDaysUsedThisWeek = :used, lastGraceDayResetDate = :resetDate, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateGraceDaysUsed(used: Int, resetDate: java.util.Date?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE smart_blocking_config SET communicationOnlyModeEnabled = :enabled, communicationOnlyApps = :apps, updatedAt = :timestamp WHERE id = 1")
    suspend fun setCommunicationOnlyMode(enabled: Boolean, apps: String, timestamp: Long = System.currentTimeMillis())
}

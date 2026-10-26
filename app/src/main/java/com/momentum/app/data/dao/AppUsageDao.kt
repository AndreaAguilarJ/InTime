package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.AppUsage
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY totalTimeInMillis DESC")
    fun getAppUsageForDate(date: Date): Flow<List<AppUsage>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY totalTimeInMillis DESC")
    fun getAppUsageForPeriod(startDate: Date, endDate: Date): Flow<List<AppUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(appUsage: List<AppUsage>)

    @Query("DELETE FROM app_usage WHERE date < :cutoffDate")
    suspend fun deleteOldUsageData(cutoffDate: Date)
}
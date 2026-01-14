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
}

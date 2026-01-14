package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.AppWhitelist
import kotlinx.coroutines.flow.Flow

@Dao
interface AppWhitelistDao {

    @Query("SELECT * FROM app_whitelist ORDER BY appName ASC")
    fun getAllWhitelistedApps(): Flow<List<AppWhitelist>>

    @Query("SELECT * FROM app_whitelist WHERE packageName = :packageName")
    suspend fun getWhitelistedApp(packageName: String): AppWhitelist?

    @Query("SELECT EXISTS(SELECT 1 FROM app_whitelist WHERE packageName = :packageName)")
    suspend fun isAppWhitelisted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistedApp(app: AppWhitelist)

    @Delete
    suspend fun removeWhitelistedApp(app: AppWhitelist)

    @Query("DELETE FROM app_whitelist WHERE packageName = :packageName")
    suspend fun removeWhitelistedAppByPackage(packageName: String)

    @Query("DELETE FROM app_whitelist")
    suspend fun clearWhitelist()
}


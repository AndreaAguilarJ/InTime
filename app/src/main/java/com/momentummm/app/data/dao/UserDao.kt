package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettings)

    @Update
    suspend fun updateUserSettings(userSettings: UserSettings)
}
package com.momentummm.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.momentummm.app.data.entity.PasswordProtection
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordProtectionDao {
    @Query("SELECT * FROM password_protection WHERE id = 1")
    fun getPasswordProtection(): Flow<PasswordProtection?>

    @Query("SELECT * FROM password_protection WHERE id = 1")
    suspend fun getPasswordProtectionSync(): PasswordProtection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(passwordProtection: PasswordProtection)

    @Update
    suspend fun update(passwordProtection: PasswordProtection)

    @Query("UPDATE password_protection SET failedAttempts = :attempts, lastFailedAttempt = :timestamp WHERE id = 1")
    suspend fun updateFailedAttempts(attempts: Int, timestamp: Long)

    @Query("UPDATE password_protection SET lockoutUntil = :timestamp WHERE id = 1")
    suspend fun updateLockoutUntil(timestamp: Long)

    @Query("UPDATE password_protection SET failedAttempts = 0 WHERE id = 1")
    suspend fun resetFailedAttempts()
}


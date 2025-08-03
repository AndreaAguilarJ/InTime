package com.momentum.app.data.repository

import com.momentum.app.data.dao.UserDao
import com.momentum.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import java.util.Date

class UserRepository(private val userDao: UserDao) {

    fun getUserSettings(): Flow<UserSettings?> {
        return userDao.getUserSettings()
    }

    suspend fun saveUserSettings(userSettings: UserSettings) {
        userDao.insertUserSettings(userSettings)
    }

    suspend fun updateUserSettings(userSettings: UserSettings) {
        userDao.updateUserSettings(userSettings)
    }

    suspend fun setBirthDate(birthDate: Date) {
        val currentSettings = UserSettings(
            birthDate = birthDate,
            isOnboardingCompleted = true
        )
        userDao.insertUserSettings(currentSettings)
    }

    suspend fun updateColors(livedColor: String, futureColor: String, backgroundColor: String) {
        val settings = UserSettings(
            birthDate = null, // Will be merged with existing data
            livedWeeksColor = livedColor,
            futureWeeksColor = futureColor,
            backgroundColor = backgroundColor
        )
        userDao.updateUserSettings(settings)
    }
}
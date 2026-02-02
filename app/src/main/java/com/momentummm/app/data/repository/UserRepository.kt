package com.momentummm.app.data.repository

import com.momentummm.app.data.dao.UserDao
import com.momentummm.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import java.util.Date

class UserRepository(private val userDao: UserDao) {

    fun getUserSettings(): Flow<UserSettings?> {
        return userDao.getUserSettings()
    }

    suspend fun getUserSettingsSync(): UserSettings? {
        return try {
            userDao.getUserSettingsSync()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error getting user settings", e)
            null
        }
    }

    suspend fun saveUserSettings(userSettings: UserSettings) {
        try {
            userDao.insertUserSettings(userSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error saving user settings", e)
        }
    }

    suspend fun updateUserSettings(userSettings: UserSettings) {
        try {
            userDao.updateUserSettings(userSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating user settings", e)
        }
    }

    suspend fun setBirthDate(birthDate: Date) {
        try {
            // Obtener configuraciones existentes para preservar datos
            val existingSettings = userDao.getUserSettingsSync()
            val updatedSettings = existingSettings?.copy(
                birthDate = birthDate,
                isOnboardingCompleted = true
            ) ?: UserSettings(
                birthDate = birthDate,
                isOnboardingCompleted = true
            )
            userDao.insertUserSettings(updatedSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error setting birth date", e)
        }
    }

    suspend fun updateColors(livedColor: String, futureColor: String, backgroundColor: String) {
        try {
            // Obtener configuraciones existentes para preservar datos
            val existingSettings = userDao.getUserSettingsSync()
            val updatedSettings = existingSettings?.copy(
                livedWeeksColor = livedColor,
                futureWeeksColor = futureColor,
                backgroundColor = backgroundColor
            ) ?: UserSettings(
                birthDate = null, // Solución: agregar el parámetro obligatorio
                livedWeeksColor = livedColor,
                futureWeeksColor = futureColor,
                backgroundColor = backgroundColor,
                isOnboardingCompleted = true // Asegurar que está marcado como completado
            )
            userDao.insertUserSettings(updatedSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating colors", e)
        }
    }

    suspend fun markTutorialAsSeen() {
        try {
            // Obtener configuraciones existentes para preservar TODOS los datos
            val existingSettings = userDao.getUserSettingsSync()
            val updatedSettings = existingSettings?.copy(
                isOnboardingCompleted = true,
                hasSeenTutorial = true
            ) ?: UserSettings(
                birthDate = null, // Solución: agregar el parámetro obligatorio
                isOnboardingCompleted = true,
                hasSeenTutorial = true
            )
            userDao.insertUserSettings(updatedSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error marking tutorial as seen", e)
        }
    }

    suspend fun completeOnboarding() {
        try {
            // Marcar definitivamente el onboarding como completado
            val existingSettings = userDao.getUserSettingsSync()
            val updatedSettings = existingSettings?.copy(
                isOnboardingCompleted = true,
                hasSeenTutorial = true
            ) ?: UserSettings(
                birthDate = null, // Solución: agregar el parámetro obligatorio
                isOnboardingCompleted = true,
                hasSeenTutorial = true
            )
            userDao.insertUserSettings(updatedSettings)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error completing onboarding", e)
        }
    }
}
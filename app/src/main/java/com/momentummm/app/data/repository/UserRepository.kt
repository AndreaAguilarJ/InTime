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
        return userDao.getUserSettingsSync()
    }

    suspend fun saveUserSettings(userSettings: UserSettings) {
        userDao.insertUserSettings(userSettings)
    }

    suspend fun updateUserSettings(userSettings: UserSettings) {
        userDao.updateUserSettings(userSettings)
    }

    suspend fun setBirthDate(birthDate: Date) {
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
    }

    suspend fun updateColors(livedColor: String, futureColor: String, backgroundColor: String) {
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
    }

    suspend fun markTutorialAsSeen() {
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
    }

    suspend fun completeOnboarding() {
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
    }
}
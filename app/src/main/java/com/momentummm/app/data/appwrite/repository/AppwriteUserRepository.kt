package com.momentummm.app.data.appwrite.repository

import com.momentummm.app.data.appwrite.AppwriteConfig
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.appwrite.models.AppwriteUserSettings
import io.appwrite.Query
import io.appwrite.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppwriteUserRepository(private val appwriteService: AppwriteService) {
    
    suspend fun createUserSettings(userSettings: AppwriteUserSettings): Result<AppwriteUserSettings> {
        return try {
            val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
            val settingsWithTimestamp = userSettings.copy(
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            val document = appwriteService.databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.USER_SETTINGS_COLLECTION_ID,
                documentId = "unique()",
                data = Json.encodeToString(settingsWithTimestamp)
            )
            
            Result.success(settingsWithTimestamp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserSettings(userId: String): Flow<AppwriteUserSettings?> = flow {
        try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.USER_SETTINGS_COLLECTION_ID,
                queries = listOf(Query.equal("userId", userId))
            )
            
            if (documents.documents.isNotEmpty()) {
                val document = documents.documents.first()
                val settings = Json.decodeFromString<AppwriteUserSettings>(document.data.toString())
                emit(settings)
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    suspend fun updateUserSettings(userId: String, userSettings: AppwriteUserSettings): Result<AppwriteUserSettings> {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.USER_SETTINGS_COLLECTION_ID,
                queries = listOf(Query.equal("userId", userId))
            )
            
            if (documents.documents.isNotEmpty()) {
                val documentId = documents.documents.first().id
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val updatedSettings = userSettings.copy(updatedAt = currentTime)
                
                appwriteService.databases.updateDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.USER_SETTINGS_COLLECTION_ID,
                    documentId = documentId,
                    data = Json.encodeToString(updatedSettings)
                )
                
                Result.success(updatedSettings)
            } else {
                // Create new settings if none exist
                createUserSettings(userSettings)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeOnboarding(userId: String): Result<Unit> {
        return try {
            // Get current settings properly
            var currentSettings: AppwriteUserSettings? = null
            getUserSettings(userId).collect { settings ->
                currentSettings = settings
            }
            
            val settings = currentSettings ?: AppwriteUserSettings(
                userId = userId, 
                birthDate = "", 
                isOnboardingCompleted = true
            )
            
            updateUserSettings(userId, settings.copy(isOnboardingCompleted = true))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sincroniza los datos de gamificación a Appwrite
     */
    suspend fun syncGamificationData(
        userId: String,
        userLevel: Int,
        currentXp: Int,
        totalXp: Int,
        timeCoins: Int,
        currentStreak: Int,
        longestStreak: Int,
        lastActiveDate: String?,
        totalFocusMinutes: Int,
        totalSessionsCompleted: Int,
        perfectDaysCount: Int,
        gamificationEnabled: Boolean,
        showXpNotifications: Boolean,
        showStreakReminders: Boolean
    ): Result<Unit> {
        return try {
            var currentSettings: AppwriteUserSettings? = null
            getUserSettings(userId).collect { settings ->
                currentSettings = settings
            }
            
            val settings = currentSettings ?: AppwriteUserSettings(
                userId = userId,
                birthDate = ""
            )
            
            val updatedSettings = settings.copy(
                userLevel = userLevel,
                currentXp = currentXp,
                totalXp = totalXp,
                timeCoins = timeCoins,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastActiveDate = lastActiveDate ?: "",
                totalFocusMinutes = totalFocusMinutes,
                totalSessionsCompleted = totalSessionsCompleted,
                perfectDaysCount = perfectDaysCount,
                gamificationEnabled = gamificationEnabled,
                showXpNotifications = showXpNotifications,
                showStreakReminders = showStreakReminders
            )
            
            updateUserSettings(userId, updatedSettings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene los datos de gamificación desde Appwrite
     */
    suspend fun getGamificationData(userId: String): Result<GamificationCloudData?> {
        return try {
            var cloudSettings: AppwriteUserSettings? = null
            getUserSettings(userId).collect { settings ->
                cloudSettings = settings
            }
            
            cloudSettings?.let { settings ->
                Result.success(GamificationCloudData(
                    userLevel = settings.userLevel,
                    currentXp = settings.currentXp,
                    totalXp = settings.totalXp,
                    timeCoins = settings.timeCoins,
                    currentStreak = settings.currentStreak,
                    longestStreak = settings.longestStreak,
                    lastActiveDate = settings.lastActiveDate,
                    totalFocusMinutes = settings.totalFocusMinutes,
                    totalSessionsCompleted = settings.totalSessionsCompleted,
                    perfectDaysCount = settings.perfectDaysCount,
                    gamificationEnabled = settings.gamificationEnabled,
                    showXpNotifications = settings.showXpNotifications,
                    showStreakReminders = settings.showStreakReminders
                ))
            } ?: Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Data class para datos de gamificación de la nube
 */
data class GamificationCloudData(
    val userLevel: Int,
    val currentXp: Int,
    val totalXp: Int,
    val timeCoins: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String?,
    val totalFocusMinutes: Int,
    val totalSessionsCompleted: Int,
    val perfectDaysCount: Int,
    val gamificationEnabled: Boolean,
    val showXpNotifications: Boolean,
    val showStreakReminders: Boolean
)
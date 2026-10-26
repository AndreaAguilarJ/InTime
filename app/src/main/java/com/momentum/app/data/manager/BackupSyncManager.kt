package com.momentummm.app.data.manager

import android.content.Context
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.entity.*
import com.momentummm.app.data.repository.*
import com.momentummm.app.ui.screen.focus.FocusSession
import com.momentummm.app.ui.screen.goals.Goal
import io.appwrite.models.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupSyncManager(
    private val context: Context,
    private val appwriteService: AppwriteService,
    private val usageStatsRepository: UsageStatsRepository,
    private val userRepository: UserRepository,
    private val quotesRepository: QuotesRepository
) {
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()
    
    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress: StateFlow<Float> = _backupProgress.asStateFlow()
    
    enum class SyncStatus {
        Idle, Syncing, Success, Failed, NoConnection
    }
    
    data class BackupData(
        val usageStats: List<AppUsageInfo>,
        val userSettings: UserSettings?,
        val quotes: List<Quote>,
        val focusSessions: List<FocusSession>,
        val goals: List<Goal>,
        val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
    
    suspend fun performFullBackup(userId: String): Result<String> {
        return try {
            _syncStatus.value = SyncStatus.Syncing
            _backupProgress.value = 0f
            
            // Collect all user data
            val usageStats = usageStatsRepository.getTodayUsageStats()
            _backupProgress.value = 0.2f
            
            val userSettings = userRepository.getUserSettings().first()
            _backupProgress.value = 0.4f
            
            val quotes = quotesRepository.getAllQuotes().first()
            _backupProgress.value = 0.6f
            
            // Create backup data structure
            val backupData = BackupData(
                usageStats = usageStats,
                userSettings = userSettings,
                quotes = quotes,
                focusSessions = emptyList(), // Would get from focus repository
                goals = emptyList() // Would get from goals repository
            )
            
            _backupProgress.value = 0.8f
            
            // Upload to Appwrite
            val backupJson = Json.encodeToString(backupData)
            val document = appwriteService.databases.createDocument(
                databaseId = appwriteService.databaseId,
                collectionId = "backups",
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "userId" to userId,
                    "backupData" to backupJson,
                    "timestamp" to backupData.timestamp,
                    "version" to "1.0"
                )
            )
            
            _backupProgress.value = 1f
            _syncStatus.value = SyncStatus.Success
            _lastSyncTime.value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            
            Result.success(document.id)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            _backupProgress.value = 0f
            Result.failure(e)
        }
    }
    
    suspend fun restoreFromBackup(userId: String, backupId: String? = null): Result<Unit> {
        return try {
            _syncStatus.value = SyncStatus.Syncing
            _backupProgress.value = 0f
            
            // Get the latest backup or specific backup
            val documents = if (backupId != null) {
                listOf(appwriteService.databases.getDocument(
                    databaseId = appwriteService.databaseId,
                    collectionId = "backups",
                    documentId = backupId
                ))
            } else {
                appwriteService.databases.listDocuments(
                    databaseId = appwriteService.databaseId,
                    collectionId = "backups",
                    queries = listOf(
                        io.appwrite.Query.equal("userId", userId),
                        io.appwrite.Query.orderDesc("timestamp"),
                        io.appwrite.Query.limit(1)
                    )
                ).documents
            }
            
            if (documents.isEmpty()) {
                _syncStatus.value = SyncStatus.Failed
                return Result.failure(Exception("No backup found"))
            }
            
            _backupProgress.value = 0.2f
            
            val backupDoc = documents.first()
            val backupJson = backupDoc.data["backupData"] as String
            val backupData = Json.decodeFromString<BackupData>(backupJson)
            
            _backupProgress.value = 0.4f
            
            // Restore user settings
            backupData.userSettings?.let { settings ->
                userRepository.updateUserSettings(settings)
            }
            
            _backupProgress.value = 0.6f
            
            // Restore usage stats
            backupData.usageStats.forEach { stats ->
                // Note: insertUsageStats method needs to be implemented in UsageStatsRepository
                // usageStatsRepository.insertUsageStats(stats)
            }
            
            _backupProgress.value = 0.8f
            
            // Restore quotes
            backupData.quotes.forEach { quote ->
                // Note: insertQuote method needs to be implemented in QuotesRepository
                // quotesRepository.insertQuote(quote)
            }
            
            _backupProgress.value = 1f
            _syncStatus.value = SyncStatus.Success
            _lastSyncTime.value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            _backupProgress.value = 0f
            Result.failure(e)
        }
    }
    
    suspend fun syncUsageStats(userId: String): Result<Unit> {
        return try {
            _syncStatus.value = SyncStatus.Syncing
            
            // Get local usage stats that haven't been synced
            val localStats = usageStatsRepository.getTodayUsageStats()

            // Upload each usage stat
            localStats.forEach { stats ->
                try {
                    appwriteService.databases.createDocument(
                        databaseId = appwriteService.databaseId,
                        collectionId = "usage_stats",
                        documentId = io.appwrite.ID.unique(),
                        data = mapOf(
                            "userId" to userId,
                            "packageName" to stats.packageName,
                            "appName" to stats.appName,
                            "totalTimeInMillis" to stats.totalTimeInMillis,
                            "lastTimeUsed" to stats.lastTimeUsed
                        )
                    )
                } catch (e: Exception) {
                    // Skip if already exists or other error
                }
            }
            
            _syncStatus.value = SyncStatus.Success
            _lastSyncTime.value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            Result.failure(e)
        }
    }
    
    suspend fun downloadCloudData(userId: String): Result<Unit> {
        return try {
            _syncStatus.value = SyncStatus.Syncing
            
            // Download usage stats from cloud
            val usageStatsDocuments = appwriteService.databases.listDocuments(
                databaseId = appwriteService.databaseId,
                collectionId = "usage_stats",
                queries = listOf(io.appwrite.Query.equal("userId", userId))
            )
            
            // Convert and save to local database
            usageStatsDocuments.documents.forEach { doc ->
                // Note: Need to create proper data class and repository method
                /*
                val usageStats = UsageStats(
                    packageName = doc.data["packageName"] as String,
                    appName = doc.data["appName"] as String,
                    totalTimeInForeground = (doc.data["totalTimeInForeground"] as Number).toLong(),
                    launchCount = (doc.data["launchCount"] as Number).toInt(),
                    date = doc.data["date"] as String,
                    category = doc.data["category"] as? String,
                    lastUsed = doc.data["lastUsed"] as String
                )
                usageStatsRepository.insertUsageStats(usageStats)
                */
            }
            
            _syncStatus.value = SyncStatus.Success
            _lastSyncTime.value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableBackups(userId: String): Result<List<BackupInfo>> {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = appwriteService.databaseId,
                collectionId = "backups",
                queries = listOf(
                    io.appwrite.Query.equal("userId", userId),
                    io.appwrite.Query.orderDesc("timestamp")
                )
            )
            
            val backupInfoList = documents.documents.map { doc ->
                BackupInfo(
                    id = doc.id,
                    timestamp = doc.data["timestamp"] as String,
                    version = doc.data["version"] as? String ?: "1.0",
                    size = (doc.data["backupData"] as String).length
                )
            }
            
            Result.success(backupInfoList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBackup(backupId: String): Result<Unit> {
        return try {
            appwriteService.databases.deleteDocument(
                databaseId = appwriteService.databaseId,
                collectionId = "backups",
                documentId = backupId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun scheduleAutoBackup(userId: String, intervalHours: Int = 24): Result<Unit> {
        return try {
            // This would integrate with WorkManager to schedule periodic backups
            // For now, just updating the preference
            appwriteService.databases.createDocument(
                databaseId = appwriteService.databaseId,
                collectionId = "user_settings",
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "userId" to userId,
                    "autoBackupEnabled" to true,
                    "autoBackupInterval" to intervalHours,
                    "lastBackupCheck" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
        _backupProgress.value = 0f
    }
    
    data class BackupInfo(
        val id: String,
        val timestamp: String,
        val version: String,
        val size: Int
    )
    
    // Dummy data classes for compilation (these would be defined elsewhere)
    data class FocusSession(
        val id: Long = 0,
        val sessionType: String,
        val duration: Int,
        val completed: Boolean,
        val date: String
    )
    
    data class Goal(
        val id: Long = 0,
        val goalType: String,
        val target: Int,
        val achieved: Int,
        val date: String
    )
}
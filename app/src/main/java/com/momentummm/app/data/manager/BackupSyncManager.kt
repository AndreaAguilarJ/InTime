package com.momentummm.app.data.manager

import android.content.Context
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.entity.*
import com.momentummm.app.data.repository.*
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
    private val quotesRepository: QuotesRepository,
    private val goalsRepository: GoalsRepository
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
    
    // El formato de las copias vive en BackupPayload.kt: es @Serializable, usa
    // tipos primitivos y está desacoplado de Room, así que una copia hecha hoy
    // se sigue leyendo si mañana cambia el esquema de la base de datos.
    
    suspend fun performFullBackup(userId: String): Result<String> {
        return try {
            _syncStatus.value = SyncStatus.Syncing
            _backupProgress.value = 0f
            
            // Collect all user data con timeout para evitar bloqueo infinito
            val usageStats = usageStatsRepository.getTodayUsageStats()
            _backupProgress.value = 0.2f
            
            val userSettings = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                userRepository.getUserSettings().first()
            }
            _backupProgress.value = 0.4f
            
            val quotes = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                quotesRepository.getAllQuotes().first()
            } ?: emptyList()
            _backupProgress.value = 0.6f
            
            // BUG CORREGIDO: `goals` iba a `emptyList()` a fuego, así que las
            // copias de seguridad NUNCA incluían las metas del usuario. Se
            // creaba la copia, se restauraba y las metas habían desaparecido.
            val goals = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                goalsRepository.getAllGoals().first()
            } ?: emptyList()

            // Create backup data structure
            // Las sesiones de enfoque son plantillas definidas en el código, no
            // datos del usuario, así que el formato de copia no las incluye.
            val payload = BackupPayload(
                userSettings = userSettings?.toBackup(),
                quotes = quotes.map { it.toBackup() },
                goals = goals.map { it.toBackup() },
                usageStats = usageStats.map { it.toBackup() }
            )
            
            _backupProgress.value = 0.8f
            
            // Upload to Appwrite
            val backupJson = Json.encodeToString(payload)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val document = appwriteService.databases.createDocument(
                databaseId = appwriteService.databaseId,
                collectionId = "backups",
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "userId" to userId,
                    "backupData" to backupJson,
                    "timestamp" to timestamp,
                    "version" to BackupPayload.CURRENT_VERSION.toString()
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
            val backupJson = backupDoc.data["backupData"] as? String
            if (backupJson == null) {
                _syncStatus.value = SyncStatus.Failed
                return Result.failure(Exception("Invalid backup format"))
            }
            
            // Parse JSON con manejo de errores robusto
            val backupData = try {
                Json { ignoreUnknownKeys = true }.decodeFromString<BackupPayload>(backupJson)
            } catch (e: kotlinx.serialization.SerializationException) {
                _syncStatus.value = SyncStatus.Failed
                return Result.failure(Exception("Corrupted backup data: ${e.message}"))
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed
                return Result.failure(Exception("Failed to parse backup: ${e.message}"))
            }
            
            _backupProgress.value = 0.4f
            
            // Restore user settings
            backupData.userSettings?.let { settingsBackup ->
                // Se parte de los ajustes actuales: si una copia antigua no trae
                // un campo, se conserva el valor local en vez de machacarlo con
                // el valor por defecto.
                val current = userRepository.getUserSettingsSync()
                if (current != null) {
                    userRepository.updateUserSettings(settingsBackup.toEntity(current))
                }
            }
            
            _backupProgress.value = 0.6f

            // BUG CORREGIDO: aquí había dos bucles con la inserción comentada
            // (`// quotesRepository.insertQuote(quote)`), y a continuación la
            // barra saltaba al 100 % y se marcaba `SyncStatus.Success`. El
            // usuario veía una restauración perfecta y sus frases y metas
            // seguían sin aparecer.
            //
            // Nota sobre las estadísticas de uso: NO se restauran porque no son
            // nuestras. `getTodayUsageStats()` las lee de UsageStatsManager, que
            // es propiedad de Android; se guardan en la copia como registro
            // histórico, pero escribirlas de vuelta es imposible.

            if (backupData.quotes.isNotEmpty()) {
                quotesRepository.insertQuotes(backupData.quotes.map { it.toEntity() })
            }

            _backupProgress.value = 0.8f

            // Restaurar metas. El modelo de la copia es el de la interfaz, así
            // que hay que convertirlo al de la base de datos.
            var restoredGoals = 0
            backupData.goals.forEach { goalBackup ->
                runCatching { goalsRepository.createGoal(goalBackup.toEntity()) }
                    .onSuccess { restoredGoals++ }
                    .onFailure { println("BackupSyncManager: meta no restaurada: ${it.message}") }
            }
            println("BackupSyncManager: ${backupData.quotes.size} frases y $restoredGoals metas restauradas")

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
            
            val backupInfoList = documents.documents.mapNotNull { doc ->
                try {
                    val timestamp = doc.data["timestamp"] as? String ?: return@mapNotNull null
                    val backupData = doc.data["backupData"] as? String ?: ""
                    BackupInfo(
                        id = doc.id,
                        timestamp = timestamp,
                        version = doc.data["version"] as? String ?: "1.0",
                        size = backupData.length
                    )
                } catch (e: Exception) {
                    null
                }
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
    
    // Las clases de relleno que había aquí (FocusSession y Goal, con un
    // comentario que admitía ser "dummy data classes for compilation") ya no
    // existen: el formato de copia real está en BackupPayload.kt.
}


// La conversión copia → entidad vive en BackupPayload.kt (GoalBackup.toEntity),
// que trabaja con milisegundos y no necesita parsear fechas de texto.

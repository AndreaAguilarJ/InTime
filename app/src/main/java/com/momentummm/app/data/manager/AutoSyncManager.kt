package com.momentummm.app.data.manager

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Administrador de sincronización automática que guarda todos los datos del usuario
 * cada vez que se cierra o pausa la aplicación, y sincroniza con Appwrite
 */
class AutoSyncManager(
    private val context: Context,
    private val appwriteService: AppwriteService,
    private val userRepository: UserRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val goalsRepository: GoalsRepository,
    private val appLimitRepository: AppLimitRepository,
    private val appWhitelistRepository: AppWhitelistRepository,
    private val quotesRepository: QuotesRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    enum class SyncStatus {
        Idle, Syncing, Success, Failed
    }

    init {
        // Cargar última vez sincronizado
        loadLastSyncTime()
    }

    /**
     * Se llama cuando la app pasa a background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Guardar todos los datos localmente y sincronizar con Appwrite
        scope.launch {
            saveAllDataLocally()
            syncToAppwrite()
        }
    }

    /**
     * Se llama cuando la app vuelve a foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Sincronizar datos desde Appwrite si han pasado más de 5 minutos
        scope.launch {
            val shouldSync = shouldSyncFromServer()
            if (shouldSync) {
                syncFromAppwrite()
            }
        }
    }

    /**
     * Guarda todos los datos localmente en Room y DataStore
     */
    private suspend fun saveAllDataLocally() {
        try {
            // Los datos ya se guardan automáticamente en Room cuando se modifican
            // Este método es para asegurar que todo está persistido

            // Forzar flush de DataStore
            UserPreferencesRepository.getWidgetColors(context)

            // Log para debugging
            println("AutoSyncManager: Datos guardados localmente")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sincroniza todos los datos con Appwrite
     */
    suspend fun syncToAppwrite() {
        if (!appwriteService.isLoggedIn.value) {
            println("AutoSyncManager: Usuario no autenticado, omitiendo sincronización")
            _syncStatus.value = SyncStatus.Failed
            return
        }

        try {
            // Timeout de 10 segundos para prevenir ANR
            withTimeout(10000L) {
                _syncStatus.value = SyncStatus.Syncing

                val userId = appwriteService.currentUser.value?.id ?: run {
                    println("AutoSyncManager: No se pudo obtener el ID del usuario")
                    _syncStatus.value = SyncStatus.Failed
                    return@withTimeout
                }

                println("AutoSyncManager: Iniciando sincronización para usuario: $userId")

                // Recopilar todos los datos en background con timeout individual
                val userSettings = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(2000L) {
                        userRepository.getUserSettings().first()
                    }
                }
                val widgetColors = withContext(Dispatchers.IO) {
                    UserPreferencesRepository.getWidgetColors(context)
                }
                val goals = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(2000L) {
                        goalsRepository.getAllGoals().first()
                    } ?: emptyList()
                }
                val appLimits = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(2000L) {
                        appLimitRepository.getAllLimits().first()
                    } ?: emptyList()
                }
                val whitelistedApps = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(2000L) {
                        appWhitelistRepository.getAllWhitelistedApps().first()
                    } ?: emptyList()
                }
                val customQuotes = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(2000L) {
                        quotesRepository.getAllQuotes().first()
                    } ?: emptyList()
                }

            println("AutoSyncManager: Datos recopilados - Goals: ${goals.size}, Limits: ${appLimits.size}, Whitelist: ${whitelistedApps.size}")

            // Preparar datos para sincronización - SOLO CAMPOS QUE EXISTEN EN APPWRITE
            val syncData = mutableMapOf<String, Any>(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),

                // Configuración del usuario - CONVERTIR birthDate A FORMATO ISO 8601
                "birthDate" to if (userSettings?.birthDate != null) {
                    // Convertir Date a formato ISO 8601 que espera Appwrite
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                    dateFormat.format(userSettings.birthDate)
                } else {
                    // Fecha por defecto si no hay birthDate
                    "2000-01-01T00:00:00Z"
                },
                "isOnboardingCompleted" to (userSettings?.isOnboardingCompleted ?: false),
                "hasSeenTutorial" to (userSettings?.hasSeenTutorial ?: false),
                "livedWeeksColor" to (userSettings?.livedWeeksColor ?: "#6366F1"),
                "futureWeeksColor" to (userSettings?.futureWeeksColor ?: "#E5E7EB"),
                "backgroundColor" to (userSettings?.backgroundColor ?: "#FFFFFF"),

                // Campos requeridos por la colección (basados en errores anteriores)
                "enableBiometric" to false,
                "minimalModeEnabled" to false,
                "allowedApps" to "", // String vacío en lugar de lista vacía
                "dailyQuotesEnabled" to true,

                // Colores del widget
                "widgetLivedColor" to (widgetColors.first ?: "#6366F1"),
                "widgetFutureColor" to (widgetColors.second ?: "#E5E7EB"),

                // Contadores
                "goalsCount" to goals.size,
                "appLimitsCount" to appLimits.size,
                "whitelistedAppsCount" to whitelistedApps.size,
                "customQuotesCount" to customQuotes.size
            )

            println("AutoSyncManager: Sincronizando ${syncData.size} campos")

            // Sincronizar con Appwrite usando el collection ID correcto
            try {
                val collectionId = "user_settings" // Por ahora usar este ID, lo crearemos en Appwrite

                println("AutoSyncManager: Intentando listar documentos existentes...")

                // Primero intentar actualizar documento existente
                val existingDocs = try {
                    appwriteService.databases.listDocuments(
                        databaseId = appwriteService.databaseId,
                        collectionId = collectionId,
                        queries = listOf()
                    )
                } catch (e: Exception) {
                    println("AutoSyncManager: Error al listar documentos - ${e.message}")
                    println("AutoSyncManager: Tipo de error: ${e.javaClass.simpleName}")
                    e.printStackTrace()
                    null
                }

                if (existingDocs != null && existingDocs.documents.isNotEmpty()) {
                    println("AutoSyncManager: Encontrados ${existingDocs.documents.size} documentos existentes")
                    // Buscar documento del usuario actual
                    val userDoc = existingDocs.documents.find {
                        (it.data["userId"] as? String) == userId
                    }

                    if (userDoc != null) {
                        // Actualizar documento existente
                        try {
                            println("AutoSyncManager: Actualizando documento existente: ${userDoc.id}")
                            appwriteService.databases.updateDocument(
                                databaseId = appwriteService.databaseId,
                                collectionId = collectionId,
                                documentId = userDoc.id,
                                data = syncData
                            )
                            _syncStatus.value = SyncStatus.Success
                            saveLastSyncTime()
                            println("AutoSyncManager: OK Sincronización exitosa (actualización)")
                        } catch (e: Exception) {
                            println("AutoSyncManager: X Error al actualizar documento - ${e.message}")
                            e.printStackTrace()
                            _syncStatus.value = SyncStatus.Failed
                        }
                    } else {
                        // Crear nuevo documento para este usuario
                        try {
                            println("AutoSyncManager: Creando nuevo documento para usuario $userId")
                            appwriteService.databases.createDocument(
                                databaseId = appwriteService.databaseId,
                                collectionId = collectionId,
                                documentId = io.appwrite.ID.unique(),
                                data = syncData
                            )
                            _syncStatus.value = SyncStatus.Success
                            saveLastSyncTime()
                            println("AutoSyncManager: OK Sincronización exitosa (creación)")
                        } catch (e: Exception) {
                            println("AutoSyncManager: X Error al crear documento - ${e.message}")
                            e.printStackTrace()
                            _syncStatus.value = SyncStatus.Failed
                        }
                    }
                } else {
                    // Crear primer documento
                    try {
                        println("AutoSyncManager: No hay documentos, creando primero...")
                        appwriteService.databases.createDocument(
                            databaseId = appwriteService.databaseId,
                            collectionId = collectionId,
                            documentId = io.appwrite.ID.unique(),
                            data = syncData
                        )
                        _syncStatus.value = SyncStatus.Success
                        saveLastSyncTime()
                        println("AutoSyncManager: OK Sincronización exitosa (primer documento)")
                    } catch (e: Exception) {
                        println("AutoSyncManager: X Error al crear primer documento - ${e.message}")
                        println("AutoSyncManager: Detalles del error:")
                        e.printStackTrace()
                        _syncStatus.value = SyncStatus.Failed
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = SyncStatus.Failed
                println("AutoSyncManager: Error al sincronizar con Appwrite")
            }
            
            } // Cierre del withTimeout

        } catch (e: TimeoutCancellationException) {
            _syncStatus.value = SyncStatus.Failed
            println("AutoSyncManager: Timeout de sincronizacion")
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            e.printStackTrace()
            println("AutoSyncManager: Error general en sincronizacion")
        }
    }

    /**
     * Sincroniza datos desde Appwrite
     */
    private suspend fun syncFromAppwrite() {
        if (!appwriteService.isLoggedIn.value) return

        try {
            _syncStatus.value = SyncStatus.Syncing

            val userId = appwriteService.currentUser.value?.id ?: return

            // Obtener datos del servidor
            val docs = appwriteService.databases.listDocuments(
                databaseId = appwriteService.databaseId,
                collectionId = "user_settings",
                queries = listOf("equal(\"userId\", \"$userId\")")
            )

            if (docs.documents.isEmpty()) {
                _syncStatus.value = SyncStatus.Idle
                return
            }

            val serverData = docs.documents.first().data as Map<*, *>

            // Restaurar configuración del usuario
            val birthDate = (serverData["birthDate"] as? Number)?.toLong()
            if (birthDate != null && birthDate > 0) {
                userRepository.setBirthDate(Date(birthDate))
            }

            // Restaurar colores
            val livedColor = serverData["livedWeeksColor"] as? String ?: "#6366F1"
            val futureColor = serverData["futureWeeksColor"] as? String ?: "#E5E7EB"
            val bgColor = serverData["backgroundColor"] as? String ?: "#FFFFFF"
            userRepository.updateColors(livedColor, futureColor, bgColor)

            // Restaurar colores del widget
            val widgetLivedColor = serverData["widgetLivedColor"] as? String ?: "#6366F1"
            val widgetFutureColor = serverData["widgetFutureColor"] as? String ?: "#E5E7EB"
            UserPreferencesRepository.setWidgetColors(context, widgetLivedColor, widgetFutureColor)

            // Marcar tutorial como visto si corresponde
            val hasSeenTutorial = serverData["hasSeenTutorial"] as? Boolean ?: false
            if (hasSeenTutorial) {
                userRepository.markTutorialAsSeen()
            }

            _syncStatus.value = SyncStatus.Success
            println("AutoSyncManager: Datos restaurados desde Appwrite")

        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Failed
            e.printStackTrace()
        }
    }

    private fun shouldSyncFromServer(): Boolean {
        // Sincronizar si han pasado más de 5 minutos
        // Por ahora siempre retornamos false para evitar sobrescribir datos locales
        return false
    }

    private fun loadLastSyncTime() {
        scope.launch {
            // Cargar desde SharedPreferences
            val prefs = context.getSharedPreferences("auto_sync", Context.MODE_PRIVATE)
            _lastSyncTime.value = prefs.getString("last_sync_time", null)
        }
    }

    private fun saveLastSyncTime() {
        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        _lastSyncTime.value = currentTime

        // Guardar en SharedPreferences
        val prefs = context.getSharedPreferences("auto_sync", Context.MODE_PRIVATE)
        prefs.edit().putString("last_sync_time", currentTime).apply()
    }

    /**
     * Forzar sincronización manual
     */
    suspend fun forceSyncNow() {
        saveAllDataLocally()
        syncToAppwrite()
    }

    fun cleanup() {
        scope.cancel()
    }
}

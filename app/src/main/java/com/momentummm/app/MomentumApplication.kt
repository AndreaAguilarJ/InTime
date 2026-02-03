package com.momentummm.app

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.work.Configuration
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.repository.UserRepository
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.data.repository.QuotesRepository
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.appwrite.repository.AppwriteUserRepository
import com.momentummm.app.data.appwrite.repository.AppwriteQuotesRepository
import com.momentummm.app.data.appwrite.repository.AppwriteFocusSessionRepository
import com.momentummm.app.data.repository.SubscriptionRepository
import com.momentummm.app.data.manager.ThemeManager
import com.momentummm.app.data.manager.BillingManager
import com.momentummm.app.data.manager.SmartNotificationManager
import com.momentummm.app.data.manager.ExportManager
import com.momentummm.app.data.manager.BackupSyncManager
import com.momentummm.app.data.manager.AutoSyncManager
import com.momentummm.app.data.manager.GamificationManager
import com.momentummm.app.data.manager.MotivationalNotificationManager
import com.momentummm.app.data.repository.GoalsRepository
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.data.repository.AppWhitelistRepository
import com.momentummm.app.minimal.MinimalPhoneManager
import com.momentummm.app.minimal.LauncherManager
import com.momentummm.app.security.AppLockManager
import com.momentummm.app.security.BiometricPromptManager
import com.momentummm.app.worker.MotivationalNotificationWorker
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@HiltAndroidApp
class MomentumApplication : Application(), Configuration.Provider {
    
    // Security managers (injected by Hilt)
    @Inject
    lateinit var appLockManager: AppLockManager
    
    @Inject
    lateinit var biometricPromptManager: BiometricPromptManager
    
    // Database instance (keeping for migration)
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Appwrite service
    val appwriteService by lazy { AppwriteService(this) }
    
    // Minimal phone manager
    val minimalPhoneManager by lazy { MinimalPhoneManager(this) }
    
    // Legacy repositories (keeping for migration)
    val userRepository by lazy { UserRepository(database.userDao()) }
    val usageStatsRepository by lazy { UsageStatsRepository(this) }
    val quotesRepository by lazy { QuotesRepository(database.quoteDao()) }
    
    // New Appwrite repositories
    val appwriteUserRepository by lazy { AppwriteUserRepository(appwriteService) }
    val appwriteQuotesRepository by lazy { AppwriteQuotesRepository(appwriteService) }
    val appwriteFocusSessionRepository by lazy { AppwriteFocusSessionRepository(appwriteService) }

    // Subscription management
    val subscriptionRepository by lazy { SubscriptionRepository(appwriteService) }
    
    // Enhanced managers
    val themeManager by lazy { ThemeManager(this) }
    val billingManager by lazy { BillingManager(this) }
    val exportManager by lazy { ExportManager(this) }
    val gamificationManager by lazy { 
        GamificationManager(database.userDao(), this).also { manager ->
            // Se configurará el NotificationManager después de que esté disponible
        }
    }
    val backupSyncManager by lazy { 
        BackupSyncManager(
            this, 
            appwriteService, 
            usageStatsRepository, 
            userRepository, 
            quotesRepository
        ) 
    }

    // Repositories for goals and app usage management (ordenados para evitar dependencias circulares)
    val appWhitelistRepository by lazy { AppWhitelistRepository(database.appWhitelistDao(), this) }

    val goalsRepository by lazy { GoalsRepository(database.goalDao(), database.challengeDao()) }

    val appLimitRepository by lazy {
        AppLimitRepository(
            database.appLimitDao(),
            this,
            usageStatsRepository,
            appWhitelistRepository
        )
    }

    // Sistema de notificaciones inteligentes
    val smartNotificationManager by lazy {
        SmartNotificationManager(
            this,
            usageStatsRepository,
            goalsRepository,
            appLimitRepository,
            quotesRepository,
            userRepository
        )
    }

    val autoSyncManager by lazy {
        AutoSyncManager(
            this,
            appwriteService,
            userRepository,
            usageStatsRepository,
            goalsRepository,
            appLimitRepository,
            appWhitelistRepository,
            quotesRepository
        )
    }

    // Motivational messages system
    val motivationalMessagesRepository by lazy {
        MotivationalMessagesRepository(
            database.motivationalMessageDao(),
            database.messageReactionDao(),
            database.motivationalPreferencesDao(),
            this
        )
    }

    val motivationalNotificationManager by lazy {
        MotivationalNotificationManager(this, motivationalMessagesRepository)
    }

    private object PreferencesKeys {
        val QUOTES_SEEDED = booleanPreferencesKey("quotes_seeded")
        val MOTIVATIONAL_MESSAGES_SEEDED = booleanPreferencesKey("motivational_messages_seeded")
        val MOTIVATIONAL_NOTIFICATIONS_SCHEDULED = booleanPreferencesKey("motivational_notifications_scheduled")
    }
    
    // Scope con SupervisorJob para evitar que un error cancele todo
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + 
        kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("MomentumApplication", "Error in background coroutine", throwable)
        }
    )
    
    private fun seedDefaultQuotesIfNeeded() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val preferences = dataStore.data.first()
                val quotesSeeded = preferences[PreferencesKeys.QUOTES_SEEDED] ?: false
                
                if (!quotesSeeded) {
                    appwriteQuotesRepository.seedQuotes()
                    
                    // Mark as seeded
                    dataStore.edit { preferences ->
                        preferences[PreferencesKeys.QUOTES_SEEDED] = true
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.e("MomentumApplication", "Error seeding quotes: ${e.message}")
            }
        }
    }

    /**
     * Initialize motivational messages seed data and schedule notifications
     */
    private fun initializeMotivationalMessagesSystem() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Seed motivational messages if needed
                motivationalMessagesRepository.initializeSeedDataIfNeeded()
                android.util.Log.d("MomentumApplication", "Motivational messages initialized")
                
                // Schedule periodic notifications if not already scheduled
                val preferences = dataStore.data.first()
                val notificationsScheduled = preferences[PreferencesKeys.MOTIVATIONAL_NOTIFICATIONS_SCHEDULED] ?: false
                
                if (!notificationsScheduled) {
                    // Schedule periodic motivational notifications
                    MotivationalNotificationWorker.schedulePeriodicNotifications(this@MomentumApplication)
                    MotivationalNotificationWorker.scheduleMorningMessage(this@MomentumApplication, 8, 0)
                    MotivationalNotificationWorker.scheduleEveningMessage(this@MomentumApplication, 21, 0)
                    
                    // Mark as scheduled
                    dataStore.edit { prefs ->
                        prefs[PreferencesKeys.MOTIVATIONAL_NOTIFICATIONS_SCHEDULED] = true
                    }
                    android.util.Log.d("MomentumApplication", "Motivational notifications scheduled")
                }
            } catch (e: Exception) {
                android.util.Log.e("MomentumApplication", "Error initializing motivational messages: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Inicialización en background para no bloquear el hilo principal
        // Usa applicationScope con SupervisorJob para manejo de errores robusto
        applicationScope.launch {
            try {
                // Inicializar managers no críticos en background
                billingManager.startConnection()
                
                // Inicializar sistema de notificaciones inteligentes
                smartNotificationManager
                
                // Conectar GamificationManager con SmartNotificationManager
                gamificationManager.setNotificationManager(smartNotificationManager)
                
                // Initialize Appwrite quotes if needed
                seedDefaultQuotesIfNeeded()
                
                // Initialize motivational messages system and schedule notifications
                initializeMotivationalMessagesSystem()
            } catch (e: Exception) {
                android.util.Log.e("MomentumApplication", "Error in background init", e)
            }
        }
        
        // Habilitar StrictMode en modo debug para detectar operaciones lentas en el hilo principal
        // NOTA: Deshabilitado penaltyFlashScreen para evitar flashes rojos molestos
        // Las violaciones se registran solo en logcat
        if (com.momentummm.app.BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog() // Solo log violations, sin flash
                    .build()
            )
            
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
        
        // Remove manual WorkManager initialization to avoid double initialization crash.
        // WorkManager is auto-initialized via App Startup when Configuration.Provider is implemented.

        // Start widget update worker
        com.momentummm.app.worker.WidgetUpdateWorker.startPeriodicUpdate(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
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

    /**
     * ─── POR QUÉ ESTOS DOS SE INYECTAN ────────────────────────────────────
     * BUG CORREGIDO: aquí se construían a mano con `by lazy`, mientras Hilt
     * creaba sus propios singletons de las mismas clases (ambas están
     * anotadas `@Singleton @Inject constructor`). Existían por tanto DOS
     * GamificationManager y DOS SmartNotificationManager:
     *
     *  - Las pantallas que hacen `applicationContext as MomentumApplication`
     *    usaban los de aquí.
     *  - DashboardViewModel, BlockingViewModel y AppMonitoringService usaban
     *    los de Hilt.
     *
     * Y `setNotificationManager(...)` sólo se llamaba sobre el par de aquí, así
     * que todo el XP otorgado por la vía de Hilt no generaba ninguna
     * notificación de subida de nivel. Inyectándolos hay una única instancia
     * compartida y el cableado vale para toda la app.
     */
    @Inject
    lateinit var gamificationManager: GamificationManager

    @Inject
    lateinit var smartNotificationManager: SmartNotificationManager
    
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
    // gamificationManager: ver el campo @Inject de arriba.
    val backupSyncManager by lazy { 
        BackupSyncManager(
            this, 
            appwriteService, 
            usageStatsRepository, 
            userRepository, 
            quotesRepository,
            // Las copias no incluían las metas porque nadie se las pasaba.
            goalsRepository
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

    // smartNotificationManager: ver el campo @Inject de arriba.

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

    // NOTA: MotivationalNotificationManager NO se instancia aquí.
    // Lo provee Hilt como @Singleton (AppModule.provideMotivationalNotificationManager)
    // y así lo reciben el receptor de alarmas y el worker. La instancia manual
    // que había aquí no la usaba nadie y duplicaba el canal de notificación y
    // un CoroutineScope.

    private object PreferencesKeys {
        val QUOTES_SEEDED = booleanPreferencesKey("quotes_seeded")
        val MOTIVATIONAL_MESSAGES_SEEDED = booleanPreferencesKey("motivational_messages_seeded")
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
     * Initialize motivational messages seed data and (re)schedule notifications.
     *
     * BUG CORREGIDO: antes la programación estaba protegida por el flag
     * `MOTIVATIONAL_NOTIFICATIONS_SCHEDULED`, que se guardaba tras el primer
     * arranque. Si el trabajo programado se perdía —cancelado, datos
     * borrados, cierre forzado en algunos fabricantes— nunca se volvía a
     * programar y el usuario dejaba de recibir mensajes de forma permanente,
     * sin manera de recuperarlo salvo reinstalar.
     *
     * Ahora se reprograma en **cada** arranque. Las alarmas son idempotentes
     * (mismo request code, FLAG_UPDATE_CURRENT), así que repetir la llamada no
     * duplica nada y sí repara cualquier cadena perdida.
     */
    private fun initializeMotivationalMessagesSystem() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Seed motivational messages if needed (también crea la fila
                // singleton de preferencias).
                motivationalMessagesRepository.initializeSeedDataIfNeeded()
                android.util.Log.d("MomentumApplication", "Motivational messages initialized")

                // Reprogramar siempre, con las preferencias reales del usuario.
                motivationalMessagesRepository.applyScheduleChanges()

                // Red de seguridad: un trabajo periódico revisa que las
                // alarmas sigan en pie aunque el usuario no abra la app.
                MotivationalNotificationWorker.scheduleWatchdog(this@MomentumApplication)

                android.util.Log.d("MomentumApplication", "Motivational notifications scheduled")
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
package com.momentum.app

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.momentum.app.data.AppDatabase
import com.momentum.app.data.repository.UserRepository
import com.momentum.app.data.repository.UsageStatsRepository
import com.momentum.app.data.repository.QuotesRepository
import com.momentum.app.data.appwrite.AppwriteService
import com.momentum.app.data.appwrite.repository.AppwriteUserRepository
import com.momentum.app.data.appwrite.repository.AppwriteQuotesRepository
import com.momentum.app.data.repository.SubscriptionRepository
import com.momentum.app.data.manager.ThemeManager
import com.momentum.app.data.manager.BillingManager
import com.momentum.app.data.manager.NotificationManager
import com.momentum.app.data.manager.ExportManager
import com.momentum.app.data.manager.BackupSyncManager
import com.momentum.app.minimal.MinimalPhoneManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class MomentumApplication : Application(), Configuration.Provider {
    
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
    
    // Subscription management
    val subscriptionRepository by lazy { SubscriptionRepository(appwriteService) }
    
    // Enhanced managers
    val themeManager by lazy { ThemeManager(this) }
    val billingManager by lazy { BillingManager(this) }
    val notificationManager by lazy { NotificationManager(this) }
    val exportManager by lazy { ExportManager(this) }
    val backupSyncManager by lazy { 
        BackupSyncManager(
            this, 
            appwriteService, 
            usageStatsRepository, 
            userRepository, 
            quotesRepository
        ) 
    }

    
    private object PreferencesKeys {
        val QUOTES_SEEDED = booleanPreferencesKey("quotes_seeded")
    }
    
    private fun seedDefaultQuotesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
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

    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Start widget update worker
        com.momentum.app.worker.WidgetUpdateWorker.startPeriodicUpdate(this)
        
        // Initialize enhanced features
        billingManager.startConnection()
        notificationManager.scheduleDailyGoalReminder()
        
        // Initialize Appwrite quotes if needed
        seedDefaultQuotesIfNeeded()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
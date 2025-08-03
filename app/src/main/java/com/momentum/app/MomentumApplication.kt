package com.momentum.app

import android.app.Application
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
import com.momentum.app.minimal.MinimalPhoneManager

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

    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Start widget update worker
        com.momentum.app.service.WidgetUpdateWorker.startPeriodicUpdate(this)
        
        // Initialize Appwrite quotes if needed
        // TODO: Seed default quotes on first run
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
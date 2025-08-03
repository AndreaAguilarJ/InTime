package com.momentum.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.momentum.app.data.AppDatabase
import com.momentum.app.data.repository.UserRepository
import com.momentum.app.data.repository.UsageStatsRepository
import com.momentum.app.data.repository.QuotesRepository

class MomentumApplication : Application(), Configuration.Provider {
    
    // Database instance
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Repositories
    val userRepository by lazy { UserRepository(database.userDao()) }
    val usageStatsRepository by lazy { UsageStatsRepository(this) }
    val quotesRepository by lazy { QuotesRepository(database.quoteDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
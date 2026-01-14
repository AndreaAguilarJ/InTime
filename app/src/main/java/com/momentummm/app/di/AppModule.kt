package com.momentummm.app.di

import android.content.Context
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.manager.AutoSyncManager
import com.momentummm.app.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppwriteService(@ApplicationContext context: Context): AppwriteService {
        return AppwriteService(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(database: AppDatabase): UserRepository {
        return UserRepository(database.userDao())
    }

    @Provides
    @Singleton
    fun provideUsageStatsRepository(@ApplicationContext context: Context): UsageStatsRepository {
        return UsageStatsRepository(context)
    }

    @Provides
    @Singleton
    fun provideQuotesRepository(database: AppDatabase): QuotesRepository {
        return QuotesRepository(database.quoteDao())
    }

    @Provides
    @Singleton
    fun provideGoalsRepository(database: AppDatabase): GoalsRepository {
        return GoalsRepository(database.goalDao(), database.challengeDao())
    }

    @Provides
    @Singleton
    fun provideAppWhitelistRepository(
        database: AppDatabase,
        @ApplicationContext context: Context
    ): AppWhitelistRepository {
        return AppWhitelistRepository(database.appWhitelistDao(), context)
    }

    @Provides
    @Singleton
    fun provideAppLimitRepository(
        database: AppDatabase,
        @ApplicationContext context: Context,
        usageStatsRepository: UsageStatsRepository,
        appWhitelistRepository: AppWhitelistRepository
    ): AppLimitRepository {
        return AppLimitRepository(
            database.appLimitDao(),
            context,
            usageStatsRepository,
            appWhitelistRepository
        )
    }

    @Provides
    @Singleton
    fun provideWebsiteBlockRepository(
        database: AppDatabase,
        @ApplicationContext context: Context
    ): WebsiteBlockRepository {
        return WebsiteBlockRepository(
            database.websiteBlockDao(),
            context
        )
    }

    @Provides
    @Singleton
    fun provideAutoSyncManager(
        @ApplicationContext context: Context,
        appwriteService: AppwriteService,
        userRepository: UserRepository,
        usageStatsRepository: UsageStatsRepository,
        goalsRepository: GoalsRepository,
        appLimitRepository: AppLimitRepository,
        appWhitelistRepository: AppWhitelistRepository,
        quotesRepository: QuotesRepository
    ): AutoSyncManager {
        return AutoSyncManager(
            context,
            appwriteService,
            userRepository,
            usageStatsRepository,
            goalsRepository,
            appLimitRepository,
            appWhitelistRepository,
            quotesRepository
        )
    }
}

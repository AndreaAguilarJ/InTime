package com.momentummm.app.di

import android.content.Context
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.dao.AppLimitDao
import com.momentummm.app.data.dao.AppUsageDao
import com.momentummm.app.data.dao.AppWhitelistDao
import com.momentummm.app.data.dao.ChallengeDao
import com.momentummm.app.data.dao.GoalDao
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.dao.PasswordProtectionDao
import com.momentummm.app.data.dao.QuoteDao
import com.momentummm.app.data.dao.UserDao
import com.momentummm.app.data.dao.WebsiteBlockDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee la base de datos y todos los DAOs necesarios
 * para la aplicación Momentum.
 * 
 * Este módulo es crítico para la inyección de dependencias y debe incluir
 * TODOS los DAOs utilizados en la aplicación.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ========================================================================
    // DATABASE
    // ========================================================================
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    // ========================================================================
    // USER & CORE DAOs
    // ========================================================================

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideQuoteDao(db: AppDatabase): QuoteDao = db.quoteDao()

    @Provides
    @Singleton
    fun provideAppUsageDao(db: AppDatabase): AppUsageDao = db.appUsageDao()

    // ========================================================================
    // APP LIMITS & BLOCKING DAOs
    // ========================================================================

    @Provides
    @Singleton
    fun provideAppLimitDao(db: AppDatabase): AppLimitDao = db.appLimitDao()

    @Provides
    @Singleton
    fun provideAppWhitelistDao(db: AppDatabase): AppWhitelistDao = db.appWhitelistDao()

    @Provides
    @Singleton
    fun provideInAppBlockRuleDao(db: AppDatabase): InAppBlockRuleDao = db.inAppBlockRuleDao()

    @Provides
    @Singleton
    fun provideWebsiteBlockDao(db: AppDatabase): WebsiteBlockDao = db.websiteBlockDao()

    // ========================================================================
    // GOALS & CHALLENGES DAOs
    // ========================================================================

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    @Singleton
    fun provideChallengeDao(db: AppDatabase): ChallengeDao = db.challengeDao()

    // ========================================================================
    // SECURITY DAOs
    // ========================================================================

    @Provides
    @Singleton
    fun providePasswordProtectionDao(db: AppDatabase): PasswordProtectionDao = db.passwordProtectionDao()
}

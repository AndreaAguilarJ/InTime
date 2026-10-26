package com.momentummm.app.di

import android.content.Context
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.dao.AppLimitDao
import com.momentummm.app.data.dao.AppWhitelistDao
import com.momentummm.app.data.dao.GoalDao
import com.momentummm.app.data.dao.ChallengeDao
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.dao.PasswordProtectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideAppLimitDao(db: AppDatabase): AppLimitDao = db.appLimitDao()

    @Provides
    @Singleton
    fun provideAppWhitelistDao(db: AppDatabase): AppWhitelistDao = db.appWhitelistDao()

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    @Singleton
    fun provideChallengeDao(db: AppDatabase): ChallengeDao = db.challengeDao()

    @Provides
    @Singleton
    fun provideInAppBlockRuleDao(db: AppDatabase): InAppBlockRuleDao = db.inAppBlockRuleDao()

    @Provides
    @Singleton
    fun providePasswordProtectionDao(db: AppDatabase): PasswordProtectionDao = db.passwordProtectionDao()
}

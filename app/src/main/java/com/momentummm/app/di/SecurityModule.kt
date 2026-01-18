package com.momentummm.app.di

import android.content.Context
import com.momentummm.app.data.repository.PasswordProtectionRepository
import com.momentummm.app.security.AppLockManager
import com.momentummm.app.security.BiometricPromptManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideBiometricPromptManager(
        @ApplicationContext context: Context
    ): BiometricPromptManager {
        return BiometricPromptManager(context)
    }

    @Provides
    @Singleton
    fun provideAppLockManager(
        passwordProtectionRepository: PasswordProtectionRepository
    ): AppLockManager {
        return AppLockManager(passwordProtectionRepository)
    }
}

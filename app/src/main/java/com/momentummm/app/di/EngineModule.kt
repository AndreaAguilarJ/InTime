package com.momentummm.app.di

import android.content.Context
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.engine.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ENGINE MODULE - Proveedores para los motores de bloqueo inteligente V2
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Este módulo provee los DAOs y engines del sistema de bloqueo avanzado:
 * - UsagePatternEngine: ML-like pattern analysis
 * - AdvancedDetectionEngine: Multi-signal content fingerprinting
 * - AdaptiveBlockingManager: Focus profiles + gradual blocking
 * - UsageAnalyticsEngine: Deep analytics + weekly reports
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    // ========================================================================
    // ENGINE DAOs
    // ========================================================================

    @Provides
    @Singleton
    fun provideUsagePatternDao(db: AppDatabase): UsagePatternDao = db.usagePatternDao()

    @Provides
    @Singleton
    fun provideFocusProfileDao(db: AppDatabase): FocusProfileDao = db.focusProfileDao()

    @Provides
    @Singleton
    fun provideBlockingEventDao(db: AppDatabase): BlockingEventDao = db.blockingEventDao()

    // ========================================================================
    // ENGINES
    // ========================================================================

    @Provides
    @Singleton
    fun provideUsagePatternEngine(
        @ApplicationContext context: Context,
        usagePatternDao: UsagePatternDao
    ): UsagePatternEngine {
        return UsagePatternEngine(context, usagePatternDao)
    }

    @Provides
    @Singleton
    fun provideAdvancedDetectionEngine(): AdvancedDetectionEngine {
        return AdvancedDetectionEngine()
    }

    @Provides
    @Singleton
    fun provideAdaptiveBlockingManager(
        @ApplicationContext context: Context,
        patternEngine: UsagePatternEngine,
        detectionEngine: AdvancedDetectionEngine,
        focusProfileDao: FocusProfileDao,
        blockingEventDao: BlockingEventDao,
        usagePatternDao: UsagePatternDao
    ): AdaptiveBlockingManager {
        return AdaptiveBlockingManager(context, patternEngine, detectionEngine, focusProfileDao, blockingEventDao, usagePatternDao)
    }

    @Provides
    @Singleton
    fun provideUsageAnalyticsEngine(
        @ApplicationContext context: Context,
        patternEngine: UsagePatternEngine,
        usagePatternDao: UsagePatternDao,
        blockingEventDao: BlockingEventDao
    ): UsageAnalyticsEngine {
        return UsageAnalyticsEngine(context, patternEngine, usagePatternDao, blockingEventDao)
    }
}

package com.momentummm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momentummm.app.data.dao.AppUsageDao
import com.momentummm.app.data.dao.QuoteDao
import com.momentummm.app.data.dao.UserDao
import com.momentummm.app.data.dao.AppLimitDao
import com.momentummm.app.data.dao.AppWhitelistDao
import com.momentummm.app.data.dao.GoalDao
import com.momentummm.app.data.dao.ChallengeDao
import com.momentummm.app.data.dao.WebsiteBlockDao
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.dao.PasswordProtectionDao
import com.momentummm.app.data.entity.AppUsage
import com.momentummm.app.data.entity.Quote
import com.momentummm.app.data.entity.UserSettings
import com.momentummm.app.data.entity.AppLimit
import com.momentummm.app.data.entity.AppWhitelist
import com.momentummm.app.data.entity.Goal
import com.momentummm.app.data.entity.GoalProgress
import com.momentummm.app.data.entity.Challenge
import com.momentummm.app.data.entity.WebsiteBlock
import com.momentummm.app.data.entity.InAppBlockRule
import com.momentummm.app.data.entity.PasswordProtection
import com.momentummm.app.data.entity.SmartBlockingConfig
import com.momentummm.app.data.entity.ContextBlockRule
import com.momentummm.app.data.entity.Friend
import com.momentummm.app.data.entity.LeaderboardEntry
import com.momentummm.app.data.entity.SharedAchievement
import com.momentummm.app.data.entity.CommunitySettings
import com.momentummm.app.data.dao.SmartBlockingConfigDao
import com.momentummm.app.data.dao.ContextBlockRuleDao
import com.momentummm.app.data.dao.FriendDao
import com.momentummm.app.data.dao.LeaderboardDao
import com.momentummm.app.data.dao.SharedAchievementDao
import com.momentummm.app.data.dao.CommunitySettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [UserSettings::class, Quote::class, AppUsage::class, AppLimit::class, AppWhitelist::class, Goal::class, GoalProgress::class, Challenge::class, WebsiteBlock::class, InAppBlockRule::class, PasswordProtection::class, SmartBlockingConfig::class, ContextBlockRule::class, Friend::class, LeaderboardEntry::class, SharedAchievement::class, CommunitySettings::class],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun quoteDao(): QuoteDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun appWhitelistDao(): AppWhitelistDao
    abstract fun goalDao(): GoalDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun websiteBlockDao(): WebsiteBlockDao
    abstract fun inAppBlockRuleDao(): InAppBlockRuleDao
    abstract fun passwordProtectionDao(): PasswordProtectionDao
    abstract fun smartBlockingConfigDao(): SmartBlockingConfigDao
    abstract fun contextBlockRuleDao(): ContextBlockRuleDao
    abstract fun friendDao(): FriendDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun sharedAchievementDao(): SharedAchievementDao
    abstract fun communitySettingsDao(): CommunitySettingsDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.quoteDao())
                }
            }
        }

        suspend fun populateDatabase(quoteDao: QuoteDao) {
            // Insert sample motivational quotes
            val quotes = getMotivationalQuotes()
            quoteDao.insertQuotes(quotes)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `in_app_block_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ruleId` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `blockType` TEXT NOT NULL,
                        `featureName` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `detectionPatterns` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // MIGRATION 9 -> 10: Agregar campos de gamificación a user_settings
        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar campos de gamificación a la tabla user_settings
                db.execSQL("ALTER TABLE user_settings ADD COLUMN userLevel INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN currentXp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN totalXp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN timeCoins INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN longestStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN lastActiveDate INTEGER")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN totalFocusMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN totalSessionsCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN perfectDaysCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // MIGRATION 10 -> 11: Agregar campos de configuración de gamificación
        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar campos de configuración de gamificación
                db.execSQL("ALTER TABLE user_settings ADD COLUMN gamificationEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN showXpNotifications INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN showStreakReminders INTEGER NOT NULL DEFAULT 1")
            }
        }

        // MIGRATION 11 -> 12: (ya existente, mantener vacío o agregar lógica si es necesario)
        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migraciones previas
            }
        }

        // MIGRATION 12 -> 13: Agregar tablas de Smart Blocking
        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabla SmartBlockingConfig
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `smart_blocking_config` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `sleepModeEnabled` INTEGER NOT NULL DEFAULT 0,
                        `sleepStartHour` INTEGER NOT NULL DEFAULT 23,
                        `sleepStartMinute` INTEGER NOT NULL DEFAULT 0,
                        `sleepEndHour` INTEGER NOT NULL DEFAULT 7,
                        `sleepEndMinute` INTEGER NOT NULL DEFAULT 0,
                        `sleepModeIgnoreTracking` INTEGER NOT NULL DEFAULT 1,
                        `digitalFastingEnabled` INTEGER NOT NULL DEFAULT 0,
                        `fastingStartHour` INTEGER NOT NULL DEFAULT 9,
                        `fastingStartMinute` INTEGER NOT NULL DEFAULT 0,
                        `fastingEndHour` INTEGER NOT NULL DEFAULT 18,
                        `fastingEndMinute` INTEGER NOT NULL DEFAULT 0,
                        `fastingDailyLimitMinutes` INTEGER NOT NULL DEFAULT 30,
                        `fastingApplyToAllApps` INTEGER NOT NULL DEFAULT 1,
                        `fastingDaysOfWeek` TEXT NOT NULL DEFAULT '1,2,3,4,5',
                        `nuclearModeEnabled` INTEGER NOT NULL DEFAULT 0,
                        `nuclearModeStartDate` INTEGER,
                        `nuclearModeEndDate` INTEGER,
                        `nuclearModeDurationDays` INTEGER NOT NULL DEFAULT 30,
                        `nuclearModeApps` TEXT NOT NULL DEFAULT '',
                        `nuclearModeRequiresAppOpen` INTEGER NOT NULL DEFAULT 1,
                        `nuclearModeUnlockWaitMinutes` INTEGER NOT NULL DEFAULT 30,
                        `nuclearModeCurrentWaitSeconds` INTEGER NOT NULL DEFAULT 0,
                        `contextBlockingEnabled` INTEGER NOT NULL DEFAULT 0,
                        `streakProtectionEnabled` INTEGER NOT NULL DEFAULT 1,
                        `graceDaysPerWeek` INTEGER NOT NULL DEFAULT 1,
                        `graceDaysUsedThisWeek` INTEGER NOT NULL DEFAULT 0,
                        `lastGraceDayResetDate` INTEGER,
                        `warningBeforeStreakBreak` INTEGER NOT NULL DEFAULT 1,
                        `warningMinutesBeforeLimit` INTEGER NOT NULL DEFAULT 5,
                        `floatingTimerEnabled` INTEGER NOT NULL DEFAULT 0,
                        `floatingTimerOpacity` REAL NOT NULL DEFAULT 0.8,
                        `floatingTimerPosition` TEXT NOT NULL DEFAULT 'TOP_RIGHT',
                        `floatingTimerSize` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `floatingTimerShowForApps` TEXT NOT NULL DEFAULT '',
                        `communicationOnlyModeEnabled` INTEGER NOT NULL DEFAULT 0,
                        `communicationOnlyApps` TEXT NOT NULL DEFAULT '',
                        `communicationOnlyAllowDMs` INTEGER NOT NULL DEFAULT 1,
                        `communicationOnlyBlockFeed` INTEGER NOT NULL DEFAULT 1,
                        `communicationOnlyBlockStories` INTEGER NOT NULL DEFAULT 1,
                        `communicationOnlyBlockReels` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Tabla ContextBlockRule
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `context_block_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ruleName` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `contextType` TEXT NOT NULL DEFAULT 'SCHEDULE',
                        `latitude` REAL,
                        `longitude` REAL,
                        `radiusMeters` INTEGER NOT NULL DEFAULT 100,
                        `locationName` TEXT,
                        `wifiSsid` TEXT,
                        `scheduleStartHour` INTEGER NOT NULL DEFAULT 9,
                        `scheduleStartMinute` INTEGER NOT NULL DEFAULT 0,
                        `scheduleEndHour` INTEGER NOT NULL DEFAULT 18,
                        `scheduleEndMinute` INTEGER NOT NULL DEFAULT 0,
                        `scheduleDaysOfWeek` TEXT NOT NULL DEFAULT '1,2,3,4,5',
                        `affectedApps` TEXT NOT NULL DEFAULT '',
                        `applyToAllLimitedApps` INTEGER NOT NULL DEFAULT 1,
                        `overrideDailyLimit` INTEGER NOT NULL DEFAULT 1,
                        `contextDailyLimitMinutes` INTEGER NOT NULL DEFAULT 15,
                        `blockCompletely` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        // MIGRATION 13 -> 14: Add Community tables
        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabla Friends
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `friends` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `friendUserId` TEXT NOT NULL,
                        `friendName` TEXT NOT NULL,
                        `friendEmail` TEXT,
                        `avatarUrl` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `sentByMe` INTEGER NOT NULL DEFAULT 1,
                        `friendLevel` INTEGER NOT NULL DEFAULT 1,
                        `friendStreak` INTEGER NOT NULL DEFAULT 0,
                        `friendTotalFocusMinutes` INTEGER NOT NULL DEFAULT 0,
                        `friendWeeklyFocusMinutes` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        `lastSyncedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_friends_friendUserId` ON `friends` (`friendUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_friends_status` ON `friends` (`status`)")
                
                // Tabla LeaderboardEntry
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `leaderboard_entries` (
                        `odId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `userName` TEXT NOT NULL,
                        `avatarUrl` TEXT,
                        `weeklyFocusMinutes` INTEGER NOT NULL DEFAULT 0,
                        `weeklyPerfectDays` INTEGER NOT NULL DEFAULT 0,
                        `currentStreak` INTEGER NOT NULL DEFAULT 0,
                        `userLevel` INTEGER NOT NULL DEFAULT 1,
                        `rank` INTEGER NOT NULL DEFAULT 0,
                        `previousRank` INTEGER NOT NULL DEFAULT 0,
                        `weekStartDate` INTEGER NOT NULL,
                        `isFriend` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_leaderboard_entries_weekStartDate` ON `leaderboard_entries` (`weekStartDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_leaderboard_entries_rank` ON `leaderboard_entries` (`rank`)")
                
                // Tabla SharedAchievements
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shared_achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `achievementType` TEXT NOT NULL,
                        `achievementValue` INTEGER NOT NULL DEFAULT 0,
                        `message` TEXT NOT NULL DEFAULT '',
                        `isShared` INTEGER NOT NULL DEFAULT 0,
                        `shareCount` INTEGER NOT NULL DEFAULT 0,
                        `achievedAt` INTEGER NOT NULL DEFAULT 0,
                        `remoteId` TEXT
                    )
                """.trimIndent())
                
                // Tabla CommunitySettings
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `community_settings` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `profileVisibility` TEXT NOT NULL DEFAULT 'FRIENDS_ONLY',
                        `showInGlobalLeaderboard` INTEGER NOT NULL DEFAULT 1,
                        `showStreakToFriends` INTEGER NOT NULL DEFAULT 1,
                        `showLevelToFriends` INTEGER NOT NULL DEFAULT 1,
                        `showFocusTimeToFriends` INTEGER NOT NULL DEFAULT 1,
                        `notifyFriendRequests` INTEGER NOT NULL DEFAULT 1,
                        `notifyFriendAchievements` INTEGER NOT NULL DEFAULT 1,
                        `notifyLeaderboardChanges` INTEGER NOT NULL DEFAULT 1,
                        `shameEnabled` INTEGER NOT NULL DEFAULT 1,
                        `gloryEnabled` INTEGER NOT NULL DEFAULT 1,
                        `autoShareAchievements` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "momentum_database"
                ).addCallback(AppDatabaseCallback(CoroutineScope(Dispatchers.IO + SupervisorJob())))
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration() // For now, allow destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private fun getMotivationalQuotes(): List<Quote> {
    return listOf(
        Quote(text = "El tiempo es el recurso más valioso que tenemos. Úsalo sabiamente.", author = "Anónimo"),
        Quote(text = "No esperes el momento perfecto. Toma el momento y hazlo perfecto.", author = "Anónimo"),
        Quote(text = "Tu vida es tu mensaje al mundo. Asegúrate de que sea inspirador.", author = "Anónimo"),
        Quote(text = "El tiempo que disfrutas perdiendo no es tiempo perdido.", author = "John Lennon"),
        Quote(text = "La procrastinación es el ladrón del tiempo.", author = "Edward Young"),
        Quote(text = "El futuro depende de lo que hagas hoy.", author = "Mahatma Gandhi"),
        Quote(text = "No puedes controlar el tiempo, pero puedes controlar cómo lo usas.", author = "Anónimo"),
        Quote(text = "Cada minuto que pasas enfadado es un minuto de felicidad que pierdes.", author = "Anónimo"),
        Quote(text = "El tiempo vuela, pero tú eres el piloto.", author = "Anónimo"),
        Quote(text = "La vida es lo que pasa mientras estás ocupado haciendo otros planes.", author = "John Lennon"),
        Quote(text = "Ayer es historia, mañana es un misterio, hoy es un regalo.", author = "Eleanor Roosevelt"),
        Quote(text = "No dejes para mañana lo que puedas hacer hoy.", author = "Benjamin Franklin"),
        Quote(text = "El tiempo es lo más valioso que podemos gastar.", author = "Theophrastus"),
        Quote(text = "La puntualidad es el respeto por el tiempo de los demás.", author = "Anónimo"),
        Quote(text = "Cada momento es un nuevo comienzo.", author = "T.S. Eliot"),
        Quote(text = "El tiempo perdido nunca se recupera.", author = "Benjamin Franklin"),
        Quote(text = "Vivir el presente es un regalo.", author = "Anónimo"),
        Quote(text = "El éxito es la suma de pequeños esfuerzos repetidos día tras día.", author = "Robert Collier"),
        Quote(text = "No tengas miedo de renunciar a lo bueno para ir por lo grandioso.", author = "John D. Rockefeller"),
        Quote(text = "La disciplina es elegir entre lo que quieres ahora y lo que más quieres.", author = "Anónimo"),
        Quote(text = "Un objetivo sin un plan es solo un deseo.", author = "Antoine de Saint-Exupéry"),
        Quote(text = "La motivación te pone en marcha, pero el hábito es lo que te mantiene en movimiento.", author = "Jim Ryun"),
        Quote(text = "No se trata de tener tiempo, se trata de hacer tiempo.", author = "Anónimo"),
        Quote(text = "Cada día es una nueva oportunidad para cambiar tu vida.", author = "Anónimo"),
        Quote(text = "El tiempo es dinero, pero el dinero no puede comprar tiempo.", author = "Anónimo"),
        Quote(text = "Haz que cada día cuente.", author = "Anónimo"),
        Quote(text = "La vida es corta, pero si la vives bien, es suficiente.", author = "Anónimo"),
        Quote(text = "No cuentes los días, haz que los días cuenten.", author = "Muhammad Ali"),
        Quote(text = "El mejor momento para plantar un árbol fue hace 20 años. El segundo mejor momento es ahora.", author = "Proverbio chino"),
        Quote(text = "Tu tiempo es limitado, no lo desperdicies viviendo la vida de otra persona.", author = "Steve Jobs"),
        Quote(text = "El secreto para salir adelante es comenzar.", author = "Mark Twain"),
        Quote(text = "La perfección no se alcanza cuando no hay nada más que agregar, sino cuando no hay nada más que quitar.", author = "Antoine de Saint-Exupéry"),
        Quote(text = "Si quieres cambiar el mundo, comienza por cambiar tú mismo.", author = "Mahatma Gandhi"),
        Quote(text = "La felicidad no es algo hecho. Viene de tus propias acciones.", author = "Dalai Lama"),
        Quote(text = "El único modo de hacer un trabajo excelente es amar lo que haces.", author = "Steve Jobs"),
        Quote(text = "No juzgues cada día por la cosecha que recoges, sino por las semillas que plantas.", author = "Robert Louis Stevenson"),
        Quote(text = "La vida es 10% lo que te sucede y 90% cómo reaccionas a ello.", author = "Charles R. Swindoll"),
        Quote(text = "El éxito no es final, el fracaso no es fatal: es el coraje para continuar lo que cuenta.", author = "Winston Churchill"),
        Quote(text = "Haz algo hoy que tu yo del futuro te agradecerá.", author = "Sean Patrick Flanery"),
        Quote(text = "La única forma de hacer un gran trabajo es amar lo que haces.", author = "Steve Jobs"),
        Quote(text = "No esperes oportunidades extraordinarias. Aprovecha las ocasiones comunes y hazlas extraordinarias.", author = "Orison Swett Marden"),
        Quote(text = "El presente es un regalo. Por eso se llama presente.", author = "Bil Keane"),
        Quote(text = "Trabaja mientras ellos duermen. Aprende mientras ellos festejan. Ahorra mientras ellos gastan. Entonces podrás vivir como ellos sueñan.", author = "Anónimo"),
        Quote(text = "La diferencia entre lo ordinario y lo extraordinario es ese pequeño 'extra'.", author = "Jimmy Johnson"),
        Quote(text = "No hay atajos para ningún lugar que valga la pena ir.", author = "Beverly Sills"),
        Quote(text = "El éxito es caminar de fracaso en fracaso sin perder el entusiasmo.", author = "Winston Churchill"),
        Quote(text = "Tu única limitación eres tú mismo.", author = "Anónimo"),
        Quote(text = "Cree en ti mismo y todo será posible.", author = "Anónimo"),
        Quote(text = "Los sueños se convierten en realidad cuando los deseos se convierten en acciones.", author = "Anónimo"),
        Quote(text = "El camino hacia el éxito está siempre en construcción.", author = "Lily Tomlin"),
        Quote(text = "No esperes que las oportunidades lleguen, créalas.", author = "Anónimo")
    )
}
package com.momentum.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momentum.app.data.dao.AppUsageDao
import com.momentum.app.data.dao.QuoteDao
import com.momentum.app.data.dao.UserDao
import com.momentum.app.data.entity.AppUsage
import com.momentum.app.data.entity.Quote
import com.momentum.app.data.entity.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [UserSettings::class, Quote::class, AppUsage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun quoteDao(): QuoteDao
    abstract fun appUsageDao(): AppUsageDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "momentum_database"
                ).addCallback(AppDatabaseCallback(kotlinx.coroutines.GlobalScope))
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
        Quote(text = "La vida es lo que pasa mientras estás ocupado haciendo otros planes.", author = "John Lennon")
    )
}
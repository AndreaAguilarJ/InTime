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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [UserSettings::class, Quote::class, AppUsage::class, AppLimit::class, AppWhitelist::class, Goal::class, GoalProgress::class, Challenge::class, WebsiteBlock::class, InAppBlockRule::class, PasswordProtection::class],
    version = 8,
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
                ).addCallback(AppDatabaseCallback(CoroutineScope(Dispatchers.IO + SupervisorJob())))
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
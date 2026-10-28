package com.momentummm.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.entity.MotivationalPreferences
import com.momentummm.app.notification.MessageRelevance
import com.momentummm.app.notification.NotificationRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Widget «Mensaje motivacional»: un mensaje de Momentum para todo el día.
 *
 * ─── EL FALLO QUE CORRIGE ───────────────────────────────────────────────
 * El mensaje se elegía entre TODAS las categorías y se quedaba el día entero en
 * la pantalla de inicio: podía salir «Buenos días» a las 23:00, «¡Nuevo lunes!»
 * todo el viernes o un «¡Logro!» sin logro, igual que pasaba en las
 * notificaciones. Ahora solo entran categorías que valen a cualquier hora (y la
 * de fin de semana cuando lo es).
 */
class MotivationalMessageWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_error) {

    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        private const val TAG = "MotivationalWidget"
        private const val LOAD_TIMEOUT_MS = 3_000L
        private const val MESSAGE_SALT = 5
        private const val MAX_PICKS = 5

        // Caché del mensaje del día: el mismo en cada refresco hasta medianoche.
        private const val PREFS_NAME = "motivational_widget_prefs"
        private const val KEY_MESSAGE_ID = "message_of_day_id"
        private const val KEY_MESSAGE_DATE = "message_date"

        /** Categorías que tienen sentido durante un día entero. */
        fun categoriesFor(date: LocalDate): List<MessageCategory> {
            val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            return MessageRelevance.UNIVERSAL + if (weekend) listOf(MessageCategory.WEEKEND) else emptyList()
        }

        /** Categorías y tonos entre los que se sortea el mensaje del día. */
        data class Selection(val categories: List<MessageCategory>, val tones: List<MessageTone>) {
            fun accepts(category: MessageCategory, tone: MessageTone): Boolean =
                category in categories && tone in tones
        }

        /**
         * Lo que el usuario dejó activado en Mensajes motivacionales, dentro de lo
         * que vale el día entero. Antes el widget sorteaba entre todos los tonos y
         * categorías: quien apagaba el tono humorístico lo seguía teniendo todo el
         * día en la pantalla de inicio.
         *
         * @return null si ninguna categoría activada vale todo el día; el widget
         *   muestra entonces un mensaje neutro en vez de saltarse lo que se apagó.
         */
        fun selectionFor(
            date: LocalDate,
            enabledCategories: List<MessageCategory>,
            enabledTones: List<MessageTone>,
        ): Selection? {
            // Vacío = preferencias ilegibles; el repositorio también lo trata como «todo».
            val enabled = enabledCategories.ifEmpty { MessageCategory.entries }
            val categories = categoriesFor(date).filter { it in enabled }
            if (categories.isEmpty()) return null
            return Selection(categories, enabledTones.ifEmpty { MessageTone.entries })
        }

        suspend fun updateAllWidgets(context: Context) {
            MotivationalMessageWidget().updateAll(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val localized = WidgetLocale.localized(context)
        val today = LocalDate.now()
        val stored = if (WidgetLocale.usesStoredSpanishContent(localized)) {
            storedMessage(context, today)?.content
        } else {
            null
        }
        val body = stored ?: fallbackMessage(localized, today)
        val label = localized.resources.getString(R.string.widget_message_label)

        provideContent {
            WidgetTextCard(
                label = label,
                body = body,
                footer = null,
                onClick = WidgetActions.open(LocalContext.current, NotificationRoutes.MOTIVATIONAL_LIBRARY),
            )
        }
    }

    /** Los mensajes guardados están en español; para otros idiomas, los traducidos. */
    private fun fallbackMessage(localized: Context, today: LocalDate): String {
        val messages = localized.resources.getStringArray(R.array.widget_messages)
        return messages[DailyPick.index(today.toEpochDay(), messages.size, salt = MESSAGE_SALT)]
    }

    private suspend fun storedMessage(context: Context, today: LocalDate): MotivationalMessage? =
        withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            try {
                withContext(Dispatchers.IO) { messageOfTheDay(context, today) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading motivational message for widget", e)
                null
            }
        }

    private suspend fun messageOfTheDay(context: Context, today: LocalDate): MotivationalMessage? {
        val db = AppDatabase.getDatabase(context)
        val userPrefs = db.motivationalPreferencesDao().getPreferencesSync() ?: MotivationalPreferences()
        val selection = selectionFor(
            date = today,
            enabledCategories = userPrefs.getEnabledCategoriesList(),
            enabledTones = userPrefs.getEnabledTonesList(),
        ) ?: return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dao = db.motivationalMessageDao()
        val todayKey = today.toString()

        // El de hoy, si ya se eligió y sigue siendo válido. Puede haber dejado de
        // serlo: la versión anterior admitía otras categorías, el usuario pudo
        // apagar su tono o categoría, o borrar el mensaje.
        if (prefs.getString(KEY_MESSAGE_DATE, null) == todayKey) {
            prefs.getString(KEY_MESSAGE_ID, null)
                ?.let { dao.getMessageById(it) }
                ?.takeIf { selection.accepts(it.category, it.tone) && WidgetMessages.isShowable(it.content) }
                ?.let { return it }
        }

        // Unos pocos intentos: el sorteo puede devolver un mensaje con marcadores
        // o demasiado largo para leerlo de un vistazo. Uno largo solo vale si no
        // sale ninguno corto.
        var longFallback: MotivationalMessage? = null
        repeat(MAX_PICKS) {
            val picked = dao.getSmartRandomMessage(selection.categories, selection.tones) ?: return@repeat
            if (!WidgetMessages.isShowable(picked.content)) return@repeat
            if (WidgetMessages.fitsWidget(picked.content)) return keepForToday(prefs, todayKey, picked)
            if (longFallback == null) longFallback = picked
        }
        return longFallback?.let { keepForToday(prefs, todayKey, it) }
    }

    private fun keepForToday(
        prefs: android.content.SharedPreferences,
        todayKey: String,
        message: MotivationalMessage,
    ): MotivationalMessage {
        prefs.edit()
            .putString(KEY_MESSAGE_DATE, todayKey)
            .putString(KEY_MESSAGE_ID, message.id)
            .apply()
        return message
    }
}

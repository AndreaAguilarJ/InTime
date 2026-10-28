package com.momentummm.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.notification.NotificationRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * Widget «Frase del día»: una frase célebre con su autor, la misma todo el día.
 */
class QuoteWidget : GlanceAppWidget(errorUiLayout = R.layout.widget_error) {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val localized = WidgetLocale.localized(context)
        val quote = quoteOfTheDay(context, localized, LocalDate.now().toEpochDay())
        val res = localized.resources
        val label = res.getString(R.string.widget_quote_label)
        val footer = quote.author?.let { res.getString(R.string.widget_quote_author, it) }

        provideContent {
            WidgetTextCard(
                label = label,
                body = quote.text,
                footer = footer,
                onClick = WidgetActions.open(LocalContext.current, NotificationRoutes.TODAY),
            )
        }
    }

    /**
     * Las frases guardadas están en español. Con la app en español se usan esas
     * (incluidas las que añada el usuario); en otro idioma, las traducidas de
     * `R.array.widget_quotes`, que también cubren una base de datos vacía.
     */
    private suspend fun quoteOfTheDay(context: Context, localized: Context, epochDay: Long): WidgetQuote {
        if (WidgetLocale.usesStoredSpanishContent(localized)) {
            storedQuote(context, epochDay)?.let { return it }
        }
        val entries = localized.resources.getStringArray(R.array.widget_quotes)
            .mapNotNull(WidgetQuotes::parse)
        return entries[DailyPick.index(epochDay, entries.size, salt = QUOTE_SALT)]
    }

    private suspend fun storedQuote(context: Context, epochDay: Long): WidgetQuote? =
        withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            try {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(context).quoteDao()
                    val count = dao.getQuoteCount()
                    if (count == 0) return@withContext null
                    dao.getQuoteAt(DailyPick.index(epochDay, count, salt = QUOTE_SALT))
                        ?.takeIf { it.text.isNotBlank() }
                        ?.let { WidgetQuote(it.text.trim(), it.author?.trim()?.takeIf(String::isNotEmpty)) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading quote for widget", e)
                null
            }
        }

    private companion object {
        const val TAG = "QuoteWidget"
        const val LOAD_TIMEOUT_MS = 3_000L
        const val QUOTE_SALT = 0
    }
}

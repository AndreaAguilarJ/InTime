package com.momentummm.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.momentummm.app.MainActivity
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.entity.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val quote = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context).quoteDao().getRandomQuote()
        } ?: defaultQuote()

        provideContent {
            QuoteWidgetContent(quote)
        }
    }

    @Composable
    private fun QuoteWidgetContent(quote: Quote) {
        val authorText = quote.author?.takeIf { it.isNotBlank() } ?: "Momentum"

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(16.dp)
                .clickable(actionStartActivity(Intent().apply {
                    setClassName("com.momentummm.app", "com.momentummm.app.MainActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💡",
                style = TextStyle(
                    fontSize = 24.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = quote.text,
                style = TextStyle(
                    fontSize = 14.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = "- $authorText",
                style = TextStyle(
                    fontSize = 12.sp
                )
            )
        }
    }

    private fun defaultQuote(): Quote {
        return Quote(
            text = "Tu tiempo es limitado, úsalo sabiamente.",
            author = "Momentum"
        )
    }
}
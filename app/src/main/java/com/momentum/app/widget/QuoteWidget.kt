package com.momentum.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuoteWidgetContent()
        }
    }

    @Composable
    private fun QuoteWidgetContent() {
        val quote = getRandomQuote()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.DarkGray))
                .padding(16.dp),
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
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = "- ${quote.author}",
                style = TextStyle(
                    color = ColorProvider(Color.LightGray),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    private fun getRandomQuote(): Quote {
        // Default quotes until we can load from database
        val quotes = listOf(
            Quote("El tiempo es más valioso que el dinero. Puedes conseguir más dinero, pero no puedes conseguir más tiempo.", "Jim Rohn"),
            Quote("Tu tiempo es limitado, no lo desperdicies viviendo la vida de alguien más.", "Steve Jobs"),
            Quote("No esperes por el momento perfecto, toma el momento y hazlo perfecto.", "Zoey Sayward"),
            Quote("El tiempo que disfrutas desperdiciando no es tiempo desperdiciado.", "Marthe Troly-Curtin"),
            Quote("La gestión del tiempo es una habilidad de vida esencial.", "Dwayne Johnson")
        )
        return quotes.random()
    }

    data class Quote(val text: String, val author: String)
}
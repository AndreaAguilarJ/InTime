package com.momentummm.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.momentummm.app.MainActivity

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidgetProvider()
}

class QuoteWidgetProvider : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuoteWidgetContent()
        }
    }
    
    @Composable
    private fun QuoteWidgetContent() {
        val quote = getDailyQuote()
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💭",
                    style = TextStyle(fontSize = 24.sp)
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = quote.text,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.glance.color.ColorProvider(
                            day = Color.Black,
                            night = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                )
                
                if (quote.author != null) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "- ${quote.author}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = androidx.glance.color.ColorProvider(
                                day = Color.Gray,
                                night = Color.LightGray
                            ),
                            textAlign = TextAlign.Center
                        )
                    )
                }
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Button(
                    text = "Abrir Momentum",
                    onClick = actionStartActivity<MainActivity>()
                )
            }
        }
    }
    
    private fun getDailyQuote(): Quote {
        // Sample quotes - in real app, this would come from database
        val quotes = listOf(
            Quote("El tiempo es el recurso más valioso que tenemos.", "Momentum"),
            Quote("Cada semana cuenta. Cada momento importa.", "Momentum"),
            Quote("No dejes para mañana lo que puedes hacer hoy.", "Benjamin Franklin"),
            Quote("El futuro depende de lo que hagas hoy.", "Mahatma Gandhi"),
            Quote("Vive como si fueras a morir mañana.", "Mahatma Gandhi")
        )
        
        // Get quote based on day of year for consistency
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }
}

data class Quote(
    val text: String,
    val author: String? = null
)
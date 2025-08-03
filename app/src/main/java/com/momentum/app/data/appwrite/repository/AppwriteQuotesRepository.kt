package com.momentum.app.data.appwrite.repository

import com.momentum.app.data.appwrite.AppwriteConfig
import com.momentum.app.data.appwrite.AppwriteService
import com.momentum.app.data.appwrite.models.AppwriteQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppwriteQuotesRepository(private val appwriteService: AppwriteService) {
    
    suspend fun getAllQuotes(): Flow<List<AppwriteQuote>> = flow {
        try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.QUOTES_COLLECTION_ID
            )
            
            val quotes = documents.documents.map { document ->
                Json.decodeFromString<AppwriteQuote>(document.data.toString())
            }
            emit(quotes)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun getRandomQuote(): AppwriteQuote? {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.QUOTES_COLLECTION_ID
            )
            
            if (documents.documents.isNotEmpty()) {
                val randomDocument = documents.documents.random()
                Json.decodeFromString<AppwriteQuote>(randomDocument.data.toString())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createQuote(quote: AppwriteQuote): Result<AppwriteQuote> {
        return try {
            val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
            val quoteWithTimestamp = quote.copy(createdAt = currentTime)
            
            val document = appwriteService.databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.QUOTES_COLLECTION_ID,
                documentId = "unique()",
                data = Json.encodeToString(quoteWithTimestamp)
            )
            
            Result.success(quoteWithTimestamp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun seedQuotes() {
        val defaultQuotes = listOf(
            AppwriteQuote(id = "1", text = "El tiempo es el recurso más valioso que tenemos.", author = "Momentum"),
            AppwriteQuote(id = "2", text = "Cada semana cuenta. Cada momento importa.", author = "Momentum"),
            AppwriteQuote(id = "3", text = "La procrastinación es el ladrón del tiempo.", author = "Edward Young"),
            AppwriteQuote(id = "4", text = "El futuro depende de lo que hagas hoy.", author = "Mahatma Gandhi"),
            AppwriteQuote(id = "5", text = "No dejes para mañana lo que puedes hacer hoy.", author = "Benjamin Franklin"),
            AppwriteQuote(id = "6", text = "El tiempo perdido nunca se recupera.", author = "Benjamin Franklin"),
            AppwriteQuote(id = "7", text = "Vive como si fueras a morir mañana.", author = "Mahatma Gandhi"),
            AppwriteQuote(id = "8", text = "La vida es lo que pasa mientras haces otros planes.", author = "John Lennon"),
            AppwriteQuote(id = "9", text = "El tiempo vuela, pero los recuerdos duran para siempre.", author = "Momentum"),
            AppwriteQuote(id = "10", text = "Haz que cada semana cuente en tu historia de vida.", author = "Momentum")
        )
        
        defaultQuotes.forEach { quote ->
            createQuote(quote)
        }
    }
}
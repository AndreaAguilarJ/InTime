package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.Quote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuote(): Quote?

    @Query("SELECT * FROM quotes")
    fun getAllQuotes(): Flow<List<Quote>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuotes(quotes: List<Quote>)

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun getQuoteCount(): Int
}
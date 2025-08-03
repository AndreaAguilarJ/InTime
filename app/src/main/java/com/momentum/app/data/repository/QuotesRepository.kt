package com.momentum.app.data.repository

import com.momentum.app.data.dao.QuoteDao
import com.momentum.app.data.entity.Quote
import kotlinx.coroutines.flow.Flow

class QuotesRepository(private val quoteDao: QuoteDao) {

    suspend fun getRandomQuote(): Quote? {
        return quoteDao.getRandomQuote()
    }

    fun getAllQuotes(): Flow<List<Quote>> {
        return quoteDao.getAllQuotes()
    }

    suspend fun getQuoteCount(): Int {
        return quoteDao.getQuoteCount()
    }

    suspend fun insertQuotes(quotes: List<Quote>) {
        quoteDao.insertQuotes(quotes)
    }
}
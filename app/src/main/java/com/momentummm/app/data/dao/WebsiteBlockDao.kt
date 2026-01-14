package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.WebsiteBlock
import com.momentummm.app.data.entity.WebsiteCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteBlockDao {
    @Query("SELECT * FROM website_blocks ORDER BY displayName ASC")
    fun getAllBlocks(): Flow<List<WebsiteBlock>>

    @Query("SELECT * FROM website_blocks WHERE isEnabled = 1")
    fun getAllEnabledBlocks(): Flow<List<WebsiteBlock>>

    @Query("SELECT * FROM website_blocks WHERE category = :category")
    fun getBlocksByCategory(category: WebsiteCategory): Flow<List<WebsiteBlock>>

    @Query("SELECT * FROM website_blocks WHERE url = :url LIMIT 1")
    suspend fun getBlockByUrl(url: String): WebsiteBlock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: WebsiteBlock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<WebsiteBlock>)

    @Update
    suspend fun updateBlock(block: WebsiteBlock)

    @Query("UPDATE website_blocks SET isEnabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateBlockEnabled(id: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteBlock(block: WebsiteBlock)

    @Query("DELETE FROM website_blocks WHERE id = :id")
    suspend fun deleteBlockById(id: Long)

    @Query("DELETE FROM website_blocks WHERE category = :category")
    suspend fun deleteBlocksByCategory(category: WebsiteCategory)

    @Query("SELECT COUNT(*) FROM website_blocks WHERE isEnabled = 1")
    suspend fun getEnabledBlocksCount(): Int

    @Query("SELECT COUNT(*) FROM website_blocks WHERE category = :category AND isEnabled = 1")
    suspend fun getEnabledBlocksCountByCategory(category: WebsiteCategory): Int
}


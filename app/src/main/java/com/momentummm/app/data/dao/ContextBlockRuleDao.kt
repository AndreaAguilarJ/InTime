package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.ContextBlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextBlockRuleDao {
    
    @Query("SELECT * FROM context_block_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<ContextBlockRule>>
    
    @Query("SELECT * FROM context_block_rules WHERE isEnabled = 1")
    fun getEnabledRules(): Flow<List<ContextBlockRule>>
    
    @Query("SELECT * FROM context_block_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesSync(): List<ContextBlockRule>
    
    @Query("SELECT * FROM context_block_rules WHERE id = :id")
    suspend fun getRuleById(id: Int): ContextBlockRule?
    
    @Query("SELECT * FROM context_block_rules WHERE contextType = :type AND isEnabled = 1")
    suspend fun getEnabledRulesByType(type: String): List<ContextBlockRule>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ContextBlockRule): Long
    
    @Update
    suspend fun updateRule(rule: ContextBlockRule)
    
    @Delete
    suspend fun deleteRule(rule: ContextBlockRule)
    
    @Query("DELETE FROM context_block_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Int)
    
    @Query("UPDATE context_block_rules SET isEnabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun setRuleEnabled(id: Int, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM context_block_rules WHERE contextType = 'SCHEDULE' AND isEnabled = 1")
    suspend fun getActiveScheduleRules(): List<ContextBlockRule>
    
    @Query("SELECT * FROM context_block_rules WHERE contextType = 'LOCATION' AND isEnabled = 1")
    suspend fun getActiveLocationRules(): List<ContextBlockRule>
    
    @Query("SELECT * FROM context_block_rules WHERE contextType = 'WIFI' AND isEnabled = 1")
    suspend fun getActiveWifiRules(): List<ContextBlockRule>
}

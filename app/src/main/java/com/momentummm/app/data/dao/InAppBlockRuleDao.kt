package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.InAppBlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface InAppBlockRuleDao {

    @Query("SELECT * FROM in_app_block_rules ORDER BY appName, featureName")
    fun getAllRules(): Flow<List<InAppBlockRule>>

    @Query("SELECT * FROM in_app_block_rules WHERE isEnabled = 1")
    fun getAllEnabledRules(): Flow<List<InAppBlockRule>>

    @Query("SELECT * FROM in_app_block_rules WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getEnabledRulesForPackage(packageName: String): List<InAppBlockRule>

    @Query("SELECT * FROM in_app_block_rules WHERE ruleId = :ruleId")
    suspend fun getRuleById(ruleId: String): InAppBlockRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: InAppBlockRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<InAppBlockRule>)

    @Update
    suspend fun updateRule(rule: InAppBlockRule)

    @Query("UPDATE in_app_block_rules SET isEnabled = :enabled, updatedAt = :updatedAt WHERE ruleId = :ruleId")
    suspend fun updateRuleEnabled(ruleId: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteRule(rule: InAppBlockRule)

    @Query("DELETE FROM in_app_block_rules WHERE packageName = :packageName")
    suspend fun deleteRulesForPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM in_app_block_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesCount(): Int
}


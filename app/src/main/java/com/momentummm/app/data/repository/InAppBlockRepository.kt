package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.InAppBlockRuleDao
import com.momentummm.app.data.entity.InAppBlockRule
import com.momentummm.app.data.entity.BlockType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppBlockRepository @Inject constructor(
    private val inAppBlockRuleDao: InAppBlockRuleDao,
    @ApplicationContext private val context: Context
) {

    fun getAllRules(): Flow<List<InAppBlockRule>> = inAppBlockRuleDao.getAllRules()

    fun getAllEnabledRules(): Flow<List<InAppBlockRule>> = inAppBlockRuleDao.getAllEnabledRules()

    suspend fun getEnabledRulesForPackage(packageName: String): List<InAppBlockRule> =
        inAppBlockRuleDao.getEnabledRulesForPackage(packageName)

    suspend fun getRuleById(ruleId: String): InAppBlockRule? =
        inAppBlockRuleDao.getRuleById(ruleId)

    suspend fun insertRule(rule: InAppBlockRule) {
        inAppBlockRuleDao.insertRule(rule)
    }

    suspend fun updateRule(rule: InAppBlockRule) {
        inAppBlockRuleDao.updateRule(rule.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleRule(ruleId: String, enabled: Boolean) {
        inAppBlockRuleDao.updateRuleEnabled(ruleId, enabled)
    }

    suspend fun deleteRule(rule: InAppBlockRule) {
        inAppBlockRuleDao.deleteRule(rule)
    }

    suspend fun getEnabledRulesCount(): Int = inAppBlockRuleDao.getEnabledRulesCount()

    /**
     * Inicializa las reglas predeterminadas para apps populares
     */
    suspend fun initializeDefaultRules() {
        val existingRules = inAppBlockRuleDao.getAllRules().first()
        if (existingRules.isNotEmpty()) {
            return // Ya hay reglas, no inicializar
        }

        val defaultRules = listOf(
            // Instagram
            InAppBlockRule(
                ruleId = "instagram_reels",
                packageName = "com.instagram.android",
                appName = "Instagram",
                blockType = BlockType.REELS,
                featureName = "Reels",
                detectionPatterns = createPatternJson(listOf(
                    "com.instagram.clips",
                    "clips/camera",
                    "clips/viewer"
                ))
            ),
            InAppBlockRule(
                ruleId = "instagram_explore",
                packageName = "com.instagram.android",
                appName = "Instagram",
                blockType = BlockType.EXPLORE,
                featureName = "Explorar",
                detectionPatterns = createPatternJson(listOf(
                    "explore/",
                    "com.instagram.explore"
                ))
            ),

            // YouTube
            InAppBlockRule(
                ruleId = "youtube_shorts",
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                blockType = BlockType.SHORTS,
                featureName = "Shorts",
                detectionPatterns = createPatternJson(listOf(
                    "shorts/",
                    "com.google.android.youtube.shorts"
                ))
            ),
            InAppBlockRule(
                ruleId = "youtube_search",
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                blockType = BlockType.SEARCH,
                featureName = "Búsqueda",
                detectionPatterns = createPatternJson(listOf(
                    "search/",
                    "com.google.android.youtube.search"
                ))
            ),

            // Facebook
            InAppBlockRule(
                ruleId = "facebook_reels",
                packageName = "com.facebook.katana",
                appName = "Facebook",
                blockType = BlockType.REELS,
                featureName = "Reels",
                detectionPatterns = createPatternJson(listOf(
                    "reels/",
                    "com.facebook.reels"
                ))
            ),

            // Snapchat
            InAppBlockRule(
                ruleId = "snapchat_discover",
                packageName = "com.snapchat.android",
                appName = "Snapchat",
                blockType = BlockType.DISCOVER,
                featureName = "Discover",
                detectionPatterns = createPatternJson(listOf(
                    "discover/",
                    "com.snapchat.discover"
                ))
            ),

            // TikTok
            InAppBlockRule(
                ruleId = "tiktok_foryou",
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                blockType = BlockType.FOR_YOU,
                featureName = "For You",
                detectionPatterns = createPatternJson(listOf(
                    "foryou/",
                    "com.tiktok.foryou"
                ))
            ),

            // X (Twitter)
            InAppBlockRule(
                ruleId = "x_explore",
                packageName = "com.twitter.android",
                appName = "X",
                blockType = BlockType.EXPLORE,
                featureName = "Explorar",
                detectionPatterns = createPatternJson(listOf(
                    "explore/",
                    "com.twitter.explore"
                ))
            )
        )

        inAppBlockRuleDao.insertRules(defaultRules)
    }

    private fun createPatternJson(patterns: List<String>): String {
        val jsonArray = JSONArray()
        patterns.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    /**
     * Obtiene los patrones de detección de una regla
     */
    fun getDetectionPatterns(rule: InAppBlockRule): List<String> {
        return try {
            val jsonArray = JSONArray(rule.detectionPatterns)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}


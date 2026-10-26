package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.WebsiteBlockDao
import com.momentummm.app.data.entity.WebsiteBlock
import com.momentummm.app.data.entity.WebsiteCategory
import com.momentummm.app.data.entity.PredefinedWebsiteBlocks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebsiteBlockRepository @Inject constructor(
    private val websiteBlockDao: WebsiteBlockDao,
    @ApplicationContext private val context: Context
) {

    fun getAllBlocks(): Flow<List<WebsiteBlock>> = websiteBlockDao.getAllBlocks()

    fun getAllEnabledBlocks(): Flow<List<WebsiteBlock>> = websiteBlockDao.getAllEnabledBlocks()

    fun getBlocksByCategory(category: WebsiteCategory): Flow<List<WebsiteBlock>> =
        websiteBlockDao.getBlocksByCategory(category)

    suspend fun getBlockByUrl(url: String): WebsiteBlock? =
        websiteBlockDao.getBlockByUrl(url)

    suspend fun addWebsiteBlock(url: String, displayName: String, category: WebsiteCategory = WebsiteCategory.CUSTOM) {
        val normalizedUrl = normalizeUrl(url)
        val block = WebsiteBlock(
            url = normalizedUrl,
            displayName = displayName,
            category = category,
            isEnabled = true
        )
        websiteBlockDao.insertBlock(block)
    }

    suspend fun addPredefinedBlocks(category: WebsiteCategory) {
        val sites = when (category) {
            WebsiteCategory.ADULT_CONTENT -> PredefinedWebsiteBlocks.getAdultContentSites()
            WebsiteCategory.SOCIAL_MEDIA -> PredefinedWebsiteBlocks.getSocialMediaSites()
            WebsiteCategory.ENTERTAINMENT -> PredefinedWebsiteBlocks.getEntertainmentSites()
            WebsiteCategory.GAMING -> PredefinedWebsiteBlocks.getGamingSites()
            else -> emptyList()
        }

        val blocks = sites.map { (url, name) ->
            WebsiteBlock(
                url = normalizeUrl(url),
                displayName = name,
                category = category,
                isEnabled = true
            )
        }

        websiteBlockDao.insertBlocks(blocks)
    }

    suspend fun updateBlock(block: WebsiteBlock) {
        websiteBlockDao.updateBlock(block.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleBlock(blockId: Long, enabled: Boolean) {
        websiteBlockDao.updateBlockEnabled(blockId, enabled)
    }

    suspend fun deleteBlock(block: WebsiteBlock) {
        websiteBlockDao.deleteBlock(block)
    }

    suspend fun deleteBlocksByCategory(category: WebsiteCategory) {
        websiteBlockDao.deleteBlocksByCategory(category)
    }

    suspend fun isUrlBlocked(url: String): Boolean {
        val normalizedUrl = normalizeUrl(url)
        val enabledBlocks = websiteBlockDao.getAllEnabledBlocks().first()

        return enabledBlocks.any { block ->
            urlMatchesBlock(normalizedUrl, block.url)
        }
    }

    suspend fun getBlockedStats(): BlockedStats {
        val allBlocks = websiteBlockDao.getAllBlocks().first()
        val enabledBlocks = allBlocks.filter { it.isEnabled }

        val byCategory = WebsiteCategory.values().associateWith { category ->
            enabledBlocks.count { it.category == category }
        }

        return BlockedStats(
            totalBlocks = allBlocks.size,
            enabledBlocks = enabledBlocks.size,
            blocksByCategory = byCategory
        )
    }

    /**
     * Normaliza una URL para comparación
     * Ejemplos:
     * - https://www.facebook.com/page -> facebook.com
     * - http://pornhub.com/video/123 -> pornhub.com
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.lowercase().trim()

        // Remover protocolo
        normalized = normalized.replace(Regex("^https?://"), "")

        // Remover www.
        normalized = normalized.replace(Regex("^www\\."), "")

        // Remover path y query params
        normalized = normalized.split("/")[0].split("?")[0]

        return normalized
    }

    /**
     * Verifica si una URL coincide con un patrón de bloqueo
     */
    private fun urlMatchesBlock(url: String, blockPattern: String): Boolean {
        val normalizedUrl = normalizeUrl(url)
        val normalizedPattern = normalizeUrl(blockPattern)

        // Coincidencia exacta
        if (normalizedUrl == normalizedPattern) return true

        // Coincidencia de dominio (ej: facebook.com bloquea www.facebook.com y m.facebook.com)
        if (normalizedUrl.endsWith(".$normalizedPattern")) return true
        if (normalizedUrl.contains(normalizedPattern)) return true

        return false
    }

    /**
     * Obtiene sugerencias de sitios para bloquear basado en una categoría
     */
    fun getSuggestedSitesForCategory(category: WebsiteCategory): List<Pair<String, String>> {
        return when (category) {
            WebsiteCategory.ADULT_CONTENT -> PredefinedWebsiteBlocks.getAdultContentSites()
            WebsiteCategory.SOCIAL_MEDIA -> PredefinedWebsiteBlocks.getSocialMediaSites()
            WebsiteCategory.ENTERTAINMENT -> PredefinedWebsiteBlocks.getEntertainmentSites()
            WebsiteCategory.GAMING -> PredefinedWebsiteBlocks.getGamingSites()
            else -> emptyList()
        }
    }
}

data class BlockedStats(
    val totalBlocks: Int,
    val enabledBlocks: Int,
    val blocksByCategory: Map<WebsiteCategory, Int>
)


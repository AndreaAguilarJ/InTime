package com.momentummm.app.ui.screen.websiteblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.WebsiteBlock
import com.momentummm.app.data.entity.WebsiteCategory
import com.momentummm.app.data.repository.WebsiteBlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebsiteBlockUiState(
    val isLoading: Boolean = true,
    val websiteBlocks: List<WebsiteBlock> = emptyList(),
    val stats: WebsiteBlockStats = WebsiteBlockStats(),
    val error: String? = null
)

data class WebsiteBlockStats(
    val totalBlocks: Int = 0,
    val enabledBlocks: Int = 0,
    val adultContentBlocked: Int = 0,
    val socialMediaBlocked: Int = 0,
    val otherBlocked: Int = 0
)

@HiltViewModel
class WebsiteBlockViewModel @Inject constructor(
    private val websiteBlockRepository: WebsiteBlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebsiteBlockUiState())
    val uiState: StateFlow<WebsiteBlockUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                websiteBlockRepository.getAllBlocks().collect { blocks ->
                    val stats = calculateStats(blocks)
                    _uiState.value = _uiState.value.copy(
                        websiteBlocks = blocks,
                        stats = stats,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun addWebsiteBlock(url: String, displayName: String, category: WebsiteCategory) {
        viewModelScope.launch {
            try {
                websiteBlockRepository.addWebsiteBlock(url, displayName, category)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al agregar sitio: ${e.message}"
                )
            }
        }
    }

    fun addPredefinedBlocks(category: WebsiteCategory) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                websiteBlockRepository.addPredefinedBlocks(category)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al agregar sitios predefinidos: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun toggleBlock(block: WebsiteBlock) {
        viewModelScope.launch {
            try {
                websiteBlockRepository.toggleBlock(block.id, !block.isEnabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cambiar estado: ${e.message}"
                )
            }
        }
    }

    fun deleteBlock(block: WebsiteBlock) {
        viewModelScope.launch {
            try {
                websiteBlockRepository.deleteBlock(block)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar sitio: ${e.message}"
                )
            }
        }
    }

    fun deleteBlocksByCategory(category: WebsiteCategory) {
        viewModelScope.launch {
            try {
                websiteBlockRepository.deleteBlocksByCategory(category)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al eliminar categoría: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun calculateStats(blocks: List<WebsiteBlock>): WebsiteBlockStats {
        val enabledBlocks = blocks.filter { it.isEnabled }
        return WebsiteBlockStats(
            totalBlocks = blocks.size,
            enabledBlocks = enabledBlocks.size,
            adultContentBlocked = enabledBlocks.count { it.category == WebsiteCategory.ADULT_CONTENT },
            socialMediaBlocked = enabledBlocks.count { it.category == WebsiteCategory.SOCIAL_MEDIA },
            otherBlocked = enabledBlocks.count {
                it.category != WebsiteCategory.ADULT_CONTENT && it.category != WebsiteCategory.SOCIAL_MEDIA
            }
        )
    }
}


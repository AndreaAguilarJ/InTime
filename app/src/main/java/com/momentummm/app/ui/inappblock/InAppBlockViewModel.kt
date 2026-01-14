package com.momentummm.app.ui.inappblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.InAppBlockRule
import com.momentummm.app.data.repository.InAppBlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InAppBlockViewModel @Inject constructor(
    private val inAppBlockRepository: InAppBlockRepository
) : ViewModel() {

    val rules: StateFlow<List<InAppBlockRule>> = inAppBlockRepository.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Inicializar reglas predeterminadas si no existen
        viewModelScope.launch {
            inAppBlockRepository.initializeDefaultRules()
        }
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            inAppBlockRepository.toggleRule(ruleId, enabled)
        }
    }

    fun deleteRule(rule: InAppBlockRule) {
        viewModelScope.launch {
            inAppBlockRepository.deleteRule(rule)
        }
    }
}


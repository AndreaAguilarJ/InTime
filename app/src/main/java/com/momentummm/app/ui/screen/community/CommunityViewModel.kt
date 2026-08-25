package com.momentummm.app.ui.screen.community

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.*
import com.momentummm.app.data.manager.CommunityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.momentummm.app.R

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityManager: CommunityManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val friends: StateFlow<List<Friend>> = communityManager.friends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val pendingRequests: StateFlow<List<Friend>> = communityManager.pendingRequests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val weeklyLeaderboard: StateFlow<List<LeaderboardEntry>> = communityManager.weeklyLeaderboard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val friendsLeaderboard: StateFlow<List<LeaderboardEntry>> = communityManager.friendsLeaderboard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val myRank: StateFlow<LeaderboardEntry?> = communityManager.myRank
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val settings: StateFlow<CommunitySettings?> = communityManager.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    private val _achievements = MutableStateFlow<List<SharedAchievement>>(emptyList())
    val achievements: StateFlow<List<SharedAchievement>> = _achievements.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                communityManager.loadWeeklyLeaderboard()
                communityManager.loadFriendsLeaderboard()
                communityManager.updateMyLeaderboardEntry()
                loadAchievements()
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.community_err_load, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadAchievements() {
        try {
            communityManager.getAllAchievements().collect {
                _achievements.value = it
            }
        } catch (e: Exception) {
            // Silent fail for achievements
        }
    }
    
    fun refreshLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                communityManager.loadWeeklyLeaderboard()
                communityManager.loadFriendsLeaderboard()
                communityManager.updateMyLeaderboardEntry()
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.community_err_ranking, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                communityManager.refreshFriends()
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.community_err_friends, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshAchievements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadAchievements()
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.community_err_achievements, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun sendFriendRequest(email: String, name: String) {
        viewModelScope.launch {
            val result = communityManager.sendFriendRequest(email, name)
            result.onSuccess {
                _errorMessage.value = context.getString(R.string.community_request_sent, name)
            }.onFailure { e ->
                _errorMessage.value = context.getString(R.string.community_request_failed, e.message ?: "")
            }
        }
    }
    
    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            val result = communityManager.acceptFriendRequest(userId)
            result.onSuccess {
                _errorMessage.value = "✅ Solicitud aceptada"
            }.onFailure { e ->
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun rejectFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                communityManager.rejectFriendRequest(userId)
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun removeFriend(userId: String) {
        viewModelScope.launch {
            try {
                val result = communityManager.removeFriend(userId)
                result.onSuccess {
                    _errorMessage.value = "Amigo eliminado"
                }.onFailure { e ->
                    _errorMessage.value = "❌ Error: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun shareAchievement(context: Context, achievement: SharedAchievement) {
        viewModelScope.launch {
            try {
                communityManager.markAchievementAsShared(achievement.id)
                
                val shareText = communityManager.generateShareText(achievement)
                
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                
                val shareIntent = Intent.createChooser(sendIntent, "Compartir logro")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                
                // Recargar logros para actualizar UI
                loadAchievements()
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error al compartir: ${e.message}"
            }
        }
    }
    
    // ================== Settings Updates ==================
    
    fun updateShowInGlobalLeaderboard(show: Boolean) {
        viewModelScope.launch {
            try {
                communityManager.setShowInGlobalLeaderboard(show)
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun updateShowStreakToFriends(show: Boolean) {
        viewModelScope.launch {
            try {
                settings.value?.let {
                    communityManager.updateSettings(it.copy(showStreakToFriends = show))
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun updateShowFocusTimeToFriends(show: Boolean) {
        viewModelScope.launch {
            try {
                settings.value?.let {
                    communityManager.updateSettings(it.copy(showFocusTimeToFriends = show))
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun updateNotifyFriendRequests(notify: Boolean) {
        viewModelScope.launch {
            try {
                settings.value?.let {
                    communityManager.updateSettings(it.copy(notifyFriendRequests = notify))
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun updateNotifyFriendAchievements(notify: Boolean) {
        viewModelScope.launch {
            try {
                settings.value?.let {
                    communityManager.updateSettings(it.copy(notifyFriendAchievements = notify))
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
    
    fun updateNotifyLeaderboardChanges(notify: Boolean) {
        viewModelScope.launch {
            try {
                settings.value?.let {
                    communityManager.updateSettings(it.copy(notifyLeaderboardChanges = notify))
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }
}

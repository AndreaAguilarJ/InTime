package com.momentummm.app.ui.screen.community

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.entity.*
import com.momentummm.app.data.manager.CommunityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityManager: CommunityManager
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
    
    private val _achievements = kotlinx.coroutines.flow.MutableStateFlow<List<SharedAchievement>>(emptyList())
    val achievements: StateFlow<List<SharedAchievement>> = _achievements
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            communityManager.loadWeeklyLeaderboard()
            communityManager.loadFriendsLeaderboard()
            communityManager.updateMyLeaderboardEntry()
        }
    }
    
    fun refreshLeaderboard() {
        loadData()
    }
    
    fun sendFriendRequest(email: String, name: String) {
        viewModelScope.launch {
            communityManager.sendFriendRequest(email, name)
        }
    }
    
    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            communityManager.acceptFriendRequest(userId)
        }
    }
    
    fun rejectFriendRequest(userId: String) {
        viewModelScope.launch {
            communityManager.rejectFriendRequest(userId)
        }
    }
    
    fun removeFriend(userId: String) {
        viewModelScope.launch {
            communityManager.removeFriend(userId)
        }
    }
    
    fun shareAchievement(context: Context, achievement: SharedAchievement) {
        viewModelScope.launch {
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
        }
    }
}

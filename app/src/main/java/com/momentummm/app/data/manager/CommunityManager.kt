package com.momentummm.app.data.manager

import android.content.Context
import android.util.Log
import com.momentummm.app.data.dao.*
import com.momentummm.app.data.entity.*
import com.momentummm.app.data.appwrite.AppwriteService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager para funcionalidades de comunidad:
 * - Sistema de amigos
 * - Leaderboards semanales
 * - Compartir logros
 * - Configuración de privacidad
 */
@Singleton
class CommunityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val friendDao: FriendDao,
    private val leaderboardDao: LeaderboardDao,
    private val sharedAchievementDao: SharedAchievementDao,
    private val communitySettingsDao: CommunitySettingsDao,
    private val userDao: UserDao
) {
    private val TAG = "CommunityManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Estados observables
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<Friend>>(emptyList())
    val pendingRequests: StateFlow<List<Friend>> = _pendingRequests.asStateFlow()
    
    private val _weeklyLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val weeklyLeaderboard: StateFlow<List<LeaderboardEntry>> = _weeklyLeaderboard.asStateFlow()
    
    private val _friendsLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val friendsLeaderboard: StateFlow<List<LeaderboardEntry>> = _friendsLeaderboard.asStateFlow()
    
    private val _myRank = MutableStateFlow<LeaderboardEntry?>(null)
    val myRank: StateFlow<LeaderboardEntry?> = _myRank.asStateFlow()
    
    val settings: Flow<CommunitySettings?> = communitySettingsDao.getSettings()
    
    init {
        scope.launch {
            initializeSettings()
            loadFriends()
        }
    }
    
    private suspend fun initializeSettings() {
        val existing = communitySettingsDao.getSettingsSync()
        if (existing == null) {
            communitySettingsDao.insertSettings(CommunitySettings())
            Log.d(TAG, "Initialized default community settings")
        }
    }
    
    private fun loadFriends() {
        scope.launch {
            friendDao.getAcceptedFriends().collect {
                _friends.value = it
            }
        }
        scope.launch {
            friendDao.getPendingRequests().collect {
                _pendingRequests.value = it
            }
        }
    }
    
    // ================== SISTEMA DE AMIGOS ==================
    
    /**
     * Envía una solicitud de amistad por email
     */
    suspend fun sendFriendRequest(email: String, userName: String): Result<Friend> {
        return try {
            // Verificar si ya existe
            val existing = friendDao.getFriendByEmail(email)
            if (existing != null) {
                return Result.failure(Exception("Ya tienes una solicitud con este usuario"))
            }
            
            val friend = Friend(
                friendUserId = "", // Se actualizará cuando se sincronice con Appwrite
                friendName = userName,
                friendEmail = email,
                status = FriendStatus.PENDING,
                sentByMe = true
            )
            
            friendDao.insertFriend(friend)
            Log.d(TAG, "Friend request sent to $email")
            
            Result.success(friend)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Acepta una solicitud de amistad
     */
    suspend fun acceptFriendRequest(friendUserId: String): Result<Unit> {
        return try {
            friendDao.updateFriendStatus(friendUserId, FriendStatus.ACCEPTED)
            Log.d(TAG, "Friend request accepted: $friendUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Rechaza una solicitud de amistad
     */
    suspend fun rejectFriendRequest(friendUserId: String): Result<Unit> {
        return try {
            friendDao.updateFriendStatus(friendUserId, FriendStatus.REJECTED)
            Log.d(TAG, "Friend request rejected: $friendUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un amigo
     */
    suspend fun removeFriend(friendUserId: String): Result<Unit> {
        return try {
            friendDao.deleteFriendByUserId(friendUserId)
            Log.d(TAG, "Friend removed: $friendUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            Result.failure(e)
        }
    }
    
    /**
     * Bloquea un usuario
     */
    suspend fun blockUser(friendUserId: String): Result<Unit> {
        return try {
            friendDao.updateFriendStatus(friendUserId, FriendStatus.BLOCKED)
            Log.d(TAG, "User blocked: $friendUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user", e)
            Result.failure(e)
        }
    }
    
    fun getAcceptedFriendsCount(): Int = _friends.value.size
    
    suspend fun getPendingRequestsCount(): Int = friendDao.getPendingRequestsCount()
    
    // ================== LEADERBOARD ==================
    
    /**
     * Obtiene el inicio de la semana actual
     */
    private fun getCurrentWeekStart(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * Carga el leaderboard semanal
     */
    fun loadWeeklyLeaderboard() {
        val weekStart = getCurrentWeekStart()
        scope.launch {
            leaderboardDao.getWeeklyLeaderboard(weekStart).collect {
                _weeklyLeaderboard.value = it
            }
        }
    }
    
    /**
     * Carga el leaderboard de amigos
     */
    fun loadFriendsLeaderboard() {
        val weekStart = getCurrentWeekStart()
        scope.launch {
            leaderboardDao.getFriendsLeaderboard(weekStart).collect {
                _friendsLeaderboard.value = it
            }
        }
    }
    
    /**
     * Actualiza mi entrada en el leaderboard
     */
    suspend fun updateMyLeaderboardEntry() {
        try {
            val user = userDao.getUserSettingsSync()
            if (user == null) return
            
            val weekStart = getCurrentWeekStart()
            val existingEntry = _myRank.value
            val previousRank = existingEntry?.rank ?: 0
            
            val entry = LeaderboardEntry(
                odId = "local_${user.id}",
                userId = user.id.toString(),
                userName = "Yo", // Se actualizará con el nombre real de Appwrite
                weeklyFocusMinutes = user.totalFocusMinutes,
                weeklyPerfectDays = 0, // TODO: calcular
                currentStreak = user.currentStreak,
                userLevel = user.userLevel,
                rank = 0, // Se calculará al sincronizar
                previousRank = previousRank,
                weekStartDate = weekStart,
                isFriend = false
            )
            
            leaderboardDao.insertEntry(entry)
            _myRank.value = entry
            
            Log.d(TAG, "Updated my leaderboard entry")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating leaderboard entry", e)
        }
    }
    
    // ================== LOGROS COMPARTIBLES ==================
    
    /**
     * Registra un nuevo logro para compartir
     */
    suspend fun recordAchievement(
        type: AchievementType,
        value: Int = 0,
        message: String = ""
    ): SharedAchievement {
        val achievement = SharedAchievement(
            achievementType = type,
            achievementValue = value,
            message = message,
            isShared = false
        )
        
        val id = sharedAchievementDao.insertAchievement(achievement)
        Log.d(TAG, "Achievement recorded: $type with value $value")
        
        return achievement.copy(id = id.toInt())
    }
    
    /**
     * Marca un logro como compartido
     */
    suspend fun markAchievementAsShared(achievementId: Int) {
        sharedAchievementDao.markAsShared(achievementId)
        Log.d(TAG, "Achievement marked as shared: $achievementId")
    }
    
    /**
     * Genera el texto para compartir un logro
     */
    fun generateShareText(achievement: SharedAchievement): String {
        return when (achievement.achievementType) {
            AchievementType.STREAK_MILESTONE -> 
                "🔥 ¡Llevo ${achievement.achievementValue} días de racha en InTime! #DopaMineDiet"
            AchievementType.LEVEL_UP ->
                "⬆️ ¡Subí al nivel ${achievement.achievementValue} en InTime! #ProductividadDigital"
            AchievementType.PERFECT_WEEK ->
                "✨ ¡Semana perfecta completada! No excedí ningún límite de apps #InTime"
            AchievementType.FOCUS_MILESTONE ->
                "🎯 ¡${achievement.achievementValue} horas de foco acumuladas en InTime! #TiempoConsciente"
            AchievementType.NUCLEAR_COMPLETED ->
                "☢️ ¡Completé ${achievement.achievementValue} días en Modo Nuclear! #DetoxDigital"
            AchievementType.TOP_LEADERBOARD ->
                "🏆 ¡Top ${achievement.achievementValue} en el leaderboard semanal de InTime!"
            AchievementType.FIRST_WEEK ->
                "🚀 ¡Primera semana usando InTime para controlar mi tiempo de pantalla!"
            AchievementType.CUSTOM ->
                achievement.message.ifEmpty { "🎉 ¡Nuevo logro en InTime!" }
        }
    }
    
    // ================== CONFIGURACIÓN ==================
    
    suspend fun setProfileVisibility(visibility: ProfileVisibility) {
        communitySettingsDao.setProfileVisibility(visibility)
    }
    
    suspend fun setShowInGlobalLeaderboard(show: Boolean) {
        communitySettingsDao.setShowInGlobalLeaderboard(show)
    }
    
    suspend fun updateSettings(settings: CommunitySettings) {
        communitySettingsDao.updateSettings(settings)
    }
}

package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface FriendDao {
    
    @Query("SELECT * FROM friends WHERE status = 'ACCEPTED' ORDER BY friendName ASC")
    fun getAcceptedFriends(): Flow<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE status = 'PENDING' AND sentByMe = 0 ORDER BY createdAt DESC")
    fun getPendingRequests(): Flow<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE status = 'PENDING' AND sentByMe = 1 ORDER BY createdAt DESC")
    fun getSentRequests(): Flow<List<Friend>>
    
    @Query("SELECT * FROM friends WHERE friendUserId = :userId")
    suspend fun getFriendByUserId(userId: String): Friend?
    
    @Query("SELECT * FROM friends WHERE friendEmail = :email")
    suspend fun getFriendByEmail(email: String): Friend?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend): Long
    
    @Update
    suspend fun updateFriend(friend: Friend)
    
    @Delete
    suspend fun deleteFriend(friend: Friend)
    
    @Query("DELETE FROM friends WHERE friendUserId = :userId")
    suspend fun deleteFriendByUserId(userId: String)
    
    @Query("UPDATE friends SET status = :status, updatedAt = :timestamp WHERE friendUserId = :userId")
    suspend fun updateFriendStatus(userId: String, status: FriendStatus, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE friends SET friendLevel = :level, friendStreak = :streak, friendTotalFocusMinutes = :totalMinutes, friendWeeklyFocusMinutes = :weeklyMinutes, lastSyncedAt = :timestamp WHERE friendUserId = :userId")
    suspend fun updateFriendStats(
        userId: String,
        level: Int,
        streak: Int,
        totalMinutes: Int,
        weeklyMinutes: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM friends WHERE status = 'ACCEPTED'")
    suspend fun getAcceptedFriendsCount(): Int
    
    @Query("SELECT COUNT(*) FROM friends WHERE status = 'PENDING' AND sentByMe = 0")
    suspend fun getPendingRequestsCount(): Int
}

@Dao
interface LeaderboardDao {
    
    @Query("SELECT * FROM leaderboard_entries WHERE weekStartDate = :weekStart ORDER BY rank ASC LIMIT 100")
    fun getWeeklyLeaderboard(weekStart: Date): Flow<List<LeaderboardEntry>>
    
    @Query("SELECT * FROM leaderboard_entries WHERE weekStartDate = :weekStart AND isFriend = 1 ORDER BY rank ASC")
    fun getFriendsLeaderboard(weekStart: Date): Flow<List<LeaderboardEntry>>
    
    @Query("SELECT * FROM leaderboard_entries WHERE userId = :userId AND weekStartDate = :weekStart")
    suspend fun getUserEntry(userId: String, weekStart: Date): LeaderboardEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LeaderboardEntry>)
    
    @Query("DELETE FROM leaderboard_entries WHERE weekStartDate < :beforeDate")
    suspend fun deleteOldEntries(beforeDate: Date)
    
    @Query("DELETE FROM leaderboard_entries WHERE weekStartDate = :weekStart")
    suspend fun clearWeeklyLeaderboard(weekStart: Date)
}

@Dao
interface SharedAchievementDao {
    
    @Query("SELECT * FROM shared_achievements ORDER BY achievedAt DESC")
    fun getAllAchievements(): Flow<List<SharedAchievement>>
    
    @Query("SELECT * FROM shared_achievements WHERE isShared = 0 ORDER BY achievedAt DESC")
    fun getUnsharedAchievements(): Flow<List<SharedAchievement>>
    
    @Query("SELECT * FROM shared_achievements WHERE achievementType = :type ORDER BY achievedAt DESC LIMIT 1")
    suspend fun getLatestByType(type: AchievementType): SharedAchievement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: SharedAchievement): Long
    
    @Update
    suspend fun updateAchievement(achievement: SharedAchievement)
    
    @Query("UPDATE shared_achievements SET isShared = 1, shareCount = shareCount + 1 WHERE id = :id")
    suspend fun markAsShared(id: Int)
}

@Dao
interface CommunitySettingsDao {
    
    @Query("SELECT * FROM community_settings WHERE id = 1")
    fun getSettings(): Flow<CommunitySettings?>
    
    @Query("SELECT * FROM community_settings WHERE id = 1")
    suspend fun getSettingsSync(): CommunitySettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: CommunitySettings)
    
    @Update
    suspend fun updateSettings(settings: CommunitySettings)
    
    @Query("UPDATE community_settings SET profileVisibility = :visibility, updatedAt = :timestamp WHERE id = 1")
    suspend fun setProfileVisibility(visibility: ProfileVisibility, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE community_settings SET showInGlobalLeaderboard = :show, updatedAt = :timestamp WHERE id = 1")
    suspend fun setShowInGlobalLeaderboard(show: Boolean, timestamp: Long = System.currentTimeMillis())
}

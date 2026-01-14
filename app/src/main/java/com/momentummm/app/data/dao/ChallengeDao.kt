package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.Challenge
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ChallengeDao {

    @Query("SELECT * FROM challenges WHERE is_active = 1 ORDER BY start_date DESC")
    fun getActiveChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges ORDER BY created_date DESC")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE id = :challengeId")
    suspend fun getChallengeById(challengeId: String): Challenge?

    @Query("SELECT * FROM challenges WHERE difficulty = :difficulty")
    fun getChallengesByDifficulty(difficulty: String): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE is_completed = 0 AND is_active = 0")
    fun getAvailableChallenges(): Flow<List<Challenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge)

    @Update
    suspend fun updateChallenge(challenge: Challenge)

    @Delete
    suspend fun deleteChallenge(challenge: Challenge)

    @Query("UPDATE challenges SET is_active = 1, start_date = :startDate WHERE id = :challengeId")
    suspend fun startChallenge(challengeId: String, startDate: Date)

    @Query("UPDATE challenges SET is_completed = 1, completion_date = :completionDate, end_date = :endDate WHERE id = :challengeId")
    suspend fun completeChallenge(challengeId: String, completionDate: Date, endDate: Date)

    @Query("UPDATE challenges SET progress = :progress, days_completed = :daysCompleted WHERE id = :challengeId")
    suspend fun updateChallengeProgress(challengeId: String, progress: Float, daysCompleted: Int)

    @Query("UPDATE challenges SET current_streak = :streak WHERE id = :challengeId")
    suspend fun updateChallengeStreak(challengeId: String, streak: Int)

    @Query("UPDATE challenges SET best_streak = :bestStreak WHERE id = :challengeId")
    suspend fun updateChallengeBestStreak(challengeId: String, bestStreak: Int)

    @Query("UPDATE challenges SET is_active = 0 WHERE id = :challengeId")
    suspend fun deactivateChallenge(challengeId: String)

    // Statistics
    @Query("SELECT COUNT(*) FROM challenges WHERE is_active = 1")
    suspend fun getActiveChallengesCount(): Int

    @Query("SELECT COUNT(*) FROM challenges WHERE is_completed = 1")
    suspend fun getCompletedChallengesCount(): Int

    @Query("SELECT AVG(progress) FROM challenges WHERE is_active = 1")
    suspend fun getAverageChallengeProgress(): Float

    @Query("SELECT MAX(current_streak) FROM challenges WHERE is_active = 1")
    suspend fun getMaxStreak(): Int
}

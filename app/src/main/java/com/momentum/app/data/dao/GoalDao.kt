package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.Goal
import com.momentummm.app.data.entity.GoalProgress
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE is_active = 1 ORDER BY created_date DESC")
    fun getActiveGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY created_date DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): Goal?

    @Query("SELECT * FROM goals WHERE period = :period AND is_active = 1")
    fun getGoalsByPeriod(period: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("UPDATE goals SET is_active = 0 WHERE id = :goalId")
    suspend fun deactivateGoal(goalId: String)

    @Query("UPDATE goals SET current_value = :currentValue, last_updated = :lastUpdated WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: String, currentValue: Int, lastUpdated: Date)

    @Query("UPDATE goals SET current_streak = :streak WHERE id = :goalId")
    suspend fun updateGoalStreak(goalId: String, streak: Int)

    @Query("UPDATE goals SET best_streak = :bestStreak WHERE id = :goalId")
    suspend fun updateGoalBestStreak(goalId: String, bestStreak: Int)

    @Query("UPDATE goals SET completion_count = completion_count + 1 WHERE id = :goalId")
    suspend fun incrementGoalCompletion(goalId: String)

    // Goal Progress operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalProgress(goalProgress: GoalProgress)

    @Query("SELECT * FROM goal_progress WHERE goal_id = :goalId ORDER BY date DESC")
    fun getGoalProgress(goalId: String): Flow<List<GoalProgress>>

    @Query("SELECT * FROM goal_progress WHERE goal_id = :goalId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getGoalProgressInPeriod(goalId: String, startDate: Date, endDate: Date): List<GoalProgress>

    @Query("SELECT * FROM goal_progress WHERE goal_id = :goalId AND date = :date")
    suspend fun getGoalProgressForDate(goalId: String, date: Date): GoalProgress?

    @Query("SELECT COUNT(*) FROM goal_progress WHERE goal_id = :goalId AND is_completed = 1")
    suspend fun getGoalCompletionCount(goalId: String): Int

    @Query("DELETE FROM goal_progress WHERE goal_id = :goalId")
    suspend fun deleteGoalProgress(goalId: String)

    // Statistics
    @Query("SELECT COUNT(*) FROM goals WHERE is_active = 1")
    suspend fun getActiveGoalsCount(): Int

    @Query("SELECT COUNT(*) FROM goals WHERE completion_count > 0")
    suspend fun getCompletedGoalsCount(): Int

    @Query("SELECT AVG(current_streak) FROM goals WHERE is_active = 1")
    suspend fun getAverageStreak(): Float
}

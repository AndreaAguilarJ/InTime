package com.momentummm.app.data.repository

import com.momentummm.app.data.dao.GoalDao
import com.momentummm.app.data.dao.ChallengeDao
import com.momentummm.app.data.entity.Goal
import com.momentummm.app.data.entity.Challenge
import com.momentummm.app.data.entity.GoalProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val challengeDao: ChallengeDao
) {

    // Goals operations
    fun getActiveGoals(): Flow<List<Goal>> = goalDao.getActiveGoals()

    fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals()

    suspend fun getGoalById(goalId: String): Goal? = goalDao.getGoalById(goalId)

    suspend fun createGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }

    suspend fun deactivateGoal(goalId: String) {
        goalDao.deactivateGoal(goalId)
    }

    // Goal progress tracking
    suspend fun updateGoalProgress(goalId: String, currentValue: Int) {
        val goal = goalDao.getGoalById(goalId) ?: return
        val now = Date()

        // Update current progress
        goalDao.updateGoalProgress(goalId, currentValue, now)

        // Check if goal is completed for today/period
        val isCompleted = currentValue >= goal.targetValue

        if (isCompleted) {
            // Increment completion count
            goalDao.incrementGoalCompletion(goalId)

            // Update streak
            val newStreak = goal.currentStreak + 1
            goalDao.updateGoalStreak(goalId, newStreak)

            // Update best streak if necessary
            if (newStreak > goal.bestStreak) {
                goalDao.updateGoalBestStreak(goalId, newStreak)
            }
        }

        // Record daily progress
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.parse(dateFormat.format(now))!!

        val progressRecord = GoalProgress(
            id = "${goalId}_${dateFormat.format(today)}",
            goalId = goalId,
            date = today,
            value = currentValue,
            targetValue = goal.targetValue,
            isCompleted = isCompleted
        )

        goalDao.insertGoalProgress(progressRecord)
    }

    suspend fun calculateGoalStreaks() {
        val goals = goalDao.getAllGoals()
        // Implementation for calculating streaks based on goal progress history
    }

    // Challenges operations
    fun getActiveChallenges(): Flow<List<Challenge>> = challengeDao.getActiveChallenges()

    fun getAvailableChallenges(): Flow<List<Challenge>> = challengeDao.getAvailableChallenges()

    suspend fun getChallengeById(challengeId: String): Challenge? =
        challengeDao.getChallengeById(challengeId)

    suspend fun createChallenge(challenge: Challenge) {
        challengeDao.insertChallenge(challenge)
    }

    suspend fun startChallenge(challengeId: String) {
        val startDate = Date()
        challengeDao.startChallenge(challengeId, startDate)
    }

    suspend fun updateChallengeProgress(challengeId: String, daysCompleted: Int) {
        val challenge = challengeDao.getChallengeById(challengeId) ?: return
        val progress = daysCompleted.toFloat() / challenge.durationDays.toFloat()

        challengeDao.updateChallengeProgress(challengeId, progress, daysCompleted)

        // Check if challenge is completed
        if (daysCompleted >= challenge.durationDays) {
            val now = Date()
            challengeDao.completeChallenge(challengeId, now, now)
        }
    }

    suspend fun abandonChallenge(challengeId: String) {
        challengeDao.deactivateChallenge(challengeId)
    }

    // Statistics and achievements
    suspend fun getGoalStatistics(): GoalStatistics {
        val activeCount = goalDao.getActiveGoalsCount()
        val completedCount = goalDao.getCompletedGoalsCount()
        val averageStreak = goalDao.getAverageStreak()

        return GoalStatistics(
            activeGoals = activeCount,
            completedGoals = completedCount,
            averageStreak = averageStreak
        )
    }

    suspend fun getChallengeStatistics(): ChallengeStatistics {
        val activeCount = challengeDao.getActiveChallengesCount()
        val completedCount = challengeDao.getCompletedChallengesCount()
        val averageProgress = challengeDao.getAverageChallengeProgress()
        val maxStreak = challengeDao.getMaxStreak()

        return ChallengeStatistics(
            activeChallenges = activeCount,
            completedChallenges = completedCount,
            averageProgress = averageProgress,
            maxStreak = maxStreak
        )
    }

    // Achievement calculations
    suspend fun calculateAchievements(): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        val goalStats = getGoalStatistics()
        val challengeStats = getChallengeStatistics()

        // Goal-based achievements
        if (goalStats.completedGoals >= 5) {
            achievements.add(Achievement.GOAL_MASTER)
        }
        if (goalStats.averageStreak >= 7) {
            achievements.add(Achievement.STREAK_WARRIOR)
        }

        // Challenge-based achievements
        if (challengeStats.completedChallenges >= 3) {
            achievements.add(Achievement.CHALLENGE_CHAMPION)
        }
        if (challengeStats.maxStreak >= 30) {
            achievements.add(Achievement.CONSISTENCY_KING)
        }

        return achievements
    }

    // Real-time progress monitoring
    fun getGoalProgressFlow(goalId: String): Flow<GoalProgress?> {
        return goalDao.getGoalProgress(goalId).map { progressList ->
            progressList.firstOrNull()
        }
    }

    fun getDashboardData(): Flow<DashboardData> {
        return combine(
            getActiveGoals(),
            getActiveChallenges()
        ) { goals, challenges ->
            DashboardData(
                activeGoals = goals,
                activeChallenges = challenges,
                totalProgress = calculateTotalProgress(goals, challenges)
            )
        }
    }

    private fun calculateTotalProgress(goals: List<Goal>, challenges: List<Challenge>): Float {
        if (goals.isEmpty() && challenges.isEmpty()) return 0f

        val goalProgress = goals.map { goal ->
            if (goal.targetValue > 0) {
                (goal.currentValue.toFloat() / goal.targetValue.toFloat()).coerceAtMost(1f)
            } else 0f
        }.average().toFloat()

        val challengeProgress = challenges.map { it.progress }.average().toFloat()

        return ((goalProgress + challengeProgress) / 2f).coerceAtMost(1f)
    }
}

data class GoalStatistics(
    val activeGoals: Int,
    val completedGoals: Int,
    val averageStreak: Float
)

data class ChallengeStatistics(
    val activeChallenges: Int,
    val completedChallenges: Int,
    val averageProgress: Float,
    val maxStreak: Int
)

data class DashboardData(
    val activeGoals: List<Goal>,
    val activeChallenges: List<Challenge>,
    val totalProgress: Float
)

enum class Achievement(val title: String, val description: String, val icon: String) {
    GOAL_MASTER("Maestro de Metas", "Completa 5 metas", "🎯"),
    STREAK_WARRIOR("Guerrero de Rachas", "Mantén una racha promedio de 7 días", "🔥"),
    CHALLENGE_CHAMPION("Campeón de Desafíos", "Completa 3 desafíos", "🏆"),
    CONSISTENCY_KING("Rey de la Consistencia", "Mantén una racha de 30 días", "👑")
}

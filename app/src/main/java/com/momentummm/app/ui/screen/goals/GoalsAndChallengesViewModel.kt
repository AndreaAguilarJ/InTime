package com.momentummm.app.ui.screen.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentummm.app.data.repository.GoalsRepository
import com.momentummm.app.data.entity.Goal
import com.momentummm.app.data.entity.Challenge
import com.momentummm.app.notification.GoalNotificationManager
import com.momentummm.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalsAndChallengesViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository,
    private val notificationManager: GoalNotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                goalsRepository.getActiveGoals(),
                goalsRepository.getActiveChallenges(),
                goalsRepository.getAvailableChallenges()
            ) { activeGoals, activeChallenges, availableChallenges ->
                _uiState.value = _uiState.value.copy(
                    activeGoals = activeGoals,
                    activeChallenges = activeChallenges,
                    availableChallenges = availableChallenges,
                    isLoading = false
                )

                // Update statistics
                updateStatistics()
            }.collect()
        }
    }

    private suspend fun updateStatistics() {
        val goalStats = goalsRepository.getGoalStatistics()
        val challengeStats = goalsRepository.getChallengeStatistics()
        val achievements = goalsRepository.calculateAchievements()

        _uiState.value = _uiState.value.copy(
            goalStatistics = goalStats,
            challengeStatistics = challengeStats,
            achievements = achievements
        )
    }

    fun createGoal(
        title: String,
        description: String,
        targetValue: Int,
        period: String,
        category: String
    ) {
        viewModelScope.launch {
            val goal = Goal(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                targetValue = targetValue,
                period = period,
                category = category,
                createdDate = Date()
            )

            goalsRepository.createGoal(goal)

            // Schedule reminder notification
            scheduleGoalReminder(goal)

            _uiState.value = _uiState.value.copy(
                showCreateGoalDialog = false
            )
        }
    }

    fun updateGoalProgress(goalId: String, newValue: Int) {
        viewModelScope.launch {
            val oldGoal = goalsRepository.getGoalById(goalId)
            goalsRepository.updateGoalProgress(goalId, newValue)

            // Check for achievements
            val newGoal = goalsRepository.getGoalById(goalId)
            if (oldGoal != null && newGoal != null) {
                checkForNewAchievements(oldGoal, newGoal)
            }
        }
    }

    fun startChallenge(challengeId: String) {
        viewModelScope.launch {
            goalsRepository.startChallenge(challengeId)

            val challenge = goalsRepository.getChallengeById(challengeId)
            if (challenge != null) {
                notificationManager.showChallengeReminderNotification(challenge)
            }
        }
    }

    fun updateChallengeProgress(challengeId: String, daysCompleted: Int) {
        viewModelScope.launch {
            val oldChallenge = goalsRepository.getChallengeById(challengeId)
            goalsRepository.updateChallengeProgress(challengeId, daysCompleted)

            val newChallenge = goalsRepository.getChallengeById(challengeId)
            if (newChallenge != null && newChallenge.isCompleted && oldChallenge?.isCompleted == false) {
                // Challenge completed!
                notificationManager.showAchievementNotification(
                    context.getString(R.string.goals_notification_challenge_completed_title),
                    context.getString(
                        R.string.goals_notification_challenge_completed_message,
                        newChallenge.title,
                        newChallenge.reward
                    )
                )
            }
        }
    }

    fun abandonChallenge(challengeId: String) {
        viewModelScope.launch {
            goalsRepository.abandonChallenge(challengeId)
            notificationManager.cancelChallengeNotifications()
        }
    }

    fun deactivateGoal(goalId: String) {
        viewModelScope.launch {
            goalsRepository.deactivateGoal(goalId)
        }
    }

    private fun scheduleGoalReminder(goal: Goal) {
        viewModelScope.launch {
            // Schedule daily reminder for the goal
            notificationManager.showGoalReminderNotification(goal)
        }
    }

    private suspend fun checkForNewAchievements(oldGoal: Goal, newGoal: Goal) {
        // Check for streak achievements
        if (newGoal.currentStreak > oldGoal.currentStreak) {
            when (newGoal.currentStreak) {
                7 -> notificationManager.showAchievementNotification(
                    context.getString(R.string.goals_notification_streak_7_title),
                    context.getString(R.string.goals_notification_streak_7_message)
                )
                30 -> notificationManager.showAchievementNotification(
                    context.getString(R.string.goals_notification_streak_30_title),
                    context.getString(R.string.goals_notification_streak_30_message)
                )
                100 -> notificationManager.showAchievementNotification(
                    context.getString(R.string.goals_notification_streak_100_title),
                    context.getString(R.string.goals_notification_streak_100_message)
                )
            }

            // Show streak reminder
            if (newGoal.currentStreak >= 3) {
                notificationManager.showStreakReminderNotification(
                    newGoal.currentStreak,
                    newGoal.title
                )
            }
        }

        // Check for completion achievements
        if (newGoal.currentValue >= newGoal.targetValue && oldGoal.currentValue < oldGoal.targetValue) {
            notificationManager.showAchievementNotification(
                context.getString(R.string.goals_notification_goal_completed_title),
                context.getString(R.string.goals_notification_goal_completed_message, newGoal.title)
            )
        }
    }

    // UI State management
    fun showCreateGoalDialog() {
        _uiState.value = _uiState.value.copy(showCreateGoalDialog = true)
    }

    fun hideCreateGoalDialog() {
        _uiState.value = _uiState.value.copy(showCreateGoalDialog = false)
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadData()
        }
    }

    // Algorithm for calculating achievements and recommendations
    fun calculateGoalRecommendations(): List<GoalRecommendation> {
        val currentGoals = _uiState.value.activeGoals
        val recommendations = mutableListOf<GoalRecommendation>()

        // Analyze current goals and suggest improvements
        val hasScreenTimeGoal = currentGoals.any { it.category == "SCREEN_TIME" }
        val hasSocialMediaGoal = currentGoals.any { it.category == "SOCIAL_MEDIA" }
        val hasProductivityGoal = currentGoals.any { it.category == "PRODUCTIVITY" }

        if (!hasScreenTimeGoal) {
            recommendations.add(
                GoalRecommendation(
                    title = context.getString(R.string.goals_recommendation_screen_time_title),
                    description = context.getString(R.string.goals_recommendation_screen_time_desc),
                    category = "SCREEN_TIME",
                    suggestedTarget = 240, // 4 hours
                    priority = GoalPriority.HIGH
                )
            )
        }

        if (!hasSocialMediaGoal) {
            recommendations.add(
                GoalRecommendation(
                    title = context.getString(R.string.goals_recommendation_social_title),
                    description = context.getString(R.string.goals_recommendation_social_desc),
                    category = "SOCIAL_MEDIA",
                    suggestedTarget = 60, // 1 hour
                    priority = GoalPriority.MEDIUM
                )
            )
        }

        if (!hasProductivityGoal) {
            recommendations.add(
                GoalRecommendation(
                    title = context.getString(R.string.goals_recommendation_productivity_title),
                    description = context.getString(R.string.goals_recommendation_productivity_desc),
                    category = "PRODUCTIVITY",
                    suggestedTarget = 120, // 2 hours
                    priority = GoalPriority.MEDIUM
                )
            )
        }

        return recommendations
    }
}

data class GoalsUiState(
    val activeGoals: List<Goal> = emptyList(),
    val activeChallenges: List<Challenge> = emptyList(),
    val availableChallenges: List<Challenge> = emptyList(),
    val goalStatistics: com.momentummm.app.data.repository.GoalStatistics? = null,
    val challengeStatistics: com.momentummm.app.data.repository.ChallengeStatistics? = null,
    val achievements: List<com.momentummm.app.data.repository.Achievement> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateGoalDialog: Boolean = false,
    val errorMessage: String? = null
)

data class GoalRecommendation(
    val title: String,
    val description: String,
    val category: String,
    val suggestedTarget: Int,
    val priority: GoalPriority
)

enum class GoalPriority {
    LOW, MEDIUM, HIGH
}

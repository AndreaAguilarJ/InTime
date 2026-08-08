package com.momentummm.app.ui.screen.goals

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes
import com.momentummm.app.R
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.ui.theme.MomentumTextStyles
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Flag
import com.momentummm.app.ui.system.*
import com.momentummm.app.data.entity.Goal as DbGoal
import com.momentummm.app.data.entity.Challenge as DbChallenge
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.momentummm.app.ui.theme.*

// UI Data classes for presentation
data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val period: GoalPeriod,
    val category: GoalCategory,
    val isActive: Boolean = true,
    val createdDate: String,
    val endDate: String?,
    val currentStreak: Int = 0,
    val completionCount: Int = 0
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val iconType: ChallengeIcon,
    val difficulty: ChallengeDifficulty,
    val durationDays: Int,
    val progress: Float,
    val reward: String,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false,
    val daysCompleted: Int = 0,
    val currentStreak: Int = 0
)

enum class GoalPeriod(@StringRes val displayNameRes: Int) {
    DAILY(R.string.goal_period_daily),
    WEEKLY(R.string.goal_period_weekly),
    MONTHLY(R.string.goal_period_monthly)
}

enum class GoalCategory(@StringRes val displayNameRes: Int, val color: Color) {
    SCREEN_TIME(R.string.goal_category_screen_time, Sky500),
    SOCIAL_MEDIA(R.string.goal_category_social_media, Violet600),
    PRODUCTIVITY(R.string.goal_category_productivity, Mint500),
    FOCUS(R.string.goal_category_focus, Coral500),
    DIGITAL_DETOX(R.string.goal_category_digital_detox, Neutral500)
}

enum class ChallengeIcon {
    PHONE_OFF, TIMER, NATURE, BOOK, MEDITATION, EXERCISE
}

enum class ChallengeDifficulty(@StringRes val displayNameRes: Int, val color: Color) {
    EASY(R.string.challenge_difficulty_easy, Mint500),
    MEDIUM(R.string.challenge_difficulty_medium, Amber500),
    HARD(R.string.challenge_difficulty_hard, Rose500)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsAndChallengesScreen(
    isPremiumUser: Boolean,
    onUpgradeClick: () -> Unit,
    viewModel: GoalsAndChallengesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Comentado temporalmente - Promoción Premium para Goals
    /*
    if (!isPremiumUser) {
        PremiumGoalsUpsellScreen(onUpgradeClick = onUpgradeClick)
        return
    }
    */

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.goals_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.goals_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        item {
            // Quick stats
            QuickStatsCard(
                goalsCompleted = uiState.goalStatistics?.completedGoals ?: 0,
                goalsActive = uiState.activeGoals.size,
                challengesCompleted = uiState.challengeStatistics?.completedChallenges ?: 0,
                streakDays = uiState.goalStatistics?.averageStreak?.toInt() ?: 0
            )
        }
        
        item {
            // Tabs for Goals and Challenges
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.goals_tab_goals)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.goals_tab_challenges)) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Goals Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.goals_active_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        MomentumButton(
                            onClick = { viewModel.showCreateGoalDialog() },
                            style = ButtonStyle.Outline,
                            size = ButtonSize.Small,
                            icon = Icons.Filled.Add
                        ) {
                            Text(stringResource(R.string.goals_new_goal_button))
                        }
                    }
                }

                items(uiState.activeGoals.map { it.toUiModel() }) { goal ->
                    GoalCard(
                        goal = goal,
                        onUpdateProgress = { newValue ->
                            viewModel.updateGoalProgress(goal.id, newValue)
                        },
                        onDeactivate = {
                            viewModel.deactivateGoal(goal.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.activeGoals.isEmpty()) {
                    item {
                        MomentumCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stringResource(R.string.goals_empty_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.goals_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MomentumButton(
                                    onClick = { viewModel.showCreateGoalDialog() },
                                    style = ButtonStyle.Primary,
                                    icon = Icons.Filled.Add
                                ) {
                                    Text(stringResource(R.string.goals_empty_action))
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Challenges Tab
                item {
                    Text(
                        text = stringResource(R.string.goals_challenges_active_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.activeChallenges.map { it.toUiModel() }) { challenge ->
                    ChallengeCard(
                        challenge = challenge,
                        onJoinChallenge = {},
                        onUpdateProgress = { daysCompleted ->
                            viewModel.updateChallengeProgress(challenge.id, daysCompleted)
                        },
                        onAbandon = {
                            viewModel.abandonChallenge(challenge.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.goals_challenges_available_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(uiState.availableChallenges.map { it.toUiModel() }) { challenge ->
                    ChallengeCard(
                        challenge = challenge,
                        onJoinChallenge = {
                            viewModel.startChallenge(challenge.id)
                        },
                        onUpdateProgress = {},
                        onAbandon = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.activeChallenges.isEmpty() && uiState.availableChallenges.isEmpty()) {
                    item {
                        MomentumCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stringResource(R.string.goals_challenges_empty_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.goals_challenges_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MomentumButton(
                                    onClick = { viewModel.refreshData() },
                                    style = ButtonStyle.Outline,
                                    icon = Icons.Filled.Refresh
                                ) {
                                    Text(stringResource(R.string.goals_challenges_refresh_button))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Goal Dialog
    if (uiState.showCreateGoalDialog) {
        CreateGoalDialog(
            onDismiss = { viewModel.hideCreateGoalDialog() },
            onCreateGoal = { title, description, targetValue, period, category ->
                viewModel.createGoal(title, description, targetValue, period, category)
            }
        )
    }
}

// Extension functions to convert between DB entities and UI models
private fun DbGoal.toUiModel(): Goal {
    return Goal(
        id = id,
        title = title,
        description = description,
        targetValue = targetValue,
        currentValue = currentValue,
        period = GoalPeriod.valueOf(period),
        category = GoalCategory.valueOf(category),
        isActive = isActive,
        createdDate = createdDate.toString(),
        endDate = endDate?.toString(),
        currentStreak = currentStreak,
        completionCount = completionCount
    )
}

private fun DbChallenge.toUiModel(): Challenge {
    return Challenge(
        id = id,
        title = title,
        description = description,
        iconType = ChallengeIcon.valueOf(iconType),
        difficulty = ChallengeDifficulty.valueOf(difficulty),
        durationDays = durationDays,
        progress = progress,
        reward = reward,
        isCompleted = isCompleted,
        isActive = isActive,
        daysCompleted = daysCompleted,
        currentStreak = currentStreak
    )
}

@Composable
private fun PremiumGoalsUpsellScreen(
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.goals_premium_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.goals_premium_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val features = listOf(
            stringResource(R.string.goals_premium_feature_1),
            stringResource(R.string.goals_premium_feature_2),
            stringResource(R.string.goals_premium_feature_3),
            stringResource(R.string.goals_premium_feature_4),
            stringResource(R.string.goals_premium_feature_5),
            stringResource(R.string.goals_premium_feature_6)
        )
        
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MomentumButton(
            onClick = onUpgradeClick,
            style = ButtonStyle.Primary,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.goals_premium_cta))
        }
    }
}

@Composable
private fun QuickStatsCard(
    goalsCompleted: Int,
    goalsActive: Int,
    challengesCompleted: Int,
    streakDays: Int
) {
    MomentumCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.goals_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = goalsCompleted.toString(),
                    label = stringResource(R.string.goals_summary_completed),
                    icon = Icons.Filled.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    value = goalsActive.toString(),
                    label = stringResource(R.string.goals_summary_active),
                    icon = Icons.Filled.Flag,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    value = challengesCompleted.toString(),
                    label = stringResource(R.string.goals_summary_challenges_completed),
                    icon = Icons.Filled.EmojiEvents,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatItem(
                    value = streakDays.toString(),
                    label = stringResource(R.string.goals_summary_streak_days),
                    icon = Icons.Filled.Whatshot,
                    color = Coral500
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    modifier: Modifier = Modifier,
    onUpdateProgress: (Int) -> Unit,
    onDeactivate: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val progress = if (goal.targetValue > 0) {
        goal.currentValue.toFloat() / goal.targetValue.toFloat()
    } else 0f
    val isComplete = progress >= 1f

    // Una meta al 20% el lunes no es un fracaso: pintarla de rojo castiga al
    // usuario por el simple paso del tiempo. Se reserva el verde para el logro y
    // se usa el color de la categoría mientras la meta está en curso.
    val accent = if (isComplete) MaterialTheme.momentum.success else goal.category.color

    LaunchedEffect(isComplete) {
        if (isComplete) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    MomentumCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(MomentumDesign.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
            ) {
                IconTile(
                    icon = if (isComplete) Icons.Filled.Check else Icons.Filled.Flag,
                    tint = accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.momentum.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.momentum.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(MomentumDesign.Shapes.pill)
                        .background(goal.category.color.copy(alpha = MomentumDesign.Alpha.soft))
                        .padding(
                            horizontal = MomentumDesign.Spacing.small,
                            vertical = MomentumDesign.Spacing.extraSmall
                        )
                ) {
                    Text(
                        text = stringResource(goal.period.displayNameRes),
                        style = MomentumTextStyles.overline,
                        color = goal.category.color
                    )
                }
            }

            // La cifra alcanzada es la protagonista; el objetivo queda como
            // referencia secundaria en la misma línea de base.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(
                        R.string.goals_progress_current,
                        goal.currentValue / 60,
                        goal.currentValue % 60
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent
                )
                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.extraSmall))
                Text(
                    text = stringResource(
                        R.string.goals_progress_target,
                        goal.targetValue / 60,
                        goal.targetValue % 60
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textTertiary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.goals_progress_completed,
                        (progress * 100).toInt()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.momentum.textSecondary
                )
            }

            ProgressBar(
                progress = progress.coerceIn(0f, 1f),
                color = accent,
                height = 10.dp,
                modifier = Modifier.fillMaxWidth()
            )

            if (isComplete) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.extraSmall),
                    modifier = Modifier
                        .clip(MomentumDesign.Shapes.pill)
                        .background(MaterialTheme.momentum.success.copy(alpha = MomentumDesign.Alpha.soft))
                        .padding(
                            horizontal = MomentumDesign.Spacing.small,
                            vertical = MomentumDesign.Spacing.extraSmall
                        )
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.momentum.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.goals_completed_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.momentum.success
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    onJoinChallenge: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateProgress: (Int) -> Unit,
    onAbandon: () -> Unit
) {
    val icon = when (challenge.iconType) {
        ChallengeIcon.PHONE_OFF -> Icons.Filled.PhoneAndroid
        ChallengeIcon.TIMER -> Icons.Filled.Timer
        ChallengeIcon.NATURE -> Icons.Filled.Nature
        ChallengeIcon.BOOK -> Icons.Filled.Book
        ChallengeIcon.MEDITATION -> Icons.Filled.SelfImprovement
        ChallengeIcon.EXERCISE -> Icons.Filled.FitnessCenter
    }

    MomentumCard(
        modifier = modifier,
        // Un reto en curso se distingue por el borde de acento, no por un fondo
        // teñido: así el texto conserva el mismo contraste en ambos estados.
        border = androidx.compose.foundation.BorderStroke(
            width = if (challenge.isActive) 1.5.dp else 1.dp,
            color = if (challenge.isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = MomentumDesign.Alpha.strong)
            } else {
                MaterialTheme.momentum.border
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(MomentumDesign.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.compact)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)
            ) {
                IconTile(
                    icon = icon,
                    tint = challenge.difficulty.color,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.momentum.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.momentum.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(MomentumDesign.Shapes.pill)
                        .background(challenge.difficulty.color.copy(alpha = MomentumDesign.Alpha.soft))
                        .padding(
                            horizontal = MomentumDesign.Spacing.small,
                            vertical = MomentumDesign.Spacing.extraSmall
                        )
                ) {
                    Text(
                        text = stringResource(challenge.difficulty.displayNameRes),
                        style = MomentumTextStyles.overline,
                        color = challenge.difficulty.color
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.medium)
            ) {
                ChallengeMeta(
                    icon = Icons.Filled.CalendarToday,
                    text = stringResource(
                        R.string.goals_challenge_duration_days,
                        challenge.durationDays
                    )
                )
                ChallengeMeta(
                    icon = Icons.Filled.WorkspacePremium,
                    text = stringResource(R.string.goals_challenge_reward, challenge.reward),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            when {
                challenge.isActive -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = stringResource(
                                R.string.goals_challenge_day_progress,
                                (challenge.progress * challenge.durationDays).toInt() + 1,
                                challenge.durationDays
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.momentum.textPrimary
                        )
                        Text(
                            text = "${(challenge.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ProgressBar(
                        progress = challenge.progress.coerceIn(0f, 1f),
                        color = MaterialTheme.colorScheme.primary,
                        height = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                challenge.isCompleted -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MomentumDesign.Shapes.card)
                            .background(
                                MaterialTheme.momentum.success.copy(
                                    alpha = MomentumDesign.Alpha.soft
                                )
                            )
                            .padding(MomentumDesign.Spacing.compact),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.momentum.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(MomentumDesign.Spacing.small))
                        Text(
                            text = stringResource(R.string.goals_challenge_completed_badge),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.momentum.success
                        )
                    }
                }

                else -> {
                    MomentumButton(
                        onClick = onJoinChallenge,
                        style = ButtonStyle.Primary,
                        size = ButtonSize.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.goals_challenge_join_button))
                    }
                }
            }
        }
    }
}

/** Metadato de un reto: icono pequeño + texto. */
@Composable
private fun ChallengeMeta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = MaterialTheme.momentum.textSecondary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.extraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
    }
}

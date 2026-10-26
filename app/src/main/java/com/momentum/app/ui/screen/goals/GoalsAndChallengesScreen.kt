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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.ui.system.*
import com.momentummm.app.data.entity.Goal as DbGoal
import com.momentummm.app.data.entity.Challenge as DbChallenge
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

enum class GoalPeriod(val displayName: String) {
    DAILY("Diario"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual")
}

enum class GoalCategory(val displayName: String, val color: Color) {
    SCREEN_TIME("Tiempo de pantalla", Color(0xFF2196F3)),
    SOCIAL_MEDIA("Redes sociales", Color(0xFF9C27B0)),
    PRODUCTIVITY("Productividad", Color(0xFF4CAF50)),
    FOCUS("Enfoque", Color(0xFFFF5722)),
    DIGITAL_DETOX("Desintoxicación", Color(0xFF607D8B))
}

enum class ChallengeIcon {
    PHONE_OFF, TIMER, NATURE, BOOK, MEDITATION, EXERCISE
}

enum class ChallengeDifficulty(val displayName: String, val color: Color) {
    EASY("Fácil", Color(0xFF4CAF50)),
    MEDIUM("Medio", Color(0xFFFF9800)),
    HARD("Difícil", Color(0xFFF44336))
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
                        text = "Metas y Desafíos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Establece objetivos y completa desafíos",
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
                    text = { Text("Metas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Desafíos") }
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
                            text = "Metas Activas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        MomentumButton(
                            onClick = { viewModel.showCreateGoalDialog() },
                            style = ButtonStyle.Outline,
                            size = ButtonSize.Small,
                            icon = Icons.Filled.Add
                        ) {
                            Text("Nueva Meta")
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
                        EmptyStateCard(
                            title = "No hay metas activas",
                            description = "Crea tu primera meta para comenzar a mejorar tus hábitos digitales",
                            actionText = "Crear Meta",
                            onActionClick = { viewModel.showCreateGoalDialog() }
                        )
                    }
                }
            }

            1 -> {
                // Challenges Tab
                item {
                    Text(
                        text = "Desafíos Activos",
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
                        text = "Desafíos Disponibles",
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
                        EmptyStateCard(
                            title = "No hay desafíos disponibles",
                            description = "Los desafíos aparecerán aquí cuando estén disponibles",
                            actionText = "Actualizar",
                            onActionClick = { viewModel.refreshData() }
                        )
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
            text = "Metas y Desafíos",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Establece metas personalizadas y completa desafíos gamificados",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val features = listOf(
            "Metas personalizadas diarias, semanales y mensuales",
            "Desafíos gamificados de desintoxicación digital",
            "Sistema de recompensas y badges",
            "Seguimiento automático de progreso",
            "Recordatorios inteligentes",
            "Análisis de cumplimiento de objetivos"
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
            Text("Actualizar a Premium")
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
                text = "Resumen",
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
                    label = "Metas\ncompletadas",
                    icon = Icons.Filled.CheckCircle,
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    value = goalsActive.toString(),
                    label = "Metas\nactivas",
                    icon = Icons.Filled.Flag,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    value = challengesCompleted.toString(),
                    label = "Desafíos\ncompletados",
                    icon = Icons.Filled.EmojiEvents,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatItem(
                    value = streakDays.toString(),
                    label = "Días de\nracha",
                    icon = Icons.Filled.Whatshot,
                    color = Color(0xFFFF5722)
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
    val progress = if (goal.targetValue > 0) {
        goal.currentValue.toFloat() / goal.targetValue.toFloat()
    } else 0f
    
    val progressColor = when {
        progress >= 1.0f -> MaterialTheme.colorScheme.primary
        progress >= 0.8f -> Color(0xFF4CAF50)
        progress >= 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    MomentumCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = goal.category.color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = goal.period.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = goal.category.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${goal.currentValue / 60}h ${goal.currentValue % 60}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Meta: ${goal.targetValue / 60}h ${goal.targetValue % 60}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                MomentumProgressIndicator(
                    progress = progress.coerceAtMost(1.0f),
                    showPercentage = false,
                    color = progressColor,
                    modifier = Modifier.height(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% completado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (progress >= 1.0f) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "¡Completado!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
    MomentumCard(
        modifier = modifier,
        containerColor = if (challenge.isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = challenge.difficulty.color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = challenge.difficulty.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = challenge.difficulty.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (challenge.iconType) {
                        ChallengeIcon.PHONE_OFF -> Icons.Filled.PhoneAndroid
                        ChallengeIcon.TIMER -> Icons.Filled.Timer
                        ChallengeIcon.NATURE -> Icons.Filled.Nature
                        ChallengeIcon.BOOK -> Icons.Filled.Book
                        ChallengeIcon.MEDITATION -> Icons.Filled.SelfImprovement
                        ChallengeIcon.EXERCISE -> Icons.Filled.FitnessCenter
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "${challenge.durationDays} días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Recompensa: ${challenge.reward}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (challenge.isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                
                MomentumProgressIndicator(
                    progress = challenge.progress,
                    showPercentage = true,
                    modifier = Modifier.height(60.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Día ${(challenge.progress * challenge.durationDays).toInt() + 1} de ${challenge.durationDays}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!challenge.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                
                MomentumButton(
                    onClick = onJoinChallenge,
                    style = ButtonStyle.Primary,
                    size = ButtonSize.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unirse al Desafío")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¡Desafío Completado!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
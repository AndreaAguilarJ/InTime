package com.momentum.app.ui.screen.goals

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
import com.momentum.app.ui.system.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int, // in minutes
    val currentValue: Int, // in minutes
    val period: GoalPeriod,
    val category: GoalCategory,
    val isActive: Boolean = true,
    val createdDate: String,
    val endDate: String?
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val iconType: ChallengeIcon,
    val difficulty: ChallengeDifficulty,
    val durationDays: Int,
    val progress: Float, // 0.0 to 1.0
    val reward: String,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false
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
    onUpgradeClick: () -> Unit
) {
    val sampleGoals = listOf(
        Goal(
            id = "1",
            title = "Reducir tiempo de pantalla",
            description = "Usar el teléfono menos de 4 horas al día",
            targetValue = 240, // 4 hours
            currentValue = 320, // Current: 5h 20m
            period = GoalPeriod.DAILY,
            category = GoalCategory.SCREEN_TIME,
            createdDate = "2024-01-01",
            endDate = null
        ),
        Goal(
            id = "2",
            title = "Menos redes sociales",
            description = "Máximo 1 hora en Instagram y TikTok",
            targetValue = 60,
            currentValue = 45,
            period = GoalPeriod.DAILY,
            category = GoalCategory.SOCIAL_MEDIA,
            createdDate = "2024-01-01",
            endDate = null
        ),
        Goal(
            id = "3",
            title = "Semana productiva",
            description = "2 horas diarias en apps de productividad",
            targetValue = 840, // 14 hours per week
            currentValue = 600, // 10 hours this week
            period = GoalPeriod.WEEKLY,
            category = GoalCategory.PRODUCTIVITY,
            createdDate = "2024-01-01",
            endDate = null
        )
    )
    
    val sampleChallenges = listOf(
        Challenge(
            id = "1",
            title = "Desconexión Digital",
            description = "7 días sin usar el teléfono después de las 9 PM",
            iconType = ChallengeIcon.PHONE_OFF,
            difficulty = ChallengeDifficulty.MEDIUM,
            durationDays = 7,
            progress = 0.4f,
            reward = "Badge de Maestro del Atardecer",
            isActive = true
        ),
        Challenge(
            id = "2",
            title = "Enfoque Matutino",
            description = "30 días de no usar redes sociales hasta las 10 AM",
            iconType = ChallengeIcon.TIMER,
            difficulty = ChallengeDifficulty.HARD,
            durationDays = 30,
            progress = 0.6f,
            reward = "Badge de Guerrero del Amanecer",
            isActive = true
        ),
        Challenge(
            id = "3",
            title = "Fin de Semana Offline",
            description = "Usar el teléfono menos de 2 horas los fines de semana",
            iconType = ChallengeIcon.NATURE,
            difficulty = ChallengeDifficulty.EASY,
            durationDays = 14,
            progress = 0.0f,
            reward = "Badge de Explorador Natural"
        )
    )
    
    if (!isPremiumUser) {
        PremiumGoalsUpsellScreen(onUpgradeClick = onUpgradeClick)
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
                goalsCompleted = 2,
                goalsActive = sampleGoals.count { it.isActive },
                challengesCompleted = 5,
                streakDays = 12
            )
        }
        
        item {
            // Active goals section
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
                    onClick = { /* Create new goal */ },
                    style = ButtonStyle.Outline,
                    size = ButtonSize.Small,
                    icon = Icons.Filled.Add
                ) {
                    Text("Nueva Meta")
                }
            }
        }
        
        items(sampleGoals.filter { it.isActive }) { goal ->
            GoalCard(
                goal = goal,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            // Challenges section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desafíos Disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sampleChallenges.count { it.isActive }} activos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        items(sampleChallenges) { challenge ->
            ChallengeCard(
                challenge = challenge,
                onJoinChallenge = { /* Join challenge */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
                    icon = Icons.Filled.LocalFire,
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
    modifier: Modifier = Modifier
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
    modifier: Modifier = Modifier
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
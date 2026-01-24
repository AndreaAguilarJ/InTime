package com.momentummm.app.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R
import com.momentummm.app.data.manager.GamificationState

/**
 * Componente visual de gamificación para el Dashboard.
 * Muestra nivel, XP, TimeCoins y racha del usuario.
 */
@Composable
fun GamificationHeader(
    gamificationState: GamificationState?,
    modifier: Modifier = Modifier,
    onCoinsClick: () -> Unit = {}
) {
    if (gamificationState == null) {
        // Skeleton loading
        GamificationHeaderSkeleton(modifier)
        return
    }

    var isAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(gamificationState.level) {
        isAnimating = true
        kotlinx.coroutines.delay(1000)
        isAnimating = false
    }

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila superior: Nivel y TimeCoins
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nivel y título
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Badge de nivel
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = gamificationState.levelEmoji,
                                    fontSize = 28.sp
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.gamification_level_format,
                                    gamificationState.level
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = gamificationState.levelTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // TimeCoins y Racha
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        // TimeCoins
                        Surface(
                            onClick = onCoinsClick,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🪙", fontSize = 16.sp)
                                Text(
                                    text = formatNumber(gamificationState.timeCoins),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Racha
                        if (gamificationState.currentStreak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🔥", fontSize = 14.sp)
                                Text(
                                    text = stringResource(
                                        R.string.gamification_header_streak_days,
                                        gamificationState.currentStreak
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                if (gamificationState.streakMultiplier > 1f) {
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "x${String.format("%.1f", gamificationState.streakMultiplier)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Barra de progreso de XP
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.gamification_header_xp_label,
                                formatNumber(gamificationState.currentXp)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(
                                R.string.gamification_header_xp_to_next_level,
                                formatNumber(gamificationState.xpForNextLevel),
                                gamificationState.level + 1
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Barra de progreso animada
                    val progress by animateFloatAsState(
                        targetValue = gamificationState.xpProgress,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "xp_progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF10B981),
                                            Color(0xFF34D399)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Stats rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStat(
                        emoji = "⏱️",
                        value = formatMinutesToHours(gamificationState.totalFocusMinutes),
                        label = stringResource(R.string.gamification_header_total_focus)
                    )
                    QuickStat(
                        emoji = "✅",
                        value = gamificationState.totalSessions.toString(),
                        label = stringResource(R.string.gamification_header_sessions)
                    )
                    QuickStat(
                        emoji = "⭐",
                        value = gamificationState.perfectDays.toString(),
                        label = stringResource(R.string.gamification_header_perfect_days)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStat(
    emoji: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun GamificationHeaderSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Notificación de evento de gamificación (level up, xp ganado, etc.)
 */
@Composable
fun GamificationEventToast(
    message: String,
    xpGained: Int = 0,
    coinsGained: Int = 0,
    isLevelUp: Boolean = false,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        LaunchedEffect(visible) {
            if (visible) {
                kotlinx.coroutines.delay(3000)
                onDismiss()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLevelUp) Color(0xFF6366F1) else Color(0xFF10B981)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isLevelUp) "🎉" else "✨",
                    fontSize = 32.sp
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (xpGained != 0) {
                            Text(
                                text = stringResource(
                                    R.string.gamification_event_xp_format,
                                    if (xpGained > 0) "+" else "",
                                    xpGained
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        if (coinsGained > 0) {
                            Text(
                                text = stringResource(
                                    R.string.gamification_event_coins_format,
                                    coinsGained
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.gamification_event_close_cd),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Helpers
private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

private fun formatMinutesToHours(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

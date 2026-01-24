package com.momentummm.app.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R

/**
 * Contador de TimeCoins animado para el TopBar.
 * Muestra el balance actual con animaciones de incremento y brillo.
 */
@Composable
fun AnimatedTimeCoinCounter(
    coins: Int,
    modifier: Modifier = Modifier,
    showPlusButton: Boolean = false,
    onPlusClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    // Track previous value para animación
    var previousCoins by remember { mutableStateOf(coins) }
    var isAnimating by remember { mutableStateOf(false) }
    var coinDelta by remember { mutableStateOf(0) }
    
    // Detectar cambio en monedas
    LaunchedEffect(coins) {
        if (coins != previousCoins) {
            coinDelta = coins - previousCoins
            isAnimating = true
            kotlinx.coroutines.delay(1500)
            isAnimating = false
            previousCoins = coins
        }
    }

    // Animación de escala cuando gana monedas
    val scale by animateFloatAsState(
        targetValue = if (isAnimating && coinDelta > 0) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coinScale"
    )

    // Animación de rotación de la moneda
    val infiniteTransition = rememberInfiniteTransition(label = "coinRotation")
    val coinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animación del contador
    val animatedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "coinCount"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        color = Color(0xFF2D2D2D),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Emoji de moneda con posible rotación
            Box(
                modifier = Modifier.rotate(if (isAnimating && coinDelta > 0) coinRotation else 0f)
            ) {
                Text(
                    text = "🪙",
                    fontSize = 18.sp
                )
            }
            
            // Número de monedas
            Text(
                text = formatCoins(animatedCoins),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            // Indicador de delta
            AnimatedVisibility(
                visible = isAnimating && coinDelta != 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { if (coinDelta > 0) 20 else -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { if (coinDelta > 0) -20 else 20 })
            ) {
                Text(
                    text = if (coinDelta > 0) "+$coinDelta" else "$coinDelta",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (coinDelta > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }

            // Botón de obtener más monedas
            if (showPlusButton) {
                Surface(
                    onClick = onPlusClick,
                    color = Color(0xFF10B981),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.timecoins_get_more_cd),
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Formatea el número de monedas para display compacto
 */
private fun formatCoins(coins: Int): String {
    return when {
        coins >= 1_000_000 -> String.format("%.1fM", coins / 1_000_000f)
        coins >= 10_000 -> String.format("%.1fK", coins / 1_000f)
        coins >= 1_000 -> String.format("%.1fK", coins / 1_000f)
        else -> coins.toString()
    }
}

/**
 * Componente de "toast" cuando el usuario gana monedas
 */
@Composable
fun CoinEarnedToast(
    amount: Int,
    reason: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -100 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -100 })
    ) {
        LaunchedEffect(visible) {
            if (visible) {
                kotlinx.coroutines.delay(2500)
                onDismiss()
            }
        }

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.9f),
                                Color(0xFF059669).copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Moneda animada
                    val rotation by rememberInfiniteTransition(label = "toast").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "toastRotation"
                    )
                    
                    Text(
                        text = "🪙",
                        fontSize = 28.sp,
                        modifier = Modifier.rotate(rotation)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.timecoins_earned_format, amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente visual de balance de TimeCoins para la tienda
 */
@Composable
fun TimeCoinBalance(
    coins: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                            Color(0xFFFFD700).copy(alpha = 0.15f),
                            Color(0xFFFFA500).copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.timecoins_balance_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🪙", fontSize = 32.sp)
                        Text(
                            text = formatCoins(coins),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.timecoins_earn_more),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.timecoins_earn_more_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

package com.momentummm.app.ui.component

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R
import com.momentummm.app.data.manager.BillingManager
import com.momentummm.app.util.SocialShareUtils
import kotlinx.coroutines.delay

/**
 * Pantalla de desbloqueo de emergencia con opciones virales.
 * Implementa el sistema "Shame/Glory Sharing" para viralidad.
 */
@Composable
fun EmergencyUnlockScreen(
    blockedAppName: String,
    currentStreakDays: Int = 0,
    billingManager: BillingManager? = null,
    onUnlockWithPayment: () -> Unit,
    onUnlockWithShame: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<UnlockOption?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var shameCountdown by remember { mutableStateOf(3) }
    var showShameCountdown by remember { mutableStateOf(false) }
    val confirmPaymentTitle = stringResource(R.string.emergency_unlock_confirm_payment_title)
    val confirmShameTitle = stringResource(R.string.emergency_unlock_confirm_shame_title)
    val confirmPaymentMessage = stringResource(
        R.string.emergency_unlock_confirm_payment_message,
        blockedAppName
    )
    val confirmShameMessage = stringResource(R.string.emergency_unlock_confirm_shame_message)
    val confirmPaymentButton = stringResource(R.string.emergency_unlock_confirm_payment_button)
    val confirmShameButton = stringResource(R.string.emergency_unlock_confirm_shame_button)
    val confirmDefaultButton = stringResource(R.string.emergency_unlock_confirm_default_button)
    val cancelText = stringResource(R.string.emergency_unlock_cancel)
    val premiumPrice = stringResource(R.string.emergency_unlock_premium_price)
    val shamePrice = stringResource(R.string.emergency_unlock_shame_price)

    // Animación de entrada
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Countdown para shame share
    LaunchedEffect(showShameCountdown) {
        if (showShameCountdown) {
            while (shameCountdown > 0) {
                delay(1000)
                shameCountdown--
            }
            // Ejecutar share
            SocialShareUtils.shareShameImage(
                context = context,
                appName = blockedAppName,
                shameType = SocialShareUtils.ShameType.DOPAMINE_FAIL,
                streakDays = currentStreakDays
            )
            onUnlockWithShame()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0505),
                        Color(0xFF2D0A0A),
                        Color(0xFF1A0505)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Icono de advertencia
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.5f)
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    color = Color(0xFFFF4444).copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6666),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // Título
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { -30 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.emergency_unlock_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_unlock_question_line1),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = blockedAppName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6666),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.emergency_unlock_question_line2),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mensaje de racha en riesgo
            if (currentStreakDays > 0) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 400))
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF4444).copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔥", fontSize = 32.sp)
                            Column {
                                Text(
                                    text = stringResource(
                                        R.string.emergency_unlock_streak_risk,
                                        currentStreakDays
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6666)
                                )
                                Text(
                                    text = stringResource(R.string.emergency_unlock_streak_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OPCIÓN 1: Pago de emergencia
            AnimatedVisibility(
                visible = isVisible && !showShameCountdown,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 600)) +
                        slideInVertically(initialOffsetY = { 40 })
            ) {
                UnlockOptionCard(
                    icon = Icons.Filled.CreditCard,
                    iconTint = Color(0xFF6366F1),
                    title = stringResource(R.string.emergency_unlock_premium_title),
                    subtitle = stringResource(R.string.emergency_unlock_premium_subtitle),
                    price = premiumPrice,
                    backgroundColor = Color(0xFF6366F1).copy(alpha = 0.1f),
                    borderColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                    onClick = {
                        selectedOption = UnlockOption.PAYMENT
                        showConfirmDialog = true
                    }
                )
            }

            // OPCIÓN 2: Shame Sharing (GRATIS pero viral)
            AnimatedVisibility(
                visible = isVisible && !showShameCountdown,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 800)) +
                        slideInVertically(initialOffsetY = { 40 })
            ) {
                UnlockOptionCard(
                    icon = Icons.Filled.Share,
                    iconTint = Color(0xFFFF6666),
                    title = stringResource(R.string.emergency_unlock_shame_title),
                    subtitle = stringResource(R.string.emergency_unlock_shame_subtitle),
                    price = shamePrice,
                    priceColor = Color(0xFF10B981),
                    backgroundColor = Color(0xFFFF4444).copy(alpha = 0.1f),
                    borderColor = Color(0xFFFF4444).copy(alpha = 0.3f),
                    badge = stringResource(R.string.emergency_unlock_shame_badge),
                    onClick = {
                        selectedOption = UnlockOption.SHAME_SHARE
                        showConfirmDialog = true
                    }
                )
            }

            // Countdown de vergüenza
            AnimatedVisibility(
                visible = showShameCountdown,
                enter = fadeIn() + scaleIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🤡",
                        fontSize = 80.sp
                    )
                    Text(
                        text = stringResource(R.string.emergency_unlock_shame_preparing),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$shameCountdown",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4444)
                    )
                    Text(
                        text = stringResource(R.string.emergency_unlock_shame_share_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón cancelar
            AnimatedVisibility(
                visible = isVisible && !showShameCountdown,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 1000))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.emergency_unlock_cancel_button))
                    }

                    Text(
                        text = stringResource(R.string.emergency_unlock_motivation_quote),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de confirmación
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = {
                Text(
                    text = when (selectedOption) {
                        UnlockOption.PAYMENT -> confirmPaymentTitle
                        UnlockOption.SHAME_SHARE -> confirmShameTitle
                        null -> ""
                    }
                )
            },
            text = {
                Text(
                    text = when (selectedOption) {
                        UnlockOption.PAYMENT -> confirmPaymentMessage
                        UnlockOption.SHAME_SHARE -> confirmShameMessage
                        null -> ""
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        when (selectedOption) {
                            UnlockOption.PAYMENT -> {
                                onUnlockWithPayment()
                            }
                            UnlockOption.SHAME_SHARE -> {
                                showShameCountdown = true
                            }
                            null -> {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedOption) {
                            UnlockOption.PAYMENT -> Color(0xFF6366F1)
                            UnlockOption.SHAME_SHARE -> Color(0xFFFF4444)
                            null -> Color(0xFF6366F1)
                        }
                    )
                ) {
                    Text(
                        when (selectedOption) {
                            UnlockOption.PAYMENT -> confirmPaymentButton
                            UnlockOption.SHAME_SHARE -> confirmShameButton
                            null -> confirmDefaultButton
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(cancelText, color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun UnlockOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    price: String,
    priceColor: Color = Color.White,
    backgroundColor: Color,
    borderColor: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Surface(
                color = iconTint.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contenido
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    badge?.let {
                        Surface(
                            color = Color(0xFFFF4444),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Precio
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = priceColor
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private enum class UnlockOption {
    PAYMENT,
    SHAME_SHARE
}

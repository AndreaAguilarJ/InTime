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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        text = "⚠️ Desbloqueo de Emergencia",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Realmente necesitas acceder a",
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
                        text = "ahora mismo?",
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
                                    text = "Tu racha de $currentStreakDays días está en riesgo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6666)
                                )
                                Text(
                                    text = "Piénsalo dos veces antes de rendirte",
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
                    title = "Desbloqueo Premium",
                    subtitle = "Acceso instantáneo por 15 minutos",
                    price = "$0.99 USD",
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
                    title = "Desbloqueo de la Vergüenza",
                    subtitle = "Comparte tu fallo en redes sociales",
                    price = "GRATIS",
                    priceColor = Color(0xFF10B981),
                    backgroundColor = Color(0xFFFF4444).copy(alpha = 0.1f),
                    borderColor = Color(0xFFFF4444).copy(alpha = 0.3f),
                    badge = "🤡 VIRAL",
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
                        text = "Preparando tu momento de vergüenza...",
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
                        text = "Se abrirá el selector para compartir",
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
                        Text("Mejor no, me quedo fuerte 💪")
                    }

                    Text(
                        text = "\"El dolor de la disciplina pesa gramos,\nel arrepentimiento pesa toneladas\"",
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
                        UnlockOption.PAYMENT -> "Confirmar pago"
                        UnlockOption.SHAME_SHARE -> "¿Estás seguro?"
                        null -> ""
                    }
                )
            },
            text = {
                Text(
                    text = when (selectedOption) {
                        UnlockOption.PAYMENT -> "Se cobrará $0.99 USD por 15 minutos de acceso a $blockedAppName."
                        UnlockOption.SHAME_SHARE -> "Se generará una imagen con el texto \"Fallé mi dieta de dopamina en InTime 🤡\" y se abrirá el selector para compartir en tus redes sociales.\n\n¡Tus amigos te verán! 😱"
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
                            UnlockOption.PAYMENT -> "Pagar $0.99"
                            UnlockOption.SHAME_SHARE -> "Sí, publicar mi vergüenza 🤡"
                            null -> "Confirmar"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
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

package com.momentummm.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.MainActivity
import com.momentummm.app.ui.system.*
import com.momentummm.app.ui.theme.MomentumTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class AppBlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedAppName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Aplicación"
        val dailyLimit = intent.getIntExtra(EXTRA_DAILY_LIMIT, 0)

        setContent {
            MomentumTheme {
                AppBlockedScreen(
                    blockedAppName = blockedAppName,
                    dailyLimit = dailyLimit,
                    onOpenMomentum = {
                        // Abrir la MainActivity
                        val launchIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(launchIntent)
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevenir que el usuario vuelva a la app bloqueada
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(launchIntent)
        finish()
    }

    companion object {
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_DAILY_LIMIT = "extra_daily_limit"

        fun start(context: Context, appName: String, dailyLimit: Int) {
            val intent = Intent(context, AppBlockedActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_DAILY_LIMIT, dailyLimit)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
private fun AppBlockedScreen(
    blockedAppName: String,
    dailyLimit: Int,
    onOpenMomentum: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableStateOf(5) }
    var isVisible by remember { mutableStateOf(false) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Countdown timer
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    // Animación de pulso para el ícono
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // Elementos decorativos de fondo
        FloatingBackgroundElements()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Icono de bloqueo animado con efecto glassmorphism
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo exterior con blur
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .alpha(alpha * 0.3f),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        shape = CircleShape
                    ) {}

                    // Círculo principal
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(scale),
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        shadowElevation = 16.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }
            }

            // Título principal con animación
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { -40 },
                            animationSpec = tween(600, delayMillis = 200)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏰ Tiempo Agotado",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Has alcanzado tu límite diario",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Card principal con información - Glassmorphism
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 400)
                        )
            ) {
                GlassmorphicCard {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // App bloqueada
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = blockedAppName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        // Tiempo límite
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "$dailyLimit minutos",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Límite diario alcanzado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Estadísticas motivacionales
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 600)
                        )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Filled.EmojiEvents,
                        value = "¡Logro!",
                        label = "Meta cumplida",
                        color = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.RestartAlt,
                        value = "00:00",
                        label = "Reinicio mañana",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mensaje motivacional
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 800)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 800)
                        )
            ) {
                GlassmorphicCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Celebration,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "¡Excelente autocontrol! Estás construyendo hábitos más saludables.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sugerencias de actividades
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 1000)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 1000)
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "💡 Sugerencias para aprovechar tu tiempo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    val suggestions = listOf(
                        SuggestionItem(Icons.Outlined.Book, "Lee un libro", "Expande tu conocimiento"),
                        SuggestionItem(Icons.Outlined.DirectionsWalk, "Sal a caminar", "Mueve tu cuerpo"),
                        SuggestionItem(Icons.Outlined.SelfImprovement, "Medita", "Calma tu mente"),
                        SuggestionItem(Icons.Outlined.FitnessCenter, "Ejercicio", "Mantente activo"),
                        SuggestionItem(Icons.Outlined.Palette, "Hobby creativo", "Explora tu creatividad")
                    )

                    suggestions.take(3).forEachIndexed { index, suggestion ->
                        SuggestionCard(
                            icon = suggestion.icon,
                            title = suggestion.title,
                            description = suggestion.description,
                            delay = index * 100
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 1200)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 1200)
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MomentumButton(
                        onClick = onOpenMomentum,
                        style = ButtonStyle.Primary,
                        size = ButtonSize.Large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Momentum")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = countdown == 0,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (countdown == 0) Color.White.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (countdown > 0) "Cerrar en $countdown..." else "Cerrar",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GlassmorphicCard(
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        content()
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        backgroundColor = Color.White.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    icon: ImageVector,
    title: String,
    description: String,
    delay: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInHorizontally(
                    initialOffsetX = { -40 },
                    animationSpec = tween(400)
                )
    ) {
        GlassmorphicCard(
            backgroundColor = Color.White.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingBackgroundElements() {
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    repeat(3) { index ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000 + index * 500,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offsetY$index"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(
                    x = (50 + index * 100).dp,
                    y = (100 + index * 150 + offsetY.toInt()).dp
                )
        ) {
            Surface(
                modifier = Modifier
                    .size((80 + index * 40).dp)
                    .alpha(0.03f),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {}
        }
    }
}

private data class SuggestionItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

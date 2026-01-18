package com.momentummm.app.ui.screen.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.util.PermissionUtils
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Pantalla de "Shock" Onboarding - Muestra una proyección alarmante del tiempo
 * que el usuario pasará en su teléfono si continúa con su ritmo actual.
 * 
 * Esta pantalla usa psicología de pérdida (Loss Aversion) para convertir usuarios.
 */
@Composable
fun ShockOnboardingScreen(
    usageStatsRepository: UsageStatsRepository,
    userBirthYear: Int? = null, // Si no se tiene, asumir edad promedio
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(true) }
    var shockData by remember { mutableStateOf<ShockData?>(null) }
    
    val hasPermission = PermissionUtils.hasUsageStatsPermission(context)

    // Calcular datos de shock
    LaunchedEffect(Unit) {
        isVisible = true
        delay(500)
        
        if (hasPermission) {
            val dailyUsageMs = usageStatsRepository.getTotalScreenTime()
            val weeklyUsageMs = usageStatsRepository.getWeeklyUsageStats().sumOf { it.totalTimeInMillis }
            
            shockData = calculateShockData(
                dailyUsageMs = dailyUsageMs,
                weeklyUsageMs = weeklyUsageMs,
                birthYear = userBirthYear
            )
        } else {
            // Usar datos promedio si no hay permiso
            shockData = calculateShockData(
                dailyUsageMs = 4 * 60 * 60 * 1000L, // 4 horas promedio
                weeklyUsageMs = 28 * 60 * 60 * 1000L, // 28 horas por semana
                birthYear = userBirthYear
            )
        }
        
        isCalculating = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7F1D1D), // Rojo oscuro
                        Color(0xFF991B1B),
                        Color(0xFFB91C1C),
                        Color(0xFF450A0A)
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
            verticalArrangement = Arrangement.Center
        ) {
            if (isCalculating) {
                // Estado de carga
                LoadingState()
            } else {
                shockData?.let { data ->
                    ShockContent(
                        data = data,
                        isVisible = isVisible,
                        hasRealData = hasPermission,
                        onContinue = onContinue,
                        onSkip = onSkip
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Analizando tu uso del teléfono...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun ShockContent(
    data: ShockData,
    isVisible: Boolean,
    hasRealData: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    // Animación de número contando
    var displayedYears by remember { mutableStateOf(0f) }
    
    LaunchedEffect(data.yearsOnPhone) {
        val targetValue = data.yearsOnPhone
        val duration = 2000L
        val steps = 60
        val stepDuration = duration / steps
        val increment = targetValue / steps
        
        repeat(steps) {
            delay(stepDuration)
            displayedYears += increment
        }
        displayedYears = targetValue
    }

    // Animación de pulso para el número principal
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Emoji de alarma
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.5f)
        ) {
            Text(
                text = "⚠️",
                fontSize = 80.sp
            )
        }

        // Título impactante
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 300)) +
                    slideInVertically(initialOffsetY = { -30 })
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LA VERDAD INCÓMODA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
                Text(
                    text = if (hasRealData) "basada en TU uso real" else "basada en promedios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Número principal de SHOCK - Años en el teléfono
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                    scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.4f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pasarás",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                // Número gigante animado
                Text(
                    text = String.format("%.1f", displayedYears),
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.scale(pulse)
                )
                
                Text(
                    text = "AÑOS",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    letterSpacing = 8.sp
                )
                
                Text(
                    text = "de tu vida mirando una pantalla",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats adicionales de shock
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 1200)) +
                    slideInVertically(initialOffsetY = { 40 })
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShockStatRow(
                        emoji = "📅",
                        value = "${data.daysOnPhone.toInt()}",
                        unit = "días completos",
                        description = "24 horas pegado a la pantalla"
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    
                    ShockStatRow(
                        emoji = "🎬",
                        value = "${data.moviesEquivalent.toInt()}",
                        unit = "películas",
                        description = "de 2 horas que podrías ver"
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    
                    ShockStatRow(
                        emoji = "📚",
                        value = "${data.booksEquivalent.toInt()}",
                        unit = "libros",
                        description = "que podrías leer"
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    
                    ShockStatRow(
                        emoji = "🌍",
                        value = "${data.travelDaysEquivalent.toInt()}",
                        unit = "viajes",
                        description = "de 2 semanas que podrías hacer"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mensaje emocional
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 1500))
        ) {
            Text(
                text = "\"La vida es demasiado corta para pasarla haciendo scroll.\"",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botones
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 1800)) +
                    slideInVertically(initialOffsetY = { 40 })
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C)
                        )
                        Text(
                            text = "Quiero cambiar esto 💪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Prefiero ignorar la realidad...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShockStatRow(
    emoji: String,
    value: String,
    unit: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(emoji, fontSize = 32.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Datos calculados para la pantalla de shock
 */
private data class ShockData(
    val dailyHours: Float,
    val yearsOnPhone: Float,
    val daysOnPhone: Float,
    val moviesEquivalent: Float,
    val booksEquivalent: Float,
    val travelDaysEquivalent: Float,
    val percentageOfWakingLife: Float
)

/**
 * Calcula las proyecciones de tiempo basadas en el uso actual
 */
private fun calculateShockData(
    dailyUsageMs: Long,
    weeklyUsageMs: Long,
    birthYear: Int?
): ShockData {
    // Convertir a horas diarias
    val dailyHours = if (weeklyUsageMs > 0) {
        (weeklyUsageMs / 7.0 / 1000 / 60 / 60).toFloat()
    } else {
        (dailyUsageMs / 1000.0 / 60 / 60).toFloat()
    }
    
    // Calcular años restantes de vida (hasta 80 años)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val estimatedAge = if (birthYear != null) {
        currentYear - birthYear
    } else {
        30 // Edad promedio estimada
    }
    val yearsRemaining = maxOf(0, 80 - estimatedAge)
    
    // Calcular tiempo total en el teléfono
    val hoursPerYear = dailyHours * 365
    val totalHoursRemaining = hoursPerYear * yearsRemaining
    val yearsOnPhone = totalHoursRemaining / (24 * 365) // Años completos (24h/día)
    val daysOnPhone = totalHoursRemaining / 24
    
    // Equivalencias impactantes
    val moviesEquivalent = totalHoursRemaining / 2 // Películas de 2 horas
    val booksEquivalent = totalHoursRemaining / 10 // ~10 horas por libro
    val travelDaysEquivalent = daysOnPhone / 14 // Viajes de 2 semanas
    
    // Porcentaje de vida despierto (16 horas)
    val percentageOfWakingLife = (dailyHours / 16) * 100
    
    return ShockData(
        dailyHours = dailyHours,
        yearsOnPhone = yearsOnPhone.toFloat(),
        daysOnPhone = daysOnPhone.toFloat(),
        moviesEquivalent = moviesEquivalent.toFloat(),
        booksEquivalent = booksEquivalent.toFloat(),
        travelDaysEquivalent = travelDaysEquivalent.toFloat(),
        percentageOfWakingLife = percentageOfWakingLife.toFloat()
    )
}

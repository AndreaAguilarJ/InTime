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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.R
import com.momentummm.app.data.repository.UsageStatsRepository
import com.momentummm.app.util.PermissionUtils
import kotlinx.coroutines.delay
import java.util.Calendar
import com.momentummm.app.ui.theme.*

/**
 * Pantalla de "Shock" Onboarding - "REALIDAD MORTAL"
 * Muestra una proyección alarmante del tiempo que el usuario pasará en su teléfono.
 * 
 * FÓRMULA: (TiempoDiarioHoras * 365 * AñosRestantesDeVida) / 24 = Años perdidos en el teléfono
 * 
 * Esta pantalla usa psicología de pérdida (Loss Aversion) para conversión inmediata.
 */
@Composable
fun ShockOnboardingScreen(
    usageStatsRepository: UsageStatsRepository,
    userBirthYear: Int? = null,
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
                        Rose900,
                        Rose800,
                        Rose600,
                        DangerVeilDeep
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
            text = stringResource(R.string.shock_analyzing),
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
        val duration = 2500L
        val steps = 80
        val stepDuration = duration / steps
        val increment = targetValue / steps
        
        repeat(steps) {
            delay(stepDuration)
            displayedYears += increment
        }
        displayedYears = targetValue
    }

    // Animación de pulso para el número principal (efecto de latido cardíaco)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Animación de resplandor rojo
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Emoji de alarma con animación
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.5f)
        ) {
            Text(
                text = "💀",
                fontSize = 72.sp
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
                    text = stringResource(R.string.shock_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(if (hasRealData) R.string.shock_subtitle_real else R.string.shock_subtitle_average),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================
        // NÚMERO PRINCIPAL DE SHOCK - AÑOS EN EL TELÉFONO
        // ========================================
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) +
                    scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.4f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Texto introductorio
                Text(
                    text = stringResource(R.string.shock_at_current_rate),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // ========================================
                // NÚMERO GIGANTE EN ROJO (colorScheme.error)
                // ========================================
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(pulse)
                ) {
                    // Glow effect detrás del número
                    Text(
                        text = String.format("%.1f", displayedYears),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error.copy(alpha = glowAlpha * 0.3f),
                        modifier = Modifier.scale(1.1f)
                    )
                    
                    // Número principal
                    Text(
                        text = String.format("%.1f", displayedYears),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error // ROJO DE ERROR
                    )
                }
                
                // "AÑOS" en dorado
                Text(
                    text = stringResource(R.string.shock_years),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber400,
                    letterSpacing = 10.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // ========================================
                // TEXTO PRINCIPAL DE IMPACTO
                // ========================================
                Text(
                    text = stringResource(R.string.shock_of_your_life),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                // Detalle adicional
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.shock_full_days_nonstop, String.format("%.0f", data.daysOnPhone)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats adicionales de shock
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 1200)) +
                    slideInVertically(initialOffsetY = { 40 })
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Información del cálculo
                    Text(
                        text = stringResource(R.string.shock_projection_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    
                    ShockStatRow(
                        emoji = "⏰",
                        value = String.format("%.1f", data.dailyHours),
                        unit = stringResource(R.string.shock_unit_hours_per_day),
                        description = stringResource(R.string.shock_desc_daily_average)
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    
                    ShockStatRow(
                        emoji = "📅",
                        value = "${data.daysOnPhone.toInt()}",
                        unit = stringResource(R.string.shock_unit_full_days),
                        description = stringResource(R.string.shock_desc_24h_nonstop)
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    
                    ShockStatRow(
                        emoji = "📚",
                        value = "${data.booksEquivalent.toInt()}",
                        unit = stringResource(R.string.shock_unit_books),
                        description = stringResource(R.string.shock_desc_books)
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    
                    ShockStatRow(
                        emoji = "🌍",
                        value = "${data.travelDaysEquivalent.toInt()}",
                        unit = stringResource(R.string.shock_unit_trips),
                        description = stringResource(R.string.shock_desc_trips)
                    )
                    
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    
                    // Porcentaje de vida despierta - MUY IMPACTANTE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⚡", fontSize = 28.sp)
                        Column {
                            Text(
                                text = stringResource(R.string.shock_waking_life_percent, String.format("%.0f", data.percentageOfWakingLife)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.shock_waking_life_detail),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mensaje emocional
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 1500))
        ) {
            Text(
                text = stringResource(R.string.shock_quote),
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
                            tint = Rose600
                        )
                        Text(
                            text = stringResource(R.string.shock_cta),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Rose600
                        )
                    }
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.shock_skip),
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
                    color = Amber400
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
    val yearsOnPhone: Float,       // FÓRMULA PRINCIPAL: (dailyHours * 365 * yearsRemaining) / 24
    val daysOnPhone: Float,        // Días completos (24h) en el teléfono
    val hoursPerYear: Float,       // Horas por año en el teléfono
    val moviesEquivalent: Float,   // Películas de 2h que podrías ver
    val booksEquivalent: Float,    // Libros que podrías leer (~10h/libro)
    val travelDaysEquivalent: Float, // Viajes de 2 semanas
    val percentageOfWakingLife: Float, // % de vida despierta en el teléfono
    val estimatedAge: Int,         // Edad estimada del usuario
    val yearsRemaining: Int        // Años restantes estimados
)

/**
 * Calcula las proyecciones de tiempo basadas en el uso actual.
 * 
 * FÓRMULA PRINCIPAL: 
 *   yearsOnPhone = (TiempoDiarioHoras * 365 * AñosRestantesDeVida) / 24
 * 
 * Esto calcula cuántos AÑOS COMPLETOS (24h/día) pasará el usuario
 * mirando su teléfono durante el resto de su vida.
 */
private fun calculateShockData(
    dailyUsageMs: Long,
    weeklyUsageMs: Long,
    birthYear: Int?
): ShockData {
    // Convertir a horas diarias (promedio de semana si está disponible)
    val dailyHours = if (weeklyUsageMs > 0) {
        (weeklyUsageMs / 7.0 / 1000.0 / 60.0 / 60.0).toFloat()
    } else {
        (dailyUsageMs / 1000.0 / 60.0 / 60.0).toFloat()
    }
    
    // Calcular años restantes de vida (expectativa de vida: 80 años)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val estimatedAge = if (birthYear != null && birthYear > 1900 && birthYear < currentYear) {
        currentYear - birthYear
    } else {
        28 // Edad promedio de usuarios de apps de productividad
    }
    val yearsRemaining = maxOf(1, 80 - estimatedAge)
    
    // ========================================
    // FÓRMULA PRINCIPAL: AÑOS EN EL TELÉFONO
    // ========================================
    // (HorasDiarias * 365 días * AñosRestantes) / 24 horas = Años completos
    val totalHoursRemaining = dailyHours * 365f * yearsRemaining
    val yearsOnPhone = totalHoursRemaining / (24f * 365f) // Años completos (24h/día, 365 días)
    
    // Cálculos adicionales para impacto
    val daysOnPhone = totalHoursRemaining / 24f // Días de 24 horas
    val hoursPerYear = dailyHours * 365f
    
    // Equivalencias impactantes
    val moviesEquivalent = totalHoursRemaining / 2f // Películas de 2 horas
    val booksEquivalent = totalHoursRemaining / 10f // ~10 horas por libro
    val travelDaysEquivalent = daysOnPhone / 14f // Viajes de 2 semanas
    
    // Porcentaje de vida despierto (16 horas despierto al día)
    val percentageOfWakingLife = (dailyHours / 16f) * 100f
    
    return ShockData(
        dailyHours = dailyHours,
        yearsOnPhone = yearsOnPhone,
        daysOnPhone = daysOnPhone,
        hoursPerYear = hoursPerYear,
        moviesEquivalent = moviesEquivalent,
        booksEquivalent = booksEquivalent,
        travelDaysEquivalent = travelDaysEquivalent,
        percentageOfWakingLife = percentageOfWakingLife,
        estimatedAge = estimatedAge,
        yearsRemaining = yearsRemaining
    )
}

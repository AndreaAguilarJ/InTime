package com.momentummm.shared.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.ui.theme.momentum
import com.momentummm.shared.focus.FocusPhase
import com.momentummm.shared.focus.FocusTimer
import com.momentummm.shared.focus.FocusTimerState
import kotlinx.coroutines.delay

/** Duraciones ofrecidas de entrada, en minutos de enfoque y de descanso. */
private val PRESETS = listOf(
    "Pomodoro" to (25 to 5),
    "Sesión larga" to (50 to 10),
    "Sprint corto" to (15 to 3),
)

/**
 * Pantalla de enfoque compartida entre Android e iOS.
 *
 * Toda la lógica está en [FocusTimer], que se comprueba con pruebas sin teléfono;
 * aquí solo se dibuja el estado y se avanza el reloj una vez por segundo.
 */
@Composable
fun FocusSharedScreen(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(FocusTimerState()) }

    // Un único bucle atado a la fase: al pausar, la condición deja de cumplirse y
    // la corrutina termina, de modo que no queda un ticker vivo consumiendo
    // batería detrás de una pantalla pausada.
    LaunchedEffect(state.phase) {
        while (state.isTicking) {
            delay(1000)
            state = FocusTimer.tick(state)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enfoque",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.momentum.textPrimary,
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = state.statusLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = state.statusColor(),
        )

        Spacer(Modifier.height(28.dp))
        CountdownRing(
            progress = state.progress,
            label = FocusTimer.formatCountdown(state.remainingSeconds),
            caption = state.sessionName,
            ringColor = state.statusColor(),
            trackColor = MaterialTheme.momentum.surfaceSunken,
            textColor = MaterialTheme.momentum.textPrimary,
            captionColor = MaterialTheme.momentum.textSecondary,
        )

        Spacer(Modifier.height(32.dp))

        when (state.phase) {
            FocusPhase.IDLE -> PresetList(onPick = { nombre, enfoque, descanso ->
                state = FocusTimer.start(nombre, enfoque, descanso)
            })

            FocusPhase.RUNNING, FocusPhase.BREAK -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { state = FocusTimer.pause(state) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Pausar") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { state = FocusTimer.stop() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.onBreak) "Saltar descanso" else "Terminar sesión") }
            }

            FocusPhase.PAUSED -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { state = FocusTimer.resume(state) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Reanudar") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { state = FocusTimer.stop() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Terminar sesión") }
            }

            FocusPhase.COMPLETED -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Sesión completada: ${state.creditedMinutes} min de enfoque.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                if (state.hasBreak) {
                    Button(
                        onClick = { state = FocusTimer.startBreak(state) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Empezar descanso de ${state.breakMinutes} min") }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = { state = FocusTimer.skipBreak(state) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.hasBreak) "Saltar descanso" else "Cerrar") }
            }
        }
    }
}

@Composable
private fun PresetList(onPick: (String, Int, Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PRESETS.forEach { (nombre, tiempos) ->
            val (enfoque, descanso) = tiempos
            Button(
                onClick = { onPick(nombre, enfoque, descanso) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.momentum.surfaceSunken,
                    contentColor = MaterialTheme.momentum.textPrimary,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$enfoque min · $descanso min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.momentum.textSecondary,
                    )
                }
            }
        }
    }
}

/** Anillo de progreso dibujado a mano: sin dependencias de iconos ni de recursos. */
@Composable
private fun CountdownRing(
    progress: Float,
    label: String,
    caption: String?,
    ringColor: Color,
    trackColor: Color,
    textColor: Color,
    captionColor: Color,
) {
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 14.dp.toPx()
            val margen = grosor / 2
            val medida = Size(size.width - grosor, size.height - grosor)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(margen, margen),
                size = medida,
                style = Stroke(width = grosor, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(margen, margen),
                    size = medida,
                    style = Stroke(width = grosor, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.displaySmall,
                color = textColor,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = captionColor,
                )
            }
        }
    }
}

@Composable
private fun FocusTimerState.statusLabel(): String = when (phase) {
    FocusPhase.IDLE -> "Elige cuánto quieres concentrarte"
    FocusPhase.RUNNING -> "En marcha"
    FocusPhase.PAUSED -> if (onBreak) "Descanso en pausa" else "En pausa"
    FocusPhase.COMPLETED -> "Completada"
    FocusPhase.BREAK -> "Descanso"
}

// El color porta significado y no decora: verde en marcha, ámbar en pausa, azul
// en descanso e índigo al completar. Es el mismo criterio que la app Android.
@Composable
private fun FocusTimerState.statusColor(): Color = when (phase) {
    FocusPhase.IDLE -> MaterialTheme.momentum.textSecondary
    FocusPhase.RUNNING -> MaterialTheme.momentum.success
    FocusPhase.PAUSED -> MaterialTheme.momentum.warning
    FocusPhase.COMPLETED -> MaterialTheme.colorScheme.primary
    FocusPhase.BREAK -> MaterialTheme.momentum.info
}

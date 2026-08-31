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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.momentummm.shared.data.SettingsKeys
import com.momentummm.shared.data.SettingsStore
import com.momentummm.shared.focus.FocusPhase
import com.momentummm.shared.focus.FocusTimer
import com.momentummm.shared.focus.FocusTimerState
import com.momentummm.shared.notification.MomentumNotifier
import com.momentummm.shared.notification.NotificationIds
import com.momentummm.shared.resources.Res
import com.momentummm.shared.resources.action_start
import com.momentummm.shared.resources.focus_close
import com.momentummm.shared.resources.focus_completed_detail
import com.momentummm.shared.resources.focus_custom_open
import com.momentummm.shared.resources.focus_end
import com.momentummm.shared.resources.focus_idle_hint
import com.momentummm.shared.resources.focus_pause
import com.momentummm.shared.resources.focus_preset_long
import com.momentummm.shared.resources.focus_preset_own
import com.momentummm.shared.resources.focus_preset_pomodoro
import com.momentummm.shared.resources.focus_preset_sprint
import com.momentummm.shared.resources.focus_preset_times
import com.momentummm.shared.resources.focus_resume
import com.momentummm.shared.resources.focus_skip_break
import com.momentummm.shared.resources.focus_start_break_action
import com.momentummm.shared.resources.focus_status_break
import com.momentummm.shared.resources.focus_status_done
import com.momentummm.shared.resources.focus_status_paused
import com.momentummm.shared.resources.focus_break_paused
import com.momentummm.shared.resources.focus_status_running
import com.momentummm.shared.resources.minutes_one
import com.momentummm.shared.resources.minutes_other
import com.momentummm.shared.resources.notif_break_done_body
import com.momentummm.shared.resources.notif_break_done_title
import com.momentummm.shared.resources.notif_break_resume_body
import com.momentummm.shared.resources.notif_focus_done_body
import com.momentummm.shared.resources.notif_focus_done_title
import com.momentummm.shared.resources.tab_focus
import com.momentummm.shared.ui.component.CustomSessionDialog
import com.momentummm.shared.util.nowEpochSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Sesiones ofrecidas de entrada: nombre traducido, enfoque y descanso. */
private data class FocusPreset(
    val name: StringResource,
    val focusMinutes: Int,
    val breakMinutes: Int,
)

private val PRESETS = listOf(
    FocusPreset(Res.string.focus_preset_pomodoro, 25, 5),
    FocusPreset(Res.string.focus_preset_long, 50, 10),
    FocusPreset(Res.string.focus_preset_sprint, 15, 3),
)

/**
 * Pantalla de enfoque compartida entre Android e iOS.
 *
 * Toda la lógica está en [FocusTimer], que se comprueba con pruebas sin teléfono;
 * aquí solo se dibuja el estado, se corrige la cuenta atrás contra el reloj real y
 * se programan los avisos.
 */
@Composable
fun FocusSharedScreen(
    notifier: MomentumNotifier,
    store: SettingsStore,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(FocusTimerState()) }
    var customising by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // El bucle consulta el RELOJ en cada vuelta en vez de restar un segundo a
    // ciegas. Es lo que hace que la cuenta atrás sea correcta al volver del
    // segundo plano en iOS, donde el sistema congela la app: la corrutina se
    // reanuda y la primera vuelta ya pone el número donde debe estar.
    // Al pausar, la condición deja de cumplirse y la corrutina termina, así que no
    // queda un ticker vivo gastando batería detrás de una pantalla pausada.
    LaunchedEffect(state.phase) {
        while (state.isTicking) {
            state = FocusTimer.syncToClock(state, nowEpochSeconds())
            delay(1000)
        }
    }

    /**
     * Programa el aviso del intervalo.
     *
     * Los textos se resuelven con `getString` dentro de una corrutina y no con
     * `stringResource`, porque esto se llama desde un toque y no durante la
     * composición: es la vía correcta para leer un recurso fuera de la pantalla.
     */
    fun scheduleNotice(id: String, seconds: Int, title: StringResource, body: suspend () -> String) {
        if (seconds < 1) return
        scope.launch {
            notifier.scheduleAfter(
                id = id,
                seconds = seconds,
                title = getString(title),
                body = body(),
            )
        }
    }

    fun startSession(nameRes: StringResource, focus: Int, breakMinutes: Int) {
        val ahora = nowEpochSeconds()
        state = FocusTimer.start(null, focus, breakMinutes, ahora).copy(
            // El nombre se guarda ya traducido para que la cuenta atrás lo muestre
            // sin volver a resolver el recurso en cada recomposición.
            sessionName = null,
        )
        scope.launch {
            val nombre = getString(nameRes)
            state = state.copy(sessionName = nombre)
        }
        // El aviso se programa AHORA para el final de la sesión: si se programara
        // al terminar, en iOS la app ya estaría suspendida y nunca se ejecutaría.
        scheduleNotice(
            id = NotificationIds.FOCUS_COMPLETE,
            seconds = focus * 60,
            title = Res.string.notif_focus_done_title,
        ) {
            getString(Res.string.notif_focus_done_body, minutesText(focus), getString(nameRes))
        }
    }

    fun endSession() {
        state = FocusTimer.stop()
        notifier.cancel(NotificationIds.FOCUS_COMPLETE)
        notifier.cancel(NotificationIds.BREAK_COMPLETE)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.tab_focus),
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
            FocusPhase.IDLE -> Column(modifier = Modifier.fillMaxWidth()) {
                PresetList(onPick = ::startSession)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { customising = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.focus_custom_open)) }
            }

            FocusPhase.RUNNING, FocusPhase.BREAK -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        val enDescanso = state.onBreak
                        state = FocusTimer.pause(state)
                        // Pausado, el aviso programado ya no corresponde a nada.
                        notifier.cancel(
                            if (enDescanso) NotificationIds.BREAK_COMPLETE
                            else NotificationIds.FOCUS_COMPLETE
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.focus_pause)) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = ::endSession,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.onBreak) Res.string.focus_skip_break else Res.string.focus_end
                        )
                    )
                }
            }

            FocusPhase.PAUSED -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        val reanudada = FocusTimer.resume(state, nowEpochSeconds())
                        state = reanudada
                        if (reanudada.onBreak) {
                            scheduleNotice(
                                id = NotificationIds.BREAK_COMPLETE,
                                seconds = reanudada.remainingSeconds,
                                title = Res.string.notif_break_done_title,
                            ) { getString(Res.string.notif_break_resume_body) }
                        } else {
                            scheduleNotice(
                                id = NotificationIds.FOCUS_COMPLETE,
                                seconds = reanudada.remainingSeconds,
                                title = Res.string.notif_focus_done_title,
                            ) {
                                getString(
                                    Res.string.notif_focus_done_body,
                                    minutesText(reanudada.remainingSeconds / 60),
                                    reanudada.sessionName.orEmpty(),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.focus_resume)) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = ::endSession,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.focus_end)) }
            }

            FocusPhase.COMPLETED -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        Res.string.focus_completed_detail,
                        minutesLabel(state.creditedMinutes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                if (state.hasBreak) {
                    val minutosDescanso = state.breakMinutes
                    Button(
                        onClick = {
                            state = FocusTimer.startBreak(state, nowEpochSeconds())
                            scheduleNotice(
                                id = NotificationIds.BREAK_COMPLETE,
                                seconds = minutosDescanso * 60,
                                title = Res.string.notif_break_done_title,
                            ) {
                                getString(
                                    Res.string.notif_break_done_body,
                                    minutesText(minutosDescanso),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(Res.string.focus_start_break_action, minutosDescanso)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = {
                        state = FocusTimer.skipBreak(state)
                        notifier.cancel(NotificationIds.BREAK_COMPLETE)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.hasBreak) Res.string.focus_skip_break
                            else Res.string.focus_close
                        )
                    )
                }
            }
        }
    }

    if (customising) {
        CustomSessionDialog(
            initialFocusMinutes = store.getLongOrNull(SettingsKeys.CUSTOM_FOCUS_MINUTES)?.toInt() ?: 25,
            initialBreakMinutes = store.getLongOrNull(SettingsKeys.CUSTOM_BREAK_MINUTES)?.toInt() ?: 5,
            onDismiss = { customising = false },
            onConfirm = { enfoque, descanso ->
                // Se recuerda la elección: quien siempre trabaja en bloques de 40
                // minutos no debería repetirla cada vez.
                store.putLong(SettingsKeys.CUSTOM_FOCUS_MINUTES, enfoque.toLong())
                store.putLong(SettingsKeys.CUSTOM_BREAK_MINUTES, descanso.toLong())
                customising = false
                startSession(Res.string.focus_preset_own, enfoque, descanso)
            },
        )
    }
}

@Composable
private fun PresetList(onPick: (StringResource, Int, Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PRESETS.forEach { preset ->
            Button(
                onClick = { onPick(preset.name, preset.focusMinutes, preset.breakMinutes) },
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
                    Text(
                        text = stringResource(preset.name),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            Res.string.focus_preset_times,
                            preset.focusMinutes,
                            preset.breakMinutes,
                        ),
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
private fun FocusTimerState.statusLabel(): String = stringResource(
    when (phase) {
        FocusPhase.IDLE -> Res.string.focus_idle_hint
        FocusPhase.RUNNING -> Res.string.focus_status_running
        FocusPhase.PAUSED -> if (onBreak) Res.string.focus_break_paused else Res.string.focus_status_paused
        FocusPhase.COMPLETED -> Res.string.focus_status_done
        FocusPhase.BREAK -> Res.string.focus_status_break
    }
)

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

/**
 * Duración con la concordancia de número correcta, dentro de la composición.
 *
 * Se resuelve con dos textos distintos y no componiendo la cifra a mano: eso es
 * lo que producía «1 minutos» en la notificación, y en alemán o francés la regla
 * no es la misma que en español.
 */
@Composable
private fun minutesLabel(amount: Int): String =
    if (amount == 1) stringResource(Res.string.minutes_one)
    else stringResource(Res.string.minutes_other, amount)

/** La misma concordancia, fuera de la composición (para los avisos). */
private suspend fun minutesText(amount: Int): String =
    if (amount == 1) getString(Res.string.minutes_one)
    else getString(Res.string.minutes_other, amount)

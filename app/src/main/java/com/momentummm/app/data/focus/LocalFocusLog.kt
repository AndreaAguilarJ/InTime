package com.momentummm.app.data.focus

import android.content.Context
import com.momentummm.app.data.appwrite.models.AppwriteFocusSession
import com.momentummm.app.data.appwrite.models.FocusSessionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Cálculos puros sobre las sesiones de enfoque guardadas en el teléfono.
 *
 * Sin cuenta, Enfoque guardaba cada sesión solo en Appwrite, así que el
 * contador del día, el tiempo total, la racha y el historial se quedaban
 * siempre en cero. Las reglas copian las del servidor
 * (AppwriteFocusSessionRepository) para que ambos cuenten igual.
 */
object LocalFocusLog {

    /** Unos meses de uso diario; evita que el archivo crezca sin límite. */
    const val MAX_SESSIONS = 500

    fun stats(sessions: List<AppwriteFocusSession>, today: LocalDate): FocusSessionStats {
        val todayIso = today.toString()
        val completedToday = sessions.filter { it.date == todayIso && it.wasCompleted }
        return FocusSessionStats(
            completedToday = completedToday.size,
            totalFocusTimeToday = completedToday.sumOf { it.actualDuration },
            streakDays = streak(sessions, today)
        )
    }

    /** Días seguidos con al menos una sesión completada, contando desde hoy. */
    private fun streak(sessions: List<AppwriteFocusSession>, today: LocalDate): Int {
        val days = sessions.filter { it.wasCompleted }.map { it.date }.toSet()
        var streak = 0
        var day = today
        while (streak < 365 && days.contains(day.toString())) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** La más reciente primero, como el historial del servidor. */
    fun history(sessions: List<AppwriteFocusSession>, limit: Int): List<AppwriteFocusSession> =
        sessions.asReversed().take(limit)

    fun append(sessions: List<AppwriteFocusSession>, session: AppwriteFocusSession): List<AppwriteFocusSession> {
        if (sessions.any { it.sessionId == session.sessionId }) return sessions
        return (sessions + session).takeLast(MAX_SESSIONS)
    }
}

/** Guarda en el teléfono las sesiones de enfoque terminadas. */
class LocalFocusSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val serializer = ListSerializer(AppwriteFocusSession.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    private val _sessions = MutableStateFlow(load())
    val sessions: StateFlow<List<AppwriteFocusSession>> = _sessions.asStateFlow()

    private fun load(): List<AppwriteFocusSession> =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            ?: emptyList()

    @Synchronized
    fun save(session: AppwriteFocusSession) {
        val updated = LocalFocusLog.append(_sessions.value, session)
        prefs.edit().putString(KEY, json.encodeToString(serializer, updated)).apply()
        _sessions.value = updated
    }

    private companion object {
        const val PREFS = "local_focus_sessions"
        const val KEY = "sessions"
    }
}

package com.momentummm.app.data.focus

import com.momentummm.app.data.appwrite.models.AppwriteFocusSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Sin cuenta, Enfoque guardaba las sesiones solo en Appwrite: el contador del
 * día, el tiempo total, la racha y el historial se quedaban siempre en cero.
 * El registro local los calcula a partir de lo guardado en el teléfono.
 */
class LocalFocusLogTest {

    private val today = LocalDate.of(2026, 11, 13)

    private fun session(date: String, minutes: Int, completed: Boolean = true, id: String = "$date-$minutes") =
        AppwriteFocusSession(
            userId = "local",
            sessionId = id,
            sessionType = "pomodoro",
            date = date,
            plannedDuration = minutes,
            actualDuration = minutes,
            wasCompleted = completed
        )

    @Test
    fun `cuenta solo las sesiones completadas de hoy`() {
        val log = listOf(
            session("2026-11-13", 25),
            session("2026-11-13", 50),
            session("2026-11-13", 10, completed = false),
            session("2026-11-12", 25)
        )
        val stats = LocalFocusLog.stats(log, today)
        assertEquals(2, stats.completedToday)
        assertEquals(75, stats.totalFocusTimeToday)
    }

    @Test
    fun `la racha cuenta dias seguidos hasta hoy`() {
        val log = listOf(
            session("2026-11-13", 25),
            session("2026-11-12", 25),
            session("2026-11-11", 25),
            session("2026-11-09", 25)
        )
        assertEquals(3, LocalFocusLog.stats(log, today).streakDays)
    }

    @Test
    fun `sin sesion hoy la racha es cero, como en el servidor`() {
        val log = listOf(session("2026-11-12", 25), session("2026-11-11", 25))
        assertEquals(0, LocalFocusLog.stats(log, today).streakDays)
    }

    @Test
    fun `una sesion abandonada no mantiene la racha`() {
        val log = listOf(session("2026-11-13", 25), session("2026-11-12", 5, completed = false))
        assertEquals(1, LocalFocusLog.stats(log, today).streakDays)
    }

    @Test
    fun `el historial va de la mas reciente a la mas antigua y respeta el limite`() {
        val log = (1..12).map { session("2026-11-%02d".format(it), it, id = "s$it") }
        val history = LocalFocusLog.history(log, limit = 10)
        assertEquals(10, history.size)
        assertEquals("s12", history.first().sessionId)
        assertEquals("s3", history.last().sessionId)
    }

    @Test
    fun `al guardar se descartan las mas antiguas por encima del tope`() {
        val log = (1..LocalFocusLog.MAX_SESSIONS).map { session("2026-01-01", 1, id = "old$it") }
        val updated = LocalFocusLog.append(log, session("2026-11-13", 25, id = "new"))
        assertEquals(LocalFocusLog.MAX_SESSIONS, updated.size)
        assertEquals("new", updated.last().sessionId)
        assertEquals("old2", updated.first().sessionId)
    }

    @Test
    fun `guardar la misma sesion dos veces no la duplica`() {
        val once = LocalFocusLog.append(emptyList(), session("2026-11-13", 25, id = "a"))
        val twice = LocalFocusLog.append(once, session("2026-11-13", 25, id = "a"))
        assertEquals(1, twice.size)
    }
}

package com.momentummm.app.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de regresión del cálculo del score de adicción.
 *
 * Contexto del fallo que vigilan: `List<Int>.average()` sobre una lista vacía devuelve
 * NaN. SQLite no tiene NaN y lo almacena como NULL, y la columna
 * `addiction_scores.overallScore` es NOT NULL, así que la inserción fallaba con
 * SQLiteConstraintException para TODAS las apps analizadas en cualquier instalación
 * nueva. No se veía nada raro en pantalla: el análisis simplemente no guardaba nada.
 *
 * En una instalación nueva no hay historial, así que la lista vacía es el caso NORMAL,
 * no un caso borde.
 */
class AddictionScoreTest {

    @Test
    fun `un solo componente NaN no contamina el resultado`() {
        // Este era exactamente el fallo: resistanceScore salía NaN porque la media de
        // duraciones de sesión se calculaba sobre una lista vacía.
        val score = weightedAddictionScore(
            frequencyScore = 10f,
            durationScore = 20f,
            resistanceScore = Float.NaN,
            escalationScore = 0f,
            timingScore = 0f,
            compulsionScore = 0f
        )

        assertFalse("el score no debe ser NaN", score.isNaN())
        assertTrue("el score debe ser finito", score.isFinite())
        // 10*0.15 + 20*0.25 = 6.5; el componente NaN cuenta como 0.
        assertEquals(6.5f, score, 0.001f)
    }

    @Test
    fun `todos los componentes NaN dan cero, nunca NaN`() {
        val score = weightedAddictionScore(
            Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN
        )

        assertFalse(score.isNaN())
        assertEquals(0f, score, 0.0f)
    }

    @Test
    fun `instalacion nueva sin historial produce cero valido y guardable`() {
        // Todos los componentes a 0 es lo que produce una instalación nueva bien
        // protegida. Debe ser un valor guardable, no un nulo.
        val score = weightedAddictionScore(0f, 0f, 0f, 0f, 0f, 0f)

        assertTrue(score.isFinite())
        assertEquals(0f, score, 0.0f)
    }

    @Test
    fun `el infinito tambien se sanea`() {
        // Una división por cero da infinito, no NaN, y SQLite tampoco lo admite.
        val score = weightedAddictionScore(
            frequencyScore = Float.POSITIVE_INFINITY,
            durationScore = Float.NEGATIVE_INFINITY,
            resistanceScore = 0f,
            escalationScore = 0f,
            timingScore = 0f,
            compulsionScore = 0f
        )

        assertTrue(score.isFinite())
        assertEquals(0f, score, 0.0f)
    }

    @Test
    fun `el resultado se mantiene en el rango 0 a 100`() {
        val maximo = weightedAddictionScore(100f, 100f, 100f, 100f, 100f, 100f)
        assertEquals(100f, maximo, 0.001f)

        // Valores fuera de rango por un cálculo defectuoso no deben escaparse del tope.
        val desbordado = weightedAddictionScore(500f, 500f, 500f, 500f, 500f, 500f)
        assertEquals(100f, desbordado, 0.001f)

        val negativo = weightedAddictionScore(-50f, -50f, -50f, -50f, -50f, -50f)
        assertEquals(0f, negativo, 0.001f)
    }

    @Test
    fun `los pesos suman uno y se aplican en la proporcion correcta`() {
        // Si todos los componentes valen lo mismo, el resultado debe ser ese valor:
        // demuestra que los seis pesos suman exactamente 1.
        val score = weightedAddictionScore(40f, 40f, 40f, 40f, 40f, 40f)
        assertEquals(40f, score, 0.001f)

        // Y la duración pesa más que la frecuencia (0.25 frente a 0.15).
        val soloDuracion = weightedAddictionScore(0f, 100f, 0f, 0f, 0f, 0f)
        val soloFrecuencia = weightedAddictionScore(100f, 0f, 0f, 0f, 0f, 0f)
        assertTrue(
            "la duración debe pesar más que la frecuencia",
            soloDuracion > soloFrecuencia
        )
    }

    @Test
    fun `orZeroIfNotFinite deja pasar los valores normales`() {
        assertEquals(42.5f, 42.5f.orZeroIfNotFinite(), 0.0f)
        assertEquals(0f, 0f.orZeroIfNotFinite(), 0.0f)
        assertEquals(-3f, (-3f).orZeroIfNotFinite(), 0.0f)
    }
}

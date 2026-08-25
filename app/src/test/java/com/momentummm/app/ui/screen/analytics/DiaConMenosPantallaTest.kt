package com.momentummm.app.ui.screen.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas del insight de productividad.
 *
 * Vigilan lo que antes estaba roto: la tarjeta afirmaba "tu productividad aumenta un 23%
 * los martes" con el número y el día escritos a mano, iguales para todo el mundo. Lo que
 * estas pruebas protegen no es solo el cálculo, es la negativa a inventar: cuando no hay
 * datos suficientes la función devuelve null y la app lo reconoce.
 */
class DiaConMenosPantallaTest {

    private fun dia(nombre: String, horas: Float) =
        WeeklyData(day = nombre, screenTime = horas, pickups = 0, mostUsedApp = "-")

    @Test
    fun `sin datos no se inventa nada`() {
        assertNull(calcularDiaConMenosPantalla(emptyList()))
    }

    @Test
    fun `con menos de tres dias de uso no se afirma nada`() {
        // Una instalación nueva: comparar contra una media de dos días no significa nada.
        val datos = listOf(dia("Lunes", 5f), dia("Martes", 2f))
        assertNull(calcularDiaConMenosPantalla(datos))
    }

    @Test
    fun `los dias sin uso no cuentan como dias de concentracion`() {
        // Un día a cero es un día sin datos, no el mejor día del usuario. Si contara,
        // la app felicitaría al usuario por el día que tuvo el teléfono apagado.
        val datos = listOf(
            dia("Lunes", 5f),
            dia("Martes", 5f),
            dia("Miércoles", 5f),
            dia("Jueves", 0f)
        )
        val insight = calcularDiaConMenosPantalla(datos)
        // Los tres días con uso son idénticos: no hay diferencia que señalar, y desde
        // luego el resultado no puede ser el jueves.
        assertNull(insight)
    }

    @Test
    fun `identifica el dia mas bajo y su diferencia real`() {
        val datos = listOf(
            dia("Lunes", 6f),
            dia("Martes", 6f),
            dia("Miércoles", 6f),
            dia("Jueves", 2f)
        )
        val insight = calcularDiaConMenosPantalla(datos)

        assertNotNull("con cuatro días y una diferencia clara debe haber insight", insight)
        assertEquals("Jueves", insight!!.dia)
        // Media = 5, mínimo = 2 -> 60 % por debajo.
        assertEquals(60, insight.porcentajeBajoLaMedia)
    }

    @Test
    fun `una diferencia pequena se considera ruido y no genera consejo`() {
        // 5 % por debajo de la media no justifica decirle a nadie que reorganice su semana.
        val datos = listOf(
            dia("Lunes", 5.0f),
            dia("Martes", 5.0f),
            dia("Miércoles", 4.8f)
        )
        assertNull(calcularDiaConMenosPantalla(datos))
    }

    @Test
    fun `el porcentaje nunca llega al cien por cien`() {
        // Un día casi a cero daría 99,9 %: decir "100 % menos" sería absurdo.
        val datos = listOf(
            dia("Lunes", 10f),
            dia("Martes", 10f),
            dia("Miércoles", 0.001f)
        )
        val insight = calcularDiaConMenosPantalla(datos)
        assertNotNull(insight)
        assertEquals(99, insight!!.porcentajeBajoLaMedia)
    }

    @Test
    fun `los valores no finitos no producen un porcentaje invalido`() {
        // Misma lección que el fallo del score de adicción: un NaN colado en los datos no
        // debe convertirse en un texto roto en pantalla.
        val datos = listOf(
            dia("Lunes", Float.NaN),
            dia("Martes", 6f),
            dia("Miércoles", 6f),
            dia("Jueves", 2f)
        )
        val insight = calcularDiaConMenosPantalla(datos)
        assertNotNull("el NaN debe descartarse, no contaminar el cálculo", insight)
        assertEquals("Jueves", insight!!.dia)
    }
}

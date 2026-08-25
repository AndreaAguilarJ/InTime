package com.momentummm.app.ui.screen.analytics

import kotlin.math.roundToInt

/**
 * Día con menos tiempo de pantalla de la semana, y cuánto baja respecto a la media.
 *
 * @param dia nombre del día, ya localizado por quien produjo los datos.
 * @param porcentajeBajoLaMedia cuánto por debajo de la media está, en porcentaje entero.
 */
data class InsightDiaMenosPantalla(
    val dia: String,
    val porcentajeBajoLaMedia: Int
)

/**
 * Deriva del uso real cuál es el día con menos tiempo de pantalla.
 *
 * Sustituye a una frase fija ("tu productividad aumenta un 23% los martes") que no venía
 * de ningún dato y que se mostraba idéntica a todos los usuarios, incluido quien acababa
 * de instalar la app.
 *
 * Devuelve null a propósito cuando los datos no sostienen ninguna afirmación. Preferir
 * null a un número inventado es el punto entero de esta función: quien la llama muestra
 * entonces un texto que reconoce que aún no hay historial suficiente.
 *
 * Función pura para poder probarla sin Android ni Compose.
 */
fun calcularDiaConMenosPantalla(datos: List<WeeklyData>): InsightDiaMenosPantalla? {
    // Solo cuentan los días con uso real: un día a cero suele ser un día sin datos
    // (el teléfono apagado, la app recién instalada), no un día de concentración.
    val conUso = datos.filter { it.screenTime > 0f && it.screenTime.isFinite() }

    // Con menos de tres días, comparar contra "la media" no significa nada.
    if (conUso.size < 3) return null

    val media = conUso.map { it.screenTime.toDouble() }.average().toFloat()
    if (!media.isFinite() || media <= 0f) return null

    val minimo = conUso.minByOrNull { it.screenTime } ?: return null

    val diferencia = ((media - minimo.screenTime) / media) * 100f
    if (!diferencia.isFinite()) return null
    val porcentaje = diferencia.roundToInt()

    // Por debajo del 10 % la diferencia es ruido, no una señal que merezca un consejo.
    if (porcentaje < 10) return null

    return InsightDiaMenosPantalla(
        dia = minimo.day,
        porcentajeBajoLaMedia = porcentaje.coerceIn(10, 99)
    )
}

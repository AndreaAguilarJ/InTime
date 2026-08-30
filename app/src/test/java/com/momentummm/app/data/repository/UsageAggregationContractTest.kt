package com.momentummm.app.data.repository

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contratos estructurales para el cálculo de uso.
 *
 * La parte que consulta UsageStatsManager no puede ejecutarse en la JVM local sin
 * Robolectric. Estas pruebas protegen el algoritmo puro incrustado en el adaptador:
 * agrupar todos los buckets, sumarlos y elegir la fuente más conservadora.
 */
class UsageAggregationContractTest {

    @Test
    fun `el repositorio agrupa todos los buckets por paquete`() {
        val source = source("com/momentummm/app/data/repository/UsageStatsRepository.kt")

        assertTrue(source.contains(".groupBy { it.packageName }"))
        assertTrue(source.contains("statsList.sumOf { it.totalTimeInForeground }"))
        assertTrue(source.contains("statsList.maxOf { it.lastTimeUsed }"))
    }

    @Test
    fun `el total de pantalla suma todas las entradas positivas`() {
        val source = source("com/momentummm/app/data/repository/UsageStatsRepository.kt")
        val totalScreenTimeBody = source.substringAfter(
            "private fun getTotalScreenTime(startTime: Long, endTime: Long): Long"
        )

        assertTrue(totalScreenTimeBody.contains(".filter { it.totalTimeInForeground > 0 }"))
        assertTrue(totalScreenTimeBody.contains(".sumOf { it.totalTimeInForeground }"))
    }

    @Test
    fun `el calculador diario usa la mayor de eventos y estadisticas agregadas`() {
        val source = source("com/momentummm/app/data/usage/DailyUsageCalculator.kt")

        assertTrue(source.contains("return maxOf(netFromEvents, netFromStats)"))
        assertTrue(source.contains("?.sumOf { it.totalTimeInForeground }"))
    }

    @Test
    fun `la ventana excluida se resta de las dos fuentes antes de comparar`() {
        // Restarla sólo de los eventos no serviría de nada: `maxOf` volvería a
        // elegir el agregado, que sigue incluyendo el uso nocturno, y la
        // Ventana de sueño quedaría anulada en silencio.
        val source = source("com/momentummm/app/data/usage/DailyUsageCalculator.kt")

        assertTrue(
            source.contains("val netFromEvents = (events.totalMillis - events.excludedMillis)")
        )
        assertTrue(
            source.contains("val netFromStats = (fromStats - events.excludedMillis)")
        )
    }

    private fun source(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/$relativePath"),
            File("app/src/main/java/$relativePath")
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("No se encontró el fuente de producción: $relativePath")
    }
}

package com.momentummm.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * «Esperanza de vida» en Ajustes > Mi vida en semanas era un control que no hacía
 * nada: el valor vivía en un `remember` de la pantalla, no se guardaba, y la
 * pantalla Mi vida, el widget y el modo minimalista calculaban siempre con 80
 * años. Quien elegía 90 veía 90 en la previsualización y 80 en todo lo demás.
 */
class LifeExpectancyTest {

    @Test
    fun `sin valor guardado son 80 años`() {
        assertEquals(80, LifeExpectancy.sanitize(null))
    }

    @Test
    fun `un valor fuera del rango del control se ajusta al rango`() {
        assertEquals(LifeExpectancy.MIN_YEARS, LifeExpectancy.sanitize(5))
        assertEquals(LifeExpectancy.MAX_YEARS, LifeExpectancy.sanitize(500))
        assertEquals(92, LifeExpectancy.sanitize(92))
    }

    @Test
    fun `la cuadricula crece con la esperanza elegida`() {
        val birth = kotlinx.datetime.LocalDate(1990, 1, 1)
        val today = kotlinx.datetime.LocalDate(2026, 10, 27)
        val at80 = LifeWeeksMath.calculate(birth, today, LifeExpectancy.sanitize(null))
        val at90 = LifeWeeksMath.calculate(birth, today, LifeExpectancy.sanitize(90))
        assertEquals(80 * 52, at80.totalWeeks)
        assertEquals(90 * 52, at90.totalWeeks)
        assertEquals(at80.weeksLived, at90.weeksLived)
        assertTrue(at90.progressPercentage < at80.progressPercentage)
    }

    private val main: File = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull { File(it, "AndroidManifest.xml").isFile }
        ?: error("No se encontró src/main desde ${File(".").absolutePath}")

    @Test
    fun `nadie calcula la vida con los 80 años por defecto`() {
        // Cada pantalla y el widget deben pasar la esperanza guardada.
        val singleArg = Regex("""calculateLifeWeeks\(\s*[^,()]+(\([^()]*\))?\s*\)""")
        val offenders = File(main, "java").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                singleArg.findAll(file.readText()).map { "${file.name}: ${it.value}" }
            }
            .toList()
        assertEquals("Llamadas sin esperanza de vida", emptyList<String>(), offenders)
    }

    @Test
    fun `el control de ajustes usa el mismo rango que se guarda`() {
        val screen = File(main, "java/com/momentummm/app/ui/screen/settings/LifeWeeksSettingsScreen.kt").readText()
        assertTrue("el valor se lee de lo guardado", screen.contains("UserPreferencesRepository.getLifeExpectancy("))
        assertTrue("el valor se guarda", screen.contains("UserPreferencesRepository.setLifeExpectancy("))
        assertTrue(
            "rango del control",
            screen.contains("LifeExpectancy.MIN_YEARS.toFloat()..LifeExpectancy.MAX_YEARS.toFloat()"),
        )
    }
}

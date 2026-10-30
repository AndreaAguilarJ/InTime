package com.momentummm.app.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Política de la API AccessibilityService de Google Play: antes de pedir que
 * se active un servicio de accesibilidad, la app tiene que mostrar un aviso
 * destacado y obtener un «Aceptar» explícito. La 1.3.0 (16) se rechazó porque
 * tres botones abrían los ajustes de accesibilidad directamente.
 *
 * Estas guardas leen el código fuente y fallan si:
 *  - otro archivo vuelve a abrir los ajustes de accesibilidad sin pasar por
 *    `AccessibilityDisclosureDialog`;
 *  - falta algún texto del aviso en alguno de los cinco idiomas;
 *  - un servicio de accesibilidad pierde su nombre o su descripción propios,
 *    o deja de declarar que no es una herramienta de accesibilidad.
 */
class AccessibilityDisclosureGuardTest {

    private val appDir: File = listOf(File("."), File("app"))
        .firstOrNull { File(it, "src/main/AndroidManifest.xml").isFile }
        ?: error("No se encontró el módulo app desde ${File(".").absolutePath}")

    private val mainSources = File(appDir, "src/main/java")
    private val res = File(appDir, "src/main/res")

    @Test
    fun `solo el aviso destacado abre los ajustes de accesibilidad`() {
        val openers = mainSources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                file.readLines().any { line ->
                    val code = line.substringBefore("//").trim()
                    !code.startsWith("*") &&
                        (code.contains("ACTION_ACCESSIBILITY_SETTINGS") ||
                            code.contains("android.settings.ACCESSIBILITY_SETTINGS"))
                }
            }
            .map { it.name }
            .toList()
        assertEquals(
            "Solo AccessibilityDisclosureDialog puede abrir los ajustes de accesibilidad " +
                "(Google Play exige el aviso destacado antes). Lo abren: $openers",
            listOf(DIALOG_FILE),
            openers
        )
    }

    @Test
    fun `el aviso abre los ajustes solo desde el boton de aceptar`() {
        val dialog = mainSources.walkTopDown().first { it.name == DIALOG_FILE }.readText()
        assertTrue(
            "openAccessibilitySettings debe ser privada para que nadie salte el aviso",
            dialog.contains("private fun openAccessibilitySettings")
        )
        val confirm = dialog.substringAfter("confirmButton").substringBefore("dismissButton")
        assertTrue(
            "Solo «Aceptar y continuar» puede abrir los ajustes",
            confirm.contains("openAccessibilitySettings(context)")
        )
        val rest = dialog.substringBefore("confirmButton") +
            dialog.substringAfter("dismissButton").substringBefore("private fun openAccessibilitySettings")
        assertTrue(
            "Cerrar o rechazar el aviso no puede abrir los ajustes",
            !rest.contains("openAccessibilitySettings(context)")
        )
    }

    @Test
    fun `el aviso esta completo en los cinco idiomas`() {
        val reference = keysIn(File(res, "values/$STRINGS_FILE"))
        assertTrue("Faltan textos del aviso: $REQUIRED_KEYS", reference.containsAll(REQUIRED_KEYS))
        LOCALES.forEach { locale ->
            val file = File(res, "values-$locale/$STRINGS_FILE")
            assertTrue("Falta $file", file.isFile)
            val missing = reference - keysIn(file)
            assertTrue("values-$locale no traduce $missing", missing.isEmpty())
        }
    }

    @Test
    fun `cada servicio de accesibilidad tiene nombre y descripcion propios`() {
        val manifest = File(appDir, "src/main/AndroidManifest.xml").readText()
        val services = Regex("""<service\b[^>]*BIND_ACCESSIBILITY_SERVICE[^>]*>""")
            .findAll(manifest).map { it.value }.toList()
        assertEquals("Se esperaban dos servicios de accesibilidad", 2, services.size)
        services.forEach { tag ->
            assertTrue("Servicio sin android:label propio: $tag", Regex("""android:label="@string/a11y_service_label_""").containsMatchIn(tag))
        }
        listOf("accessibility_service_config.xml", "website_block_accessibility_service.xml").forEach { name ->
            val config = File(res, "xml/$name").readText()
            assertTrue("$name usa el nombre de la app como descripción", !config.contains("""android:description="@string/app_name""""))
            assertTrue("$name debe declarar isAccessibilityTool=false", config.contains("""android:isAccessibilityTool="false""""))
        }
    }

    private fun keysIn(file: File): Set<String> =
        Regex("""<string name="([^"]+)"""").findAll(file.readText()).map { it.groupValues[1] }.toSet()

    private companion object {
        const val DIALOG_FILE = "AccessibilityDisclosureDialog.kt"
        const val STRINGS_FILE = "strings_accessibility_disclosure.xml"
        val LOCALES = listOf("en", "fr", "de", "pt")
        val REQUIRED_KEYS = setOf(
            "a11y_disclosure_title",
            "a11y_disclosure_intro",
            "a11y_disclosure_reads_app",
            "a11y_disclosure_reads_content",
            "a11y_disclosure_reads_url",
            "a11y_disclosure_use_rules",
            "a11y_disclosure_use_private",
            "a11y_disclosure_accept",
            "a11y_disclosure_decline"
        )
    }
}

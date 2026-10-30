package com.momentummm.app.build

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Requisitos de Google Play que, si se incumplen, bloquean publicar
 * actualizaciones a partir del 1 nov 2026:
 *
 *  - Biblioteca de Facturación de Google Play 8.0.0 o posterior.
 *  - `targetSdk` de Android 16 (API 36) o posterior.
 *
 * Se LEE el texto de `app/build.gradle.kts` en vez de fiarse de BuildConfig:
 * así la guarda también vigila la versión de la dependencia, que no llega a
 * ninguna constante en tiempo de ejecución, y se pone en rojo en cuanto alguien
 * baja una cifra «para que compile».
 */
class PlayPolicyGuardTest {

    private val buildFile: File = listOf(File("build.gradle.kts"), File("app/build.gradle.kts"))
        .firstOrNull { it.isFile && it.readText().contains("com.android.application") }
        ?: error("No se encontró app/build.gradle.kts desde ${File(".").absolutePath}")

    private val script = buildFile.readText()

    private fun intSetting(name: String): Int {
        val match = Regex("""\b$name\s*=\s*(\d+)""").find(script)
            ?: error("$name no aparece en ${buildFile.path}")
        return match.groupValues[1].toInt()
    }

    @Test
    fun `targetSdk es Android 16 o posterior`() {
        val target = intSetting("targetSdk")
        assertTrue("targetSdk=$target: Play exige 36 o más desde el 1 nov 2026", target >= MIN_TARGET_SDK)
    }

    @Test
    fun `compileSdk no es menor que targetSdk`() {
        assertTrue(intSetting("compileSdk") >= intSetting("targetSdk"))
    }

    @Test
    fun `todas las dependencias de facturacion son 8 o posterior`() {
        val billing = Regex("""com\.android\.billingclient:([\w-]+):(\d+)\.(\d+)\.(\d+)""")
            .findAll(script).toList()
        if (billing.isEmpty()) fail("No se encontró la dependencia de Google Play Billing")
        billing.forEach { match ->
            val (artifact, major) = match.destructured.let { it.component1() to it.component2().toInt() }
            assertTrue(
                "$artifact:${match.groupValues[2]}.${match.groupValues[3]}.${match.groupValues[4]} " +
                    "— Play exige Billing $MIN_BILLING_MAJOR.0.0 o posterior",
                major >= MIN_BILLING_MAJOR
            )
        }
    }

    @Test
    fun `versionCode sube por encima de la ultima version publicada`() {
        // La versión 14 es la publicada con Billing 7 y API 35: Play rechaza
        // un paquete cuyo código no supere al ya subido.
        assertTrue(intSetting("versionCode") > LAST_PUBLISHED_VERSION_CODE)
    }

    private companion object {
        const val MIN_TARGET_SDK = 36
        const val MIN_BILLING_MAJOR = 8
        const val LAST_PUBLISHED_VERSION_CODE = 14
    }
}

package com.momentummm.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guardas sobre los ficheros de los widgets: cada una corresponde a un fallo que
 * ya llegó a usuarios y se pone en rojo si alguien lo reintroduce.
 */
class WidgetGuardTest {

    private val main: File = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull { File(it, "AndroidManifest.xml").isFile }
        ?: error("No se encontró src/main desde ${File(".").absolutePath}")

    private val manifest = stripXmlComments(File(main, "AndroidManifest.xml").readText())
    private val widgetSources = File(main, "java/com/momentummm/app/widget").listFiles()!!
        .filter { it.extension == "kt" }
    private val providerInfos = File(main, "res/xml").listFiles()!!
        .filter { it.readText().contains("<appwidget-provider") }

    /** Código sin comentarios: los comentarios citan a propósito el fallo que se corrigió. */
    private fun File.code(): String = readText()
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""//[^\n]*"""), "")

    private fun stripXmlComments(xml: String): String =
        xml.replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "")

    private val languages = listOf("values-en", "values-pt", "values-fr", "values-de")

    @Test
    fun `cada widget tiene su propio nombre en el selector`() {
        // Sin android:label el selector mostraba los cuatro como «Momentum».
        val receivers = Regex("""<receiver\b[^>]*>(?:(?!</receiver>).)*android\.appwidget\.provider""", RegexOption.DOT_MATCHES_ALL)
            .findAll(manifest).map { it.value.substringBefore(">") }.toList()
        assertEquals("receptores de widget en el manifiesto", 4, receivers.size)
        receivers.forEach { tag ->
            assertTrue("Falta android:label en $tag", tag.contains("android:label=\"@string/"))
        }
    }

    @Test
    fun `la vista inicial es la de carga, no una previsualizacion con cifras inventadas`() {
        assertEquals(4, providerInfos.size)
        providerInfos.forEach { file ->
            val xml = stripXmlComments(file.readText())
            assertTrue("${file.name}: initialLayout", xml.contains("""android:initialLayout="@layout/glance_default_loading_layout""""))
            assertTrue("${file.name}: previewLayout", xml.contains("android:previewLayout="))
            // Android 8-11 ignora previewLayout: sin previewImage el selector
            // mostraba el icono de la app en vez del widget.
            val image = Regex("""android:previewImage="@drawable/([a-z0-9_]+)"""").find(xml)
            assertTrue("${file.name}: previewImage", image != null)
            val name = image!!.groupValues[1]
            val exists = File(main, "res").listFiles()!!
                .filter { it.name.startsWith("drawable") }
                .any { dir -> dir.listFiles()!!.any { it.nameWithoutExtension == name } }
            assertTrue("${file.name}: no existe @drawable/$name", exists)
            // «Reconfigurar» sin actividad de configuración no hace nada.
            if (xml.contains("reconfigurable")) {
                assertTrue("${file.name}: reconfigurable sin configure", xml.contains("android:configure="))
            }
        }
    }

    @Test
    fun `los widgets abren la app por su clase, no por un nombre de paquete fijo`() {
        // setClassName("com.momentummm.app", …) no abre nada en la versión .debug.
        widgetSources.forEach { file ->
            assertTrue("${file.name} usa setClassName", !file.code().contains("setClassName("))
        }
    }

    @Test
    fun `ningun texto visible de los widgets esta escrito en el codigo`() {
        // Antes «vividas», «restantes», «EN PROGRESO»… salían en español en todos los idiomas.
        val literal = Regex("""\btext\s*=\s*"""")
        widgetSources.forEach { file ->
            val hit = literal.find(file.code())
            assertTrue("${file.name}: texto fijo «${hit?.value}»", hit == null)
        }
    }

    @Test
    fun `ninguna fila o columna de Glance se rellena con un bucle`() {
        // Glance 1.0 recorta cualquier contenedor a 10 elementos («Truncated
        // container from 26 to 10»): la cuadrícula vieja era un repeat(26)
        // dentro de una Row. Fuera de un @Composable un bucle no crea vistas.
        val loop = Regex("""\brepeat\(|\.forEach\s*\{|\bfor\s*\(""")
        widgetSources.forEach { file ->
            composableBodies(file.code()).forEach { body ->
                val hit = loop.find(body)
                assertTrue("${file.name}: bucle «${hit?.value}» dentro de un @Composable", hit == null)
            }
        }
    }

    /** Cuerpos (entre llaves) de las funciones marcadas @Composable. */
    private fun composableBodies(source: String): List<String> =
        Regex("""@Composable\s+(?:private\s+|internal\s+)?fun\b""").findAll(source).map { match ->
            val open = source.indexOf('{', match.range.last)
            var depth = 0
            var end = open
            for (i in open until source.length) {
                when (source[i]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) { end = i; break }
                }
            }
            source.substring(open, end + 1)
        }.toList()

    @Test
    fun `todos los textos de los widgets existen en los cinco idiomas`() {
        val base = keysOf(File(main, "res/values/strings_widget.xml"))
        assertTrue(base.isNotEmpty())
        languages.forEach { dir ->
            val keys = keysOf(File(main, "res/$dir/strings_widget.xml"))
            assertEquals("$dir: faltan ${base - keys}, sobran ${keys - base}", base, keys)
        }
    }

    @Test
    fun `las frases y mensajes de respaldo tienen el mismo numero de entradas en cada idioma`() {
        val base = File(main, "res/values/strings_widget.xml").readText()
        for (array in listOf("widget_quotes", "widget_messages")) {
            val expected = itemCount(base, array)
            assertTrue("$array vacío", expected > 0)
            languages.forEach { dir ->
                val text = File(main, "res/$dir/strings_widget.xml").readText()
                assertEquals("$dir/$array", expected, itemCount(text, array))
            }
        }
    }

    @Test
    fun `cada frase de respaldo lleva autor`() {
        (listOf("values") + languages).forEach { dir ->
            val text = File(main, "res/$dir/strings_widget.xml").readText()
            val block = arrayBlock(text, "widget_quotes")
            Regex("""<item>(.*?)</item>""").findAll(block).forEach {
                val quote = WidgetQuotes.parse(it.groupValues[1])
                assertTrue("$dir: «${it.groupValues[1]}» sin autor", quote?.author != null)
            }
        }
    }

    @Test
    fun `los mensajes de respaldo no llevan marcadores ni emoji`() {
        (listOf("values") + languages).forEach { dir ->
            val text = File(main, "res/$dir/strings_widget.xml").readText()
            Regex("""<item>(.*?)</item>""").findAll(arrayBlock(text, "widget_messages")).forEach {
                val message = it.groupValues[1]
                assertTrue("$dir: «$message»", WidgetMessages.isShowable(message))
                assertTrue("$dir: emoji en «$message»", message.codePoints().noneMatch { cp -> cp >= 0x1F000 })
            }
        }
    }

    @Test
    fun `las etiquetas bajo las cifras caben en su columna`() {
        // Una columna de tres en el widget de 3 × 3 tiene ~61 dp: a 10 sp eso son
        // unas 10 letras. «semanas vividas» (15) se cortaba a media palabra, y
        // Glance no pone puntos suspensivos.
        val plurals = listOf(
            "widget_life_weeks_lived", "widget_life_weeks_left", "widget_life_years",
            "widget_year_days_passed", "widget_year_days_left",
        )
        (listOf("values") + languages).forEach { dir ->
            val text = File(main, "res/$dir/strings_widget.xml").readText()
            plurals.forEach { name ->
                val block = text.substringAfter("""<plurals name="$name">""").substringBefore("</plurals>")
                Regex("""<item quantity="\w+">(.*?)</item>""").findAll(block).forEach {
                    val label = it.groupValues[1]
                    assertTrue("$dir/$name: «$label» tiene ${label.length} letras", label.length <= MAX_STAT_LABEL)
                }
            }
            listOf("widget_quote_label", "widget_message_label").forEach { name ->
                val label = Regex("""<string name="$name">(.*?)</string>""").find(text)?.groupValues?.get(1).orEmpty()
                assertTrue("$dir/$name: «$label»", label.isNotEmpty() && label.length <= MAX_CARD_LABEL)
            }
        }
    }

    private fun keysOf(file: File): Set<String> =
        Regex("""<(?:string|plurals|string-array) name="([^"]+)"""").findAll(file.readText())
            .map { it.groupValues[1] }.toSet()

    private fun arrayBlock(text: String, name: String): String =
        text.substringAfter("""<string-array name="$name">""").substringBefore("</string-array>")

    private fun itemCount(text: String, name: String): Int =
        Regex("<item>").findAll(arrayBlock(text, name)).count()

    private companion object {
        /** Letras que caben a 10 sp en una de las tres columnas de cifras (~61 dp). */
        const val MAX_STAT_LABEL = 10

        /** Letras que caben a 11 sp en la etiqueta de la Frase y el Mensaje (160 dp). */
        const val MAX_CARD_LABEL = 20
    }
}

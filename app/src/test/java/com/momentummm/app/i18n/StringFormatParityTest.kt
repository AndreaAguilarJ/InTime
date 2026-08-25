package com.momentummm.app.i18n

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guardián de traducciones. Lee los ficheros de recursos reales del proyecto.
 *
 * Vigila una clase de fallo que NO se ve al compilar y que solo aparece en un idioma:
 * si una traducción pierde o cambia un especificador de formato (`%1$s`, `%d`), Android
 * lanza excepción al construir ese texto. El usuario español no nota nada y el alemán
 * ve la app romperse en esa pantalla.
 *
 * También detecta el caso inverso: una traducción que AÑADE un argumento que quien la
 * usa no le pasa.
 *
 * Es un test de datos, no de lógica: cubre las 1097 claves existentes y cualquiera que
 * se añada en el futuro, sin que nadie tenga que acordarse de comprobarlo.
 */
class StringFormatParityTest {

    /** Especificadores de formato de Java/Android, incluido el `%%` literal. */
    private val especificador = Regex("%%|%(\\d+\\$)?[-+ 0,#]*\\d*(?:\\.\\d+)?[sdfeEgGxXoc]")

    private val idiomas = listOf("values-en", "values-de", "values-fr", "values-pt")

    private fun raizDeRecursos(): File {
        // Las pruebas unitarias de Android corren con el directorio del módulo como
        // working directory. Se prueban las dos rutas posibles para no depender de eso.
        val candidatos = listOf(File("src/main/res"), File("app/src/main/res"))
        return candidatos.firstOrNull { it.isDirectory }
            ?: error("no encuentro res/ desde ${File("").absolutePath}")
    }

    /** Devuelve clave -> lista ordenada de especificadores, por cada fichero de ese idioma. */
    private fun leerCadenas(carpeta: File): Map<String, List<String>> {
        if (!carpeta.isDirectory) return emptyMap()
        val resultado = mutableMapOf<String, List<String>>()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        carpeta.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }
            ?.sortedBy { it.name }
            ?.forEach { fichero ->
                val doc = builder.parse(fichero)
                val nodos = doc.getElementsByTagName("string")
                for (i in 0 until nodos.length) {
                    val nodo = nodos.item(i)
                    val nombre = nodo.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val texto = nodo.textContent ?: ""
                    resultado[nombre] = especificador.findAll(texto).map { it.value }.toList()
                }
            }
        return resultado
    }

    /**
     * Devuelve clave -> (cantidad -> especificadores) por cada `<plurals>`.
     *
     * Los `<string>` no cubren esto: un `<plurals>` tiene un `<item>` por cantidad
     * (`one`, `other`, …) y cada uno formatea con los MISMOS argumentos. Si una
     * traducción pierde el `%1$d` en su forma `one` —justo lo que estuvo a punto de
     * pasar al convertir cadenas a plurales— la app revienta solo cuando el contador
     * vale 1, en ese idioma. Compila y pasa desapercibido.
     */
    private fun leerPlurales(carpeta: File): Map<String, Map<String, List<String>>> {
        if (!carpeta.isDirectory) return emptyMap()
        val resultado = mutableMapOf<String, Map<String, List<String>>>()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        carpeta.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }
            ?.sortedBy { it.name }
            ?.forEach { fichero ->
                val doc = builder.parse(fichero)
                val nodos = doc.getElementsByTagName("plurals")
                for (i in 0 until nodos.length) {
                    val nodo = nodos.item(i)
                    val nombre = nodo.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    val porCantidad = mutableMapOf<String, List<String>>()
                    val hijos = nodo.childNodes
                    for (j in 0 until hijos.length) {
                        val item = hijos.item(j)
                        if (item.nodeName != "item") continue
                        val cantidad = item.attributes?.getNamedItem("quantity")?.nodeValue ?: continue
                        porCantidad[cantidad] = especificador.findAll(item.textContent ?: "").map { it.value }.toList()
                    }
                    resultado[nombre] = porCantidad
                }
            }
        return resultado
    }
        val res = raizDeRecursos()
        val base = leerCadenas(File(res, "values"))
        assertTrue("el idioma por defecto debe tener cadenas", base.isNotEmpty())

        val problemas = mutableListOf<String>()

        for (idioma in idiomas) {
            val traducido = leerCadenas(File(res, idioma))
            if (traducido.isEmpty()) continue

            for ((clave, esperados) in base) {
                val obtenidos = traducido[clave] ?: continue // clave sin traducir: otro asunto
                if (esperados.sorted() != obtenidos.sorted()) {
                    problemas += "$idioma / $clave: el original usa $esperados y la " +
                        "traducción usa $obtenidos"
                }
            }
        }

        assertTrue(
            "Hay traducciones cuyos especificadores de formato no coinciden con el " +
                "original. Cada una de estas revienta la app en ese idioma:\n" +
                problemas.joinToString("\n"),
            problemas.isEmpty()
        )
    }

    @Test
    fun `ninguna traduccion usa argumentos que el original no tiene`() {
        // Caso especialmente traicionero: la traducción pide %2$s pero quien la usa solo
        // pasa un argumento. Compila, y falla al mostrarse.
        val res = raizDeRecursos()
        val base = leerCadenas(File(res, "values"))
        val problemas = mutableListOf<String>()

        for (idioma in idiomas) {
            val traducido = leerCadenas(File(res, idioma))
            for ((clave, obtenidos) in traducido) {
                val esperados = base[clave] ?: continue
                val sobran = obtenidos.filterNot { it == "%%" }.size -
                    esperados.filterNot { it == "%%" }.size
                if (sobran > 0) {
                    problemas += "$idioma / $clave: la traducción usa $sobran argumento(s) más"
                }
            }
        }

        assertTrue(
            "Traducciones que piden más argumentos que el original:\n" +
                problemas.joinToString("\n"),
            problemas.isEmpty()
        )
    }

    @Test
    fun `cada forma de un plural conserva los mismos especificadores en todos los idiomas`() {
        val res = raizDeRecursos()
        val base = leerPlurales(File(res, "values"))
        assertTrue("el idioma por defecto debe tener plurales", base.isNotEmpty())

        val problemas = mutableListOf<String>()

        // Canónico: los especificadores que deben aparecer en CUALQUIER forma de esa clave.
        // Se toma de la forma 'other' del original (siempre presente); si no, de cualquiera.
        for ((clave, formasBase) in base) {
            val canonico = (formasBase["other"] ?: formasBase.values.firstOrNull() ?: emptyList()).sorted()

            // El propio original debe ser internamente coherente (one y other con los mismos args).
            for ((cantidad, specs) in formasBase) {
                if (specs.sorted() != canonico) {
                    problemas += "values / $clave [$cantidad]: usa $specs pero se esperaba $canonico"
                }
            }

            // Y cada idioma, en cada forma que defina.
            for (idioma in idiomas) {
                val formas = leerPlurales(File(res, idioma))[clave] ?: continue
                for ((cantidad, specs) in formas) {
                    if (specs.sorted() != canonico) {
                        problemas += "$idioma / $clave [$cantidad]: usa $specs pero se esperaba $canonico"
                    }
                }
            }
        }

        assertTrue(
            "Hay formas de plural cuyos especificadores no coinciden. Cada una revienta la " +
                "app en ese idioma cuando el contador cae en esa cantidad:\n" +
                problemas.joinToString("\n"),
            problemas.isEmpty()
        )
    }

    @Test
    fun `los cinco idiomas se leen y tienen contenido`() {
        // Si un fichero se corrompe o se renombra, el resto de pruebas pasarian en vacio.
        val res = raizDeRecursos()
        val base = leerCadenas(File(res, "values"))
        assertTrue("values debe tener más de 1000 claves, tiene ${base.size}", base.size > 1000)

        for (idioma in idiomas) {
            val traducido = leerCadenas(File(res, idioma))
            assertTrue(
                "$idioma debe tener más de 900 claves, tiene ${traducido.size}",
                traducido.size > 900
            )
        }
    }
}

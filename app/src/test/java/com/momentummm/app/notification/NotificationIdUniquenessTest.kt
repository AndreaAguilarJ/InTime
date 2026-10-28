package com.momentummm.app.notification

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ningún aviso puede compartir id con otro de OTRO componente.
 *
 * Android identifica una notificación por (paquete, id): si dos partes de la
 * app publican el mismo id, la segunda REEMPLAZA a la primera. Así se colaba
 * un fallo real: el Modo nuclear usaba 3001 para su aviso fijo de servicio, el
 * mismo que los mensajes motivacionales, y cada mensaje sustituía ese aviso y
 * se volvía imposible de cerrar mientras el modo estuviera activo. El monitor
 * de apps (1001) y el bloqueo superpuesto (1003) chocaban igual con las metas,
 * y el temporizador flotante (2001) con el aviso de límite.
 *
 * Se leen los fuentes como texto: cada `const val …NOTIFICATION_ID… = N`.
 */
class NotificationIdUniquenessTest {

    private val sourceRoot: File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull { File(it, "com/momentummm/app").isDirectory }
        ?: error("No se encontraron los fuentes desde ${File(".").absolutePath}")

    private val declaration = Regex("""const val (\w*NOTIFICATION_ID\w*)\s*=\s*(\d+)""")

    @Test
    fun `cada id de notificacion pertenece a un solo componente`() {
        val owners = mutableMapOf<Int, MutableSet<String>>()
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                declaration.findAll(file.readText()).forEach { match ->
                    val id = match.groupValues[2].toInt()
                    owners.getOrPut(id) { mutableSetOf() } += "${file.name}:${match.groupValues[1]}"
                }
            }

        assertTrue("No se encontró ningún id: la expresión dejó de casar", owners.size > 10)

        val clashes = owners.filter { (_, names) -> names.map { it.substringBefore(':') }.toSet().size > 1 }
        assertTrue(
            "Ids compartidos entre componentes (el segundo aviso borra al primero): " +
                clashes.entries.joinToString { (id, names) -> "$id → ${names.sorted()}" },
            clashes.isEmpty()
        )
    }

    /**
     * Android pinta el icono pequeño solo con su canal alfa: un icono a color
     * (`mipmap/ic_launcher`) sale como un cuadrado blanco en la barra de estado,
     * que es como salía el temporizador de enfoque.
     */
    @Test
    fun `ningun aviso usa el icono a color de la app como icono pequeno`() {
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { Regex("""setSmallIcon\(\s*R\.mipmap\.""").containsMatchIn(it.readText()) }
            .map { it.name }
            .toList()
        assertTrue("setSmallIcon con un mipmap a color en: $offenders", offenders.isEmpty())
    }
}

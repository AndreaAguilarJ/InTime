package com.momentummm.shared.notification

/**
 * Avisos locales del sistema.
 *
 * En iOS esto no es un adorno sino la única forma de que el cronómetro funcione:
 * el sistema **suspende** la app en segundo plano, de modo que un bucle contando
 * segundos deja de avanzar en cuanto la persona cambia de app. El aviso hay que
 * programarlo al arrancar la sesión, para la hora exacta en que termina, y
 * cancelarlo si se abandona antes.
 *
 * No requiere ningún permiso especial de Apple; solo el consentimiento de la
 * persona, que el sistema pide la primera vez.
 */
interface MomentumNotifier {

    /** Pide permiso para avisar. Repetirlo es inofensivo: el sistema recuerda. */
    fun requestPermission()

    /**
     * Programa un aviso para dentro de [seconds].
     *
     * @param id identificador estable; volver a programar con el mismo id
     *   sustituye el aviso anterior en vez de acumular duplicados.
     */
    fun scheduleAfter(id: String, seconds: Int, title: String, body: String)

    /** Cancela un aviso programado que ya no procede. */
    fun cancel(id: String)
}

/** Identificadores de los avisos que emite el núcleo compartido. */
object NotificationIds {
    /** Fin del intervalo de enfoque. */
    const val FOCUS_COMPLETE = "focus_complete"

    /** Fin del descanso. */
    const val BREAK_COMPLETE = "break_complete"
}

/** Implementación que no hace nada, para pruebas y previsualizaciones. */
class NoOpNotifier : MomentumNotifier {
    override fun requestPermission() = Unit
    override fun scheduleAfter(id: String, seconds: Int, title: String, body: String) = Unit
    override fun cancel(id: String) = Unit
}

/** Registra las llamadas en memoria, para comprobar en pruebas qué se programó. */
class RecordingNotifier : MomentumNotifier {
    data class Scheduled(val id: String, val seconds: Int, val title: String, val body: String)

    val scheduled = mutableListOf<Scheduled>()
    val cancelled = mutableListOf<String>()
    var permissionRequested = false
        private set

    override fun requestPermission() {
        permissionRequested = true
    }

    override fun scheduleAfter(id: String, seconds: Int, title: String, body: String) {
        // Mismo contrato que el sistema: reprogramar con el mismo id sustituye.
        scheduled.removeAll { it.id == id }
        scheduled += Scheduled(id, seconds, title, body)
    }

    override fun cancel(id: String) {
        scheduled.removeAll { it.id == id }
        cancelled += id
    }
}

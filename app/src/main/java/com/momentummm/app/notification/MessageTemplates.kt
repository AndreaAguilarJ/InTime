package com.momentummm.app.notification

import com.momentummm.app.data.entity.MessageCategory

/**
 * Lógica de texto de la personalización, sin dependencias de Android.
 *
 * Está separada de [MessagePersonalizer] a propósito: ahí viven las lecturas de
 * base de datos y de estadísticas de uso, que necesitan un `Context`. Aquí sólo
 * hay funciones puras, así que se pueden verificar con tests JVM normales sin
 * emulador ni Robolectric.
 */
object MessageTemplates {

    /** A partir de esta racha merece la pena mencionarla en el mensaje. */
    const val STREAK_WORTH_MENTIONING = 2

    /** Datos reales del usuario usados para personalizar. */
    data class UserContext(
        val name: String? = null,
        val streak: Int = 0,
        val level: Int = 1,
        val timeCoins: Int = 0,
        val screenTimeMinutesToday: Int = 0
    )

    /**
     * Sustituye los marcadores de [content] por datos reales.
     *
     * Los marcadores existen en dos idiomas para que valgan tanto los mensajes
     * de la semilla en español como cualquier mensaje que escriba el usuario.
     *
     * Si no hay nombre configurado, `{nombre}` se **elimina** en lugar de
     * inventar un tratamiento: "Hola {nombre}" queda en "Hola", no en
     * "Hola usuario". [tidy] limpia la coma o el espacio que sobran.
     */
    fun applyPlaceholders(
        content: String,
        userContext: UserContext,
        hourOfDay: Int
    ): String {
        if (!content.contains('{')) return content

        val name = userContext.name?.trim().orEmpty()
        val replaced = content
            .replaceToken("nombre", name)
            .replaceToken("name", name)
            .replaceToken("racha", userContext.streak.toString())
            .replaceToken("streak", userContext.streak.toString())
            .replaceToken("nivel", userContext.level.toString())
            .replaceToken("level", userContext.level.toString())
            .replaceToken("monedas", userContext.timeCoins.toString())
            .replaceToken("coins", userContext.timeCoins.toString())
            .replaceToken("pantalla", formatMinutes(userContext.screenTimeMinutesToday))
            .replaceToken("screen_time", formatMinutes(userContext.screenTimeMinutesToday))
            .replaceToken("saludo", greeting(hourOfDay))
            .replaceToken("greeting", greeting(hourOfDay))

        return tidy(replaced)
    }

    /**
     * Título dirigido al usuario: "☀️ Buenos días" → "☀️ Buenos días, Andrea".
     * Sin nombre configurado devuelve el título tal cual.
     */
    fun buildTitle(baseTitle: String, userContext: UserContext): String {
        val name = userContext.name?.trim()?.takeIf { it.isNotEmpty() } ?: return baseTitle
        // Evita "Buenos días, Andrea, Andrea" si el título ya la nombra.
        if (baseTitle.contains(name, ignoreCase = true)) return baseTitle
        return "$baseTitle, $name"
    }

    /**
     * Línea extra con un dato real del día, o null si no hay nada que aportar.
     *
     * Se limita a un solo dato: una notificación motivacional con un informe de
     * estadísticas deja de ser motivadora.
     */
    fun buildContextLine(userContext: UserContext, category: MessageCategory?): String? {
        val streak = userContext.streak
        val screen = userContext.screenTimeMinutesToday

        return when {
            // La racha es el dato más motivador cuando existe.
            streak >= STREAK_WORTH_MENTIONING ->
                "🔥 Llevas $streak días seguidos cuidando tu tiempo."

            // Al cerrar el día, el tiempo de pantalla da perspectiva.
            category == MessageCategory.EVENING && screen > 0 ->
                "📱 Hoy has usado el teléfono ${formatMinutes(screen)}."

            category == MessageCategory.MORNING ->
                "🌱 Día nuevo, contador a cero."

            else -> null
        }
    }

    /** Saludo según la hora, para el marcador `{saludo}`. */
    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0 min"
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "$mins min"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}min"
        }
    }

    /**
     * Limpia los restos de un marcador vacío: comas o espacios sobrantes que
     * quedan al quitar el nombre cuando no está configurado.
     */
    fun tidy(text: String): String = text
        .replace(Regex("\\s+([,.!?;:])"), "$1")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("(^|\\n)[ \\t]*[,;:]\\s*"), "$1")
        .trim()

    /**
     * Sustituye `{token}` sin distinguir mayúsculas.
     *
     * Se usa el `replace` con lambda a propósito: ese overload trata el valor
     * devuelto como texto literal. Con el overload de String habría que escapar
     * `$` y `\` del nombre del usuario, y un nombre como "A$B" saldría
     * corrompido o lanzaría excepción.
     */
    private fun String.replaceToken(token: String, value: String): String {
        if (!contains("{$token}", ignoreCase = true)) return this
        return Regex("\\{$token\\}", RegexOption.IGNORE_CASE).replace(this) { value }
    }
}

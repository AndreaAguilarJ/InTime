package com.momentummm.app.notification

/**
 * Pantallas a las que puede llevar una notificación al tocarla.
 *
 * ─── POR QUÉ EXISTE ──────────────────────────────────────────────────────
 * Las notificaciones ya guardaban un extra `navigate_to` («goals»,
 * «motivational_settings»…), pero nadie lo leía: tocar «Instagram: límite
 * alcanzado» o «Tu resumen semanal» abría siempre la pantalla de inicio, y
 * además con `FLAG_ACTIVITY_CLEAR_TASK`, que destruía la pantalla en la que
 * estuviera el usuario. El aviso decía algo y la app no lo continuaba.
 *
 * Cada valor es una ruta REAL del grafo de navegación de `MainAppContent`.
 * Cualquier otro texto se descarta en [sanitize]: un extra viene de fuera de
 * la composición y no debe poder pedir una ruta inexistente, que haría
 * lanzar a Navigation un IllegalArgumentException.
 */
object NotificationRoutes {

    /** Extra del Intent que lleva el destino. Se conserva el nombre antiguo. */
    const val EXTRA_DESTINATION = "navigate_to"

    const val TODAY = "today"
    const val ANALYTICS = "analytics"
    const val FOCUS = "focus"
    const val MY_LIFE = "my_life"
    const val APP_LIMITS = "app_limits"
    const val WEBSITE_BLOCKS = "website_blocks"
    const val GAMIFICATION = "gamification_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val MOTIVATIONAL_SETTINGS = "motivational_messages_settings"
    const val MOTIVATIONAL_LIBRARY = "motivational_messages_library"

    /** Pestañas de la barra inferior: se navega a ellas sin apilar copias. */
    val TAB_ROUTES: Set<String> = setOf(TODAY, ANALYTICS, FOCUS)

    private val KNOWN: Set<String> = setOf(
        TODAY, ANALYTICS, FOCUS, MY_LIFE, APP_LIMITS, WEBSITE_BLOCKS,
        GAMIFICATION, NOTIFICATION_SETTINGS, MOTIVATIONAL_SETTINGS, MOTIVATIONAL_LIBRARY
    )

    /**
     * Valores que escribían versiones anteriores y que pueden seguir vivos en
     * notificaciones ya publicadas en la bandeja del usuario.
     */
    private val LEGACY_ALIASES: Map<String, String> = mapOf(
        "motivational_settings" to MOTIVATIONAL_SETTINGS,
        "goals" to TODAY,
        "challenges" to TODAY,
        "achievements" to TODAY
    )

    /** Devuelve una ruta navegable o null si [raw] no es un destino conocido. */
    fun sanitize(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (value in KNOWN) return value
        return LEGACY_ALIASES[value]
    }
}

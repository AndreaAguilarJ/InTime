package com.momentummm.app.data.account

import android.content.Context
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que la app sabe del servidor de cuentas (Appwrite). */
enum class AccountServer {
    /** Todavía no se ha preguntado. */
    UNKNOWN,

    /** Respondió con normalidad, aunque sea para decir que no hay sesión. */
    AVAILABLE,

    /** Respondió que no atiende a nadie: proyecto en solo lectura o caída del servidor. */
    UNAVAILABLE,

    /** No se pudo hablar con él: sin red, DNS o tiempo agotado. */
    UNREACHABLE
}

/**
 * Reglas del modo sin cuenta.
 *
 * La cuenta es opcional: todo lo que hace Momentum (límites, bloqueos, enfoque,
 * Mi vida en semanas, widgets) se guarda en el teléfono. Antes la app entera
 * estaba detrás del inicio de sesión, y cuando el proyecto de Appwrite pasó a
 * solo lectura (402 `team_read_only` en cualquier petición, incluso
 * `GET /locale`) nadie pudo usarla, ni el revisor de Google Play.
 */
object AccountAccess {

    /** El servidor contestó, pero se niega a atender a cualquiera. */
    private fun isServiceRefusal(code: Int?, type: String?): Boolean =
        type == "team_read_only" || code == 402 || (code != null && code >= 500)

    fun classify(error: Throwable): AccountServer = when {
        error is AppwriteException && isServiceRefusal(error.code, error.type) -> AccountServer.UNAVAILABLE
        error is AppwriteException -> AccountServer.AVAILABLE
        else -> AccountServer.UNREACHABLE
    }

    /** Con sesión o en modo sin cuenta se entra en la app. */
    fun entersApp(isLoggedIn: Boolean, localMode: Boolean): Boolean = isLoggedIn || localMode

    /**
     * Si el servidor no atiende, la pantalla de cuenta solo llevaría a un error:
     * se entra directamente sin cuenta. Sin red no se decide nada, porque puede
     * ser pasajero y la bienvenida ya ofrece seguir sin cuenta.
     */
    fun shouldAutoEnterLocal(server: AccountServer, isLoggedIn: Boolean): Boolean =
        !isLoggedIn && server == AccountServer.UNAVAILABLE

    /** Iniciar sesión y crear cuenta solo se ofrecen si pueden funcionar. */
    fun offersAccounts(server: AccountServer): Boolean = server != AccountServer.UNAVAILABLE
}

/** Recuerda que la persona eligió usar Momentum sin cuenta en este teléfono. */
class LocalModeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _localMode = MutableStateFlow(prefs.getBoolean(KEY_LOCAL_MODE, false))
    val localMode: StateFlow<Boolean> = _localMode.asStateFlow()

    fun setLocalMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_MODE, enabled).apply()
        _localMode.value = enabled
    }

    private companion object {
        const val PREFS = "account_access"
        const val KEY_LOCAL_MODE = "local_mode"
    }
}

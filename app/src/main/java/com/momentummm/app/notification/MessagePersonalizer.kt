package com.momentummm.app.notification

import android.content.Context
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.data.dao.UserDao
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.repository.UsageStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convierte un mensaje motivacional genérico en uno dirigido al usuario.
 *
 * ─── POR QUÉ EXISTE ──────────────────────────────────────────────────────
 * Los "mensajes personalizados" no lo eran: la notificación mostraba tal cual
 * el texto de la tabla semilla, idéntico para todo el mundo y sin relación con
 * lo que estaba pasando en el teléfono. La única personalización del proyecto
 * vivía en [com.momentummm.app.data.ai.MotivationalMessageGenerator], que
 * requiere IA activada y conexión, y que por defecto está desactivada.
 *
 * Aquí la personalización es local, instantánea y siempre disponible:
 *
 *  - **Nombre**: si el usuario ha dicho cómo quiere que se le llame, el título
 *    se dirige a él ("Buenos días, Andrea").
 *  - **Marcadores en el texto**: `{nombre}`, `{racha}`, `{nivel}`,
 *    `{pantalla}`, `{monedas}`, `{saludo}` se sustituyen por datos reales. Los
 *    mensajes de la semilla que no los usan siguen funcionando igual.
 *  - **Línea de contexto**: se añade un dato real del día (racha en curso,
 *    tiempo de pantalla de hoy) cuando aporta algo.
 *
 * Todo dato es opcional: si algo falla o no hay información, el mensaje
 * original se entrega sin tocar. Nunca se deja de enviar por esto.
 *
 * La lógica de texto vive en [MessageTemplates], sin dependencias de Android,
 * para poder verificarla con tests JVM.
 */
@Singleton
class MessagePersonalizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val usageStatsRepository: UsageStatsRepository
) {

    /** Alias para no obligar a los llamadores a nombrar [MessageTemplates]. */
    companion object {
        fun emptyContext(): MessageTemplates.UserContext = MessageTemplates.UserContext()
    }

    /**
     * Lee los datos del usuario.
     *
     * Va en [Dispatchers.IO] a propósito: consulta DataStore, Room y
     * UsageStats. El botón "Probar" de los ajustes llama a esta cadena desde
     * `viewModelScope`, que es el hilo principal, y StrictMode detectó ahí
     * lecturas de disco en el hilo de UI.
     */
    suspend fun loadUserContext(): MessageTemplates.UserContext = withContext(Dispatchers.IO) {
        val name = runCatching { UserPreferencesRepository.getDisplayName(context) }
            .getOrNull()

        val settings = runCatching { userDao.getUserSettingsSync() }.getOrNull()

        val screenMinutes = runCatching {
            (usageStatsRepository.getTotalScreenTime() / 60_000L).toInt()
        }.getOrDefault(0)

        MessageTemplates.UserContext(
            name = name,
            streak = settings?.currentStreak ?: 0,
            level = settings?.userLevel ?: 1,
            timeCoins = settings?.timeCoins ?: 0,
            screenTimeMinutesToday = screenMinutes
        )
    }

    fun applyPlaceholders(content: String, userContext: MessageTemplates.UserContext): String =
        MessageTemplates.applyPlaceholders(content, userContext, currentHour())

    fun buildTitle(baseTitle: String, userContext: MessageTemplates.UserContext): String =
        MessageTemplates.buildTitle(baseTitle, userContext)

    fun buildContextLine(
        userContext: MessageTemplates.UserContext,
        category: MessageCategory?
    ): String? = MessageTemplates.buildContextLine(userContext, category)

    private fun currentHour(): Int =
        runCatching { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }.getOrDefault(12)
}

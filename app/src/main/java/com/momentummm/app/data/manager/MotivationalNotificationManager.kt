package com.momentummm.app.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import com.momentummm.app.data.repository.MotivationalMessagesRepository
import com.momentummm.app.notification.MessagePersonalizer
import com.momentummm.app.receiver.MotivationalNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages motivational message notifications with smart timing and interactive actions.
 * 
 * Features:
 * - High priority notification channel for motivational messages
 * - BigTextStyle for longer messages
 * - Interactive actions: "❤️ Me encantó", "🔄 Otro mensaje", "⚙️ Configurar"
 * - Smart timing integration with user preferences
 * - Integration with goals and streaks for contextual messages
 */
@Singleton
class MotivationalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messagesRepository: MotivationalMessagesRepository,
    private val personalizer: MessagePersonalizer
) {

    companion object {
        private const val TAG = "MotivationalNotifMgr"
        
        // Channel ID
        const val CHANNEL_ID_MOTIVATIONAL = "motivational_messages_channel"
        
        // Notification IDs
        const val NOTIFICATION_ID_MOTIVATIONAL = 3001
        const val NOTIFICATION_ID_GOAL_PROGRESS = 3002
        const val NOTIFICATION_ID_STREAK = 3003
        const val NOTIFICATION_ID_COMEBACK = 3004
        
        // Action request codes
        const val REQUEST_CODE_LOVE = 3101
        const val REQUEST_CODE_ANOTHER = 3102
        const val REQUEST_CODE_SETTINGS = 3103
        const val REQUEST_CODE_OPEN = 3104

        /**
         * Prefijo de los mensajes incrustados en el código que se usan cuando
         * la base de datos no devuelve nada. No existen como fila, así que su
         * id no debe colisionar con los de la semilla.
         */
        private const val FALLBACK_ID_PREFIX = "builtin_fallback_"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create the motivational messages notification channel with high priority.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MOTIVATIONAL,
                context.getString(R.string.notification_channel_motivational_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_motivational_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show a motivational message notification with actions.
     *
     * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────
     * 1. **Se perdía el mensaje si no había ninguno "disponible".** Si
     *    `getNextMessageToShow()` devolvía null se salía sin más: el usuario no
     *    recibía nada y no quedaba ni una traza del motivo. Ahora hay una
     *    cadena de respaldo que termina en un mensaje incrustado en el código,
     *    así que **siempre** se entrega algo.
     * 2. **La ventana horaria descartaba mensajes legítimos.** Sin permiso de
     *    alarmas exactas el sistema puede retrasar la alarma hasta una hora, de
     *    modo que un mensaje programado a las 21:40 llegaba a las 22:10 y
     *    `isGoodTimeForNotification()` lo tiraba a la basura. Las llamadas que
     *    vienen de una alarma ya se programaron dentro de la ventana, así que
     *    pueden pedir [respectWindow] = false.
     */
    suspend fun showMotivationalNotification(
        message: MotivationalMessage? = null,
        contextInfo: String? = null,
        respectWindow: Boolean = true,
        windowGraceMinutes: Int = 0
    ) {
        try {
            if (!ensureCanNotify()) return

            // Check if it's a good time
            if (respectWindow && !messagesRepository.isGoodTimeForNotification(windowGraceMinutes)) {
                Log.d(TAG, "Not a good time for notification, skipping")
                return
            }

            val messageToShow = message ?: resolveMessage(null)

            // Mark as shown
            messagesRepository.markMessageAsShown(messageToShow.id, contextInfo)

            // Build and show notification
            showNotificationInternal(messageToShow, NOTIFICATION_ID_MOTIVATIONAL)

            Log.d(TAG, "Showed motivational notification: ${messageToShow.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing motivational notification", e)
        }
    }

    /**
     * Envía un mensaje ahora mismo, sin comprobar la ventana horaria.
     *
     * Es lo que usa el botón "Probar" de los ajustes. Antes ese botón sólo
     * abría un diálogo de vista previa mientras el mensaje de la interfaz
     * afirmaba "Notificación de prueba enviada": el usuario no podía comprobar
     * si las notificaciones funcionaban de verdad.
     *
     * @return null si se envió, o el motivo por el que no se pudo.
     */
    /**
     * Resultado de enviar un mensaje de prueba, para que la interfaz pueda
     * decir la verdad en lugar de afirmar siempre que se envió.
     */
    data class TestResult(
        val delivered: Boolean,
        val message: MotivationalMessage? = null,
        val error: String? = null
    )

    suspend fun showTestMessage(): TestResult {
        return try {
            if (!areSystemNotificationsEnabled()) {
                return TestResult(
                    delivered = false,
                    error = "Las notificaciones de Momentum están desactivadas en los ajustes del sistema"
                )
            }
            val message = resolveMessage(null)
            messagesRepository.markMessageAsShown(message.id, "test")
            showNotificationInternal(message, NOTIFICATION_ID_MOTIVATIONAL)
            Log.d(TAG, "Mensaje de prueba enviado: ${message.id}")
            TestResult(delivered = true, message = message)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando el mensaje de prueba", e)
            TestResult(
                delivered = false,
                error = "No se pudo enviar la notificación: ${e.message ?: "error desconocido"}"
            )
        }
    }
    
    /**
     * Devuelve el mensaje a mostrar, cayendo hacia atrás hasta un mensaje
     * incrustado si la base de datos no da nada.
     *
     * Antes cada método hacía `if (message == null) return`, así que un seed
     * incompleto o un fallo puntual de Room significaba silencio absoluto.
     */
    private suspend fun resolveMessage(category: MessageCategory?): MotivationalMessage {
        if (category != null) {
            runCatching { messagesRepository.getContextualMessage(category) }
                .getOrNull()
                ?.let { return it }
            Log.w(TAG, "Sin mensajes para la categoría $category; se usa el respaldo general")
        }

        runCatching { messagesRepository.getNextMessageToShow() }
            .getOrNull()
            ?.let { return it }

        Log.w(TAG, "La base de datos no devolvió ningún mensaje; se usa el incrustado")
        return builtInFallback(category)
    }

    /**
     * Último recurso: un mensaje que no depende de la base de datos.
     * Se marca como "no persistido" con un id propio para que `markMessageAsShown`
     * no falle silenciosamente contra una fila inexistente.
     */
    private fun builtInFallback(category: MessageCategory?): MotivationalMessage {
        val resolvedCategory = category ?: MessageCategory.MOTIVATION
        val content = when (resolvedCategory) {
            MessageCategory.MORNING ->
                "Hoy decides tú en qué gastas tus horas. Empieza por lo que de verdad importa."
            MessageCategory.EVENING ->
                "Cierra el día en paz: lo que hiciste cuenta más que lo que dejaste sin hacer."
            else ->
                "Tu tiempo es tu vida. Recupéralo un minuto a la vez."
        }
        return MotivationalMessage(
            id = "$FALLBACK_ID_PREFIX${resolvedCategory.name.lowercase()}",
            content = content,
            category = resolvedCategory,
            tone = MessageTone.FRIENDLY,
            emoji = resolvedCategory.emoji
        )
    }

    /**
     * true si el sistema permite publicar notificaciones.
     *
     * Sin `POST_NOTIFICATIONS` (Android 13+) o con el canal desactivado,
     * `notify()` **no lanza ninguna excepción**: descarta la notificación en
     * silencio. El código anterior sólo capturaba SecurityException, así que el
     * caso real —permiso denegado— pasaba desapercibido y parecía que el
     * sistema de mensajes estaba roto sin dejar rastro en el log.
     */
    private fun areSystemNotificationsEnabled(): Boolean =
        runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }
            .getOrDefault(true)

    private suspend fun ensureCanNotify(): Boolean {
        if (!areSystemNotificationsEnabled()) {
            Log.w(TAG, "Notificaciones desactivadas por el usuario en el sistema: no se envía nada")
            return false
        }
        if (!messagesRepository.areNotificationsEnabled()) {
            Log.d(TAG, "Mensajes motivacionales desactivados en los ajustes de la app")
            return false
        }
        return true
    }

    /**
     * Show notification for goal progress milestone.
     */
    suspend fun showGoalProgressNotification(progressPercent: Int, goalTitle: String) {
        try {
            if (!ensureCanNotify()) return
            val message = messagesRepository.getGoalProgressMessage(progressPercent, goalTitle)
                ?: resolveMessage(null)

            val contextInfo = "goal_${progressPercent}%"
            messagesRepository.markMessageAsShown(message.id, contextInfo)

            // Customize title based on progress
            val title = when {
                progressPercent >= 100 -> "🎉 ¡Meta completada: $goalTitle!"
                progressPercent >= 75 -> "🚀 ¡75% completado: $goalTitle!"
                progressPercent >= 50 -> "💪 ¡Mitad del camino: $goalTitle!"
                progressPercent >= 25 -> "🌟 ¡25% completado: $goalTitle!"
                else -> "📈 Progreso en: $goalTitle"
            }

            showNotificationInternal(
                message = message,
                notificationId = NOTIFICATION_ID_GOAL_PROGRESS,
                customTitle = title
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing goal progress notification", e)
        }
    }
    
    /**
     * Show notification for streak celebration.
     */
    suspend fun showStreakNotification(streakDays: Int) {
        try {
            if (!ensureCanNotify()) return
            val message = messagesRepository.getStreakMessage(streakDays)
                ?: resolveMessage(null)

            val contextInfo = "streak_${streakDays}_days"
            messagesRepository.markMessageAsShown(message.id, contextInfo)

            val title = when {
                streakDays >= 100 -> "🔥🔥🔥 ¡$streakDays días de racha! ¡Legendario!"
                streakDays >= 30 -> "🔥🔥 ¡$streakDays días de racha! ¡Increíble!"
                streakDays >= 7 -> "🔥 ¡$streakDays días de racha! ¡Sigue así!"
                else -> "🔥 ¡$streakDays días de racha!"
            }

            showNotificationInternal(
                message = message,
                notificationId = NOTIFICATION_ID_STREAK,
                customTitle = title
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing streak notification", e)
        }
    }
    
    /**
     * Show notification for user comeback (returning after inactivity).
     */
    suspend fun showComebackNotification() {
        try {
            if (!ensureCanNotify()) return
            val message = resolveMessage(MessageCategory.COMEBACK)
            messagesRepository.markMessageAsShown(message.id, "comeback")

            showNotificationInternal(
                message = message,
                notificationId = NOTIFICATION_ID_COMEBACK,
                customTitle = "🔄 ¡Bienvenido de vuelta!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing comeback notification", e)
        }
    }
    
    /**
     * Show morning message notification.
     *
     * BUG CORREGIDO: si no había ningún mensaje de la categoría MORNING sin
     * mostrar recientemente, `getContextualMessage` devolvía null y este método
     * salía sin hacer nada ni registrar nada. El usuario simplemente no
     * recibía su mensaje de la mañana y no había forma de saber por qué.
     */
    suspend fun showMorningMessage() {
        try {
            if (!ensureCanNotify()) return
            val message = resolveMessage(MessageCategory.MORNING)
            messagesRepository.markMessageAsShown(message.id, "morning")
            showNotificationInternal(message, NOTIFICATION_ID_MOTIVATIONAL)
            Log.d(TAG, "Mensaje de la mañana enviado: ${message.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing morning message", e)
        }
    }

    /**
     * Show evening message notification.
     */
    suspend fun showEveningMessage() {
        try {
            if (!ensureCanNotify()) return
            val message = resolveMessage(MessageCategory.EVENING)
            messagesRepository.markMessageAsShown(message.id, "evening")
            showNotificationInternal(message, NOTIFICATION_ID_MOTIVATIONAL)
            Log.d(TAG, "Mensaje de la noche enviado: ${message.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing evening message", e)
        }
    }
    
    /**
     * Handle "Love" action from notification.
     */
    fun handleLoveAction(messageId: String) {
        scope.launch {
            try {
                messagesRepository.loveMessage(messageId)
                Log.d(TAG, "Message loved: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling love action", e)
            }
        }
    }
    
    /**
     * Handle "Another Message" action from notification.
     */
    fun handleAnotherMessageAction(messageId: String) {
        scope.launch {
            try {
                messagesRepository.recordAnotherMessageRequested(messageId)

                // El usuario lo ha pedido explícitamente pulsando el botón, así
                // que no se aplica la ventana horaria: antes, pedir "otro
                // mensaje" a las 22:05 no devolvía nada y parecía que el botón
                // estaba roto.
                showMotivationalNotification(respectWindow = false)
                
                Log.d(TAG, "Another message requested after: $messageId")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling another message action", e)
            }
        }
    }
    
    /**
     * Internal method to build and show the notification.
     *
     * Aquí es donde el mensaje deja de ser genérico: se sustituyen los
     * marcadores por datos reales del usuario, el título le habla por su nombre
     * y se añade una línea de contexto con su racha o su tiempo de pantalla.
     */
    private suspend fun showNotificationInternal(
        message: MotivationalMessage,
        notificationId: Int,
        customTitle: String? = null
    ) {
        try {
            val userContext = runCatching { personalizer.loadUserContext() }
                .getOrElse {
                    Log.w(TAG, "No se pudo cargar el contexto del usuario", it)
                    MessagePersonalizer.emptyContext()
                }

            val personalizedContent = personalizer.applyPlaceholders(message.content, userContext)
            val contextLine = personalizer.buildContextLine(userContext, message.category)
            val bigText = if (contextLine != null) {
                "$personalizedContent\n\n$contextLine"
            } else {
                personalizedContent
            }

            val baseTitle = customTitle ?: buildTitle(message)
            val title = personalizer.buildTitle(baseTitle, userContext)

            // Content intent - opens app
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("message_id", message.id)
                putExtra("source", "notification")
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Love
            val loveIntent = Intent(context, MotivationalNotificationReceiver::class.java).apply {
                action = MotivationalNotificationReceiver.ACTION_LOVE
                putExtra(MotivationalNotificationReceiver.EXTRA_MESSAGE_ID, message.id)
            }
            val lovePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_LOVE,
                loveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Another message
            val anotherIntent = Intent(context, MotivationalNotificationReceiver::class.java).apply {
                action = MotivationalNotificationReceiver.ACTION_ANOTHER
                putExtra(MotivationalNotificationReceiver.EXTRA_MESSAGE_ID, message.id)
            }
            val anotherPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ANOTHER,
                anotherIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Action: Settings
            val settingsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "motivational_settings")
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_SETTINGS,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MOTIVATIONAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(personalizedContent)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle(title))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .addAction(0, "❤️ Me encantó", lovePendingIntent)
                .addAction(0, "🔄 Otro", anotherPendingIntent)
                .addAction(0, "⚙️", settingsPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
            
            // Show notification
            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Missing POST_NOTIFICATIONS permission", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building notification", e)
        }
    }
    
    /**
     * Build a title based on message category.
     */
    private fun buildTitle(message: MotivationalMessage): String {
        val emoji = message.emoji ?: message.category.emoji
        val categoryName = when (message.category) {
            MessageCategory.MORNING -> "Buenos días"
            MessageCategory.EVENING -> "Buenas noches"
            MessageCategory.ACHIEVEMENT -> "¡Logro!"
            MessageCategory.FOCUS -> "Momento de enfoque"
            MessageCategory.GRATITUDE -> "Momento de gratitud"
            MessageCategory.CHALLENGE -> "¡Ánimo!"
            MessageCategory.STREAK -> "Tu racha"
            MessageCategory.CELEBRATION -> "¡A celebrar!"
            MessageCategory.MOTIVATION -> "Motivación"
            MessageCategory.PRODUCTIVITY -> "Productividad"
            MessageCategory.MINDFULNESS -> "Mindfulness"
            MessageCategory.SELF_CARE -> "Cuídate"
            MessageCategory.GROWTH -> "Crecimiento"
            MessageCategory.RESILIENCE -> "Resiliencia"
            MessageCategory.WEEKEND -> "¡Fin de semana!"
            MessageCategory.MONDAY -> "¡Nuevo lunes!"
            MessageCategory.MILESTONE -> "¡Hito alcanzado!"
            MessageCategory.COMEBACK -> "¡De vuelta!"
        }
        return "$emoji $categoryName"
    }
    
    /**
     * Cancel all motivational notifications.
     */
    fun cancelAllMotivationalNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_MOTIVATIONAL)
        notificationManager.cancel(NOTIFICATION_ID_GOAL_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_STREAK)
        notificationManager.cancel(NOTIFICATION_ID_COMEBACK)
    }
}

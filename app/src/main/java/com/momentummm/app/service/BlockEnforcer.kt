package com.momentummm.app.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.momentummm.app.accessibility.MomentumAccessibilityService
import com.momentummm.app.ui.AppBlockedActivity
import com.momentummm.app.ui.overlay.AppBlockOverlayService
import com.momentummm.app.util.AccessibilityUtils

/**
 * Ejecuta el bloqueo de una app por la vía más fiable disponible.
 *
 * ─── POR QUÉ EXISTE ESTA CLASE ────────────────────────────────────────────
 * El bloqueo por límite de tiempo no funcionaba porque
 * `AppMonitoringService.blockApp()` hacía, en la práctica:
 *
 *     context.startActivity(AppBlockedActivity)   // desde un Service
 *
 * Desde Android 10 (API 29) el sistema prohíbe que un servicio en segundo
 * plano lance actividades ("background activity launch"). La llamada **falla
 * en silencio**: el usuario superaba el límite y no pasaba absolutamente
 * nada. En Android 14+ la restricción es aún más estricta.
 *
 * Mientras tanto, `AppBlockOverlayService` —que sí es una vía válida— existía
 * en el proyecto pero nunca se llamaba desde ningún sitio.
 *
 * ─── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *  1. **Overlay** (`TYPE_APPLICATION_OVERLAY`): no está sujeto a las
 *     restricciones de lanzamiento en segundo plano. Es la vía principal.
 *  2. **Servicio de accesibilidad**: además de tapar la app, saca al usuario
 *     con GLOBAL_ACTION_HOME. Está exento de las restricciones y es
 *     instantáneo.
 *  3. **Activity** (último recurso): sólo funciona si la app está en primer
 *     plano o tiene permiso de superposición.
 *  4. Si no hay ninguna vía utilizable, se avisa al usuario con una
 *     notificación que abre los ajustes del permiso que falta: sin overlay ni
 *     accesibilidad, ningún bloqueo es posible en Android moderno.
 */
object BlockEnforcer {

    private const val TAG = "BlockEnforcer"

    private const val CHANNEL_ID = "app_blocking_permission_channel"
    private const val NOTIFICATION_ID_PERMISSION = 2001

    /** Evita relanzar la UI de bloqueo en bucle para la misma app. */
    private const val ENFORCE_COOLDOWN_MS = 2_000L

    private val lastEnforcedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Bloquea [packageName].
     *
     * @param accessibility instancia viva del servicio de accesibilidad si la
     *   llamada procede de él; permite sacar al usuario de la app.
     * @param force ignora el cooldown (para bloqueos recién detectados).
     * @return true si se pudo aplicar alguna vía de bloqueo.
     */
    fun enforce(
        context: Context,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        reason: String? = null,
        accessibility: AccessibilityService? = null,
        force: Boolean = false
    ): Boolean {
        if (packageName.isEmpty()) return false

        val now = System.currentTimeMillis()
        if (!force) {
            val last = lastEnforcedAt[packageName] ?: 0L
            if (now - last < ENFORCE_COOLDOWN_MS) return true
        }
        lastEnforcedAt[packageName] = now

        Log.d(TAG, "Aplicando bloqueo a $packageName (límite ${dailyLimitMinutes}m) ${reason ?: ""}")

        // 1. Saca al usuario de la app si tenemos accesibilidad disponible.
        //    Es lo único que puede interrumpir la app de forma inmediata.
        var handled = false
        if (accessibility != null) {
            handled = runCatching {
                accessibility.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }.getOrElse {
                Log.e(TAG, "GLOBAL_ACTION_HOME falló", it)
                false
            }
        }

        // 2. Overlay: la vía fiable para mostrar la pantalla de bloqueo.
        if (canDrawOverlays(context)) {
            val shown = showOverlay(context, packageName, appName, dailyLimitMinutes, reason)
            if (shown) return true
        }

        // 3. Sin overlay pero con accesibilidad: la Activity sí puede lanzarse.
        if (accessibility != null) {
            val started = startBlockedActivity(context, packageName, appName, dailyLimitMinutes, reason)
            if (started) return true
        }

        // 4. Último recurso. Puede fallar silenciosamente por las
        //    restricciones de lanzamiento en segundo plano.
        val started = startBlockedActivity(context, packageName, appName, dailyLimitMinutes, reason)
        if (started) return true

        if (!handled) {
            notifyMissingPermission(context)
        }
        return handled
    }

    /**
     * true si existe alguna vía capaz de bloquear. Útil para avisar al usuario
     * antes de que confíe en un límite que no se puede aplicar.
     */
    fun canEnforce(context: Context): Boolean =
        canDrawOverlays(context) || AccessibilityUtils.isAccessibilityServiceEnabled(
            context,
            MomentumAccessibilityService::class.java
        )

    private fun canDrawOverlays(context: Context): Boolean =
        runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    private fun showOverlay(
        context: Context,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        reason: String?
    ): Boolean {
        return try {
            val intent = Intent(context, AppBlockOverlayService::class.java).apply {
                putExtra(AppBlockOverlayService.EXTRA_BLOCKED_PACKAGE, packageName)
                putExtra(AppBlockOverlayService.EXTRA_BLOCKED_APP_NAME, appName)
                putExtra(AppBlockOverlayService.EXTRA_DAILY_LIMIT, dailyLimitMinutes)
                putExtra(AppBlockOverlayService.EXTRA_REASON, reason)
            }
            context.startForegroundService(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo mostrar el overlay de bloqueo", e)
            false
        }
    }

    private fun startBlockedActivity(
        context: Context,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        reason: String?
    ): Boolean {
        return try {
            AppBlockedActivity.start(
                context = context,
                appName = appName,
                dailyLimit = dailyLimitMinutes,
                customReason = reason,
                blockedPackage = packageName
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo abrir AppBlockedActivity", e)
            false
        }
    }

    /**
     * Sin permiso de superposición ni accesibilidad el bloqueo es imposible.
     * En vez de fallar en silencio —el comportamiento anterior— se le explica
     * al usuario y se le lleva al ajuste correspondiente.
     */
    private fun notifyMissingPermission(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Permisos de bloqueo",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos cuando falta un permiso necesario para bloquear apps"
                }
            )

            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("No se pudo bloquear la app")
                .setContentText("Activa \"Mostrar sobre otras apps\" para que el límite funcione")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Momentum necesita el permiso \"Mostrar sobre otras apps\" " +
                            "(o el servicio de accesibilidad) para poder bloquear " +
                            "aplicaciones cuando alcanzas tu límite. Toca para activarlo."
                    )
                )
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_ID_PERMISSION, notification)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo notificar el permiso faltante", e)
        }
    }
}

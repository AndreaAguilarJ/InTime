package com.momentummm.app.util

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.momentummm.app.accessibility.MomentumAccessibilityService

/**
 * Responde a dos preguntas de las que depende TODO el bloqueo de apps:
 *
 *  1. ¿Tenemos los permisos necesarios para bloquear?
 *  2. ¿Es este el momento de bloquear (pantalla encendida y desbloqueada)?
 *
 * ─── POR QUÉ EXISTE ──────────────────────────────────────────────────────
 * El bloqueo dependía de dos permisos que se daban por concedidos:
 *
 * - **Acceso al uso de aplicaciones** (`PACKAGE_USAGE_STATS`): sin él
 *   `queryEvents` devuelve una lista vacía, así que el uso diario siempre es
 *   0, `uso >= límite` nunca es cierto y **no se bloquea nada, nunca**. No
 *   había ninguna comprobación: el fallo era total y silencioso.
 * - **Mostrar sobre otras apps** (`SYSTEM_ALERT_WINDOW`): sin él no se puede
 *   dibujar el overlay de bloqueo.
 *
 * Y de una condición de contexto que tampoco se comprobaba: si la pantalla
 * está apagada o bloqueada, tapar una app no sirve de nada. Peor: el overlay
 * aparecía **sobre la pantalla de bloqueo** y, al llevar `FLAG_KEEP_SCREEN_ON`,
 * dejaba el teléfono encendido indefinidamente.
 */
object BlockingCapabilities {

    private const val TAG = "BlockingCapabilities"

    /**
     * true si la app puede leer estadísticas de uso. Es el permiso del que
     * depende que el límite diario se calcule; sin él el bloqueo es imposible.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            // unsafeCheckOpNoThrow exige API 29 y el minimo de la app es 26: en Android 8 y 9
            // esto lanzaba NoSuchMethodError justo al comprobar si el bloqueo puede
            // funcionar, que es el permiso del que depende la funcion principal de la app.
            // Antes de 29 el metodo equivalente es checkOpNoThrow (obsoleto pero valido).
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> true
                // MODE_DEFAULT significa "decide el permiso declarado en el
                // manifiesto", que para PACKAGE_USAGE_STATS es siempre denegado
                // salvo que el usuario lo conceda a mano en Ajustes.
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo comprobar el permiso de uso de apps", e)
            false
        }
    }

    /** true si podemos dibujar el overlay de bloqueo. */
    fun canDrawOverlays(context: Context): Boolean =
        runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    /** true si el servicio de accesibilidad de Momentum está activo. */
    fun isAccessibilityEnabled(context: Context): Boolean =
        AccessibilityUtils.isAccessibilityServiceEnabled(
            context,
            MomentumAccessibilityService::class.java
        )

    /**
     * true si existe alguna vía para tapar una app. Sin overlay ni
     * accesibilidad, Android moderno no permite interponerse: conviene
     * decírselo al usuario antes de que confíe en un límite que no se aplicará.
     */
    fun canEnforceBlocks(context: Context): Boolean =
        canDrawOverlays(context) || isAccessibilityEnabled(context)

    /**
     * Motivo por el que el bloqueo no puede funcionar, o null si todo está en
     * orden. Se usa para avisar al usuario en vez de fallar en silencio.
     */
    fun missingRequirement(context: Context): MissingRequirement? = when {
        !hasUsageStatsPermission(context) -> MissingRequirement.USAGE_STATS
        !canEnforceBlocks(context) -> MissingRequirement.OVERLAY
        else -> null
    }

    enum class MissingRequirement { USAGE_STATS, OVERLAY }

    /**
     * true si tiene sentido mostrar una pantalla de bloqueo ahora mismo:
     * la pantalla está encendida y no está en el bloqueo de pantalla.
     *
     * Con la pantalla apagada el usuario no está usando la app (el sistema
     * puede seguir reportándola como "en primer plano" durante un rato), así
     * que bloquear sólo gastaría batería y dejaría el overlay flotando sobre
     * la pantalla de bloqueo.
     */
    fun isScreenUsable(context: Context): Boolean {
        return try {
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val interactive = power?.isInteractive ?: true
            if (!interactive) return false

            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = keyguard?.isKeyguardLocked ?: false
            !locked
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo determinar el estado de la pantalla", e)
            true // Ante la duda, se permite bloquear.
        }
    }
}

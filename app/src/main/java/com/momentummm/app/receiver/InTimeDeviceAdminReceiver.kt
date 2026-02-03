package com.momentummm.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.momentummm.app.R

/**
 * DeviceAdminReceiver para prevenir la desinstalación de InTime
 * mientras hay bloqueos activos.
 * 
 * Esta característica es solicitada por usuarios: 
 * "I would much rather have it to block myself from deleting it"
 * 
 * El Device Admin permite:
 * - Prevenir la desinstalación de la app
 * - Mostrar advertencia cuando el usuario intenta desinstalar
 * - Requerir desactivación manual del admin antes de desinstalar
 */
class InTimeDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "InTimeDeviceAdmin"

        /**
         * Obtiene el ComponentName del DeviceAdminReceiver
         */
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, InTimeDeviceAdminReceiver::class.java)
        }

        /**
         * Verifica si el Device Admin está habilitado
         */
        fun isAdminActive(context: Context): Boolean {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as android.app.admin.DevicePolicyManager
            return devicePolicyManager.isAdminActive(getComponentName(context))
        }

        /**
         * Solicita activación del Device Admin
         */
        fun requestAdminActivation(context: Context) {
            if (isAdminActive(context)) {
                Log.d(TAG, "Device Admin ya está activo")
                return
            }

            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.device_admin_explanation)
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        /**
         * Desactiva el Device Admin (permite desinstalación)
         */
        fun removeAdmin(context: Context) {
            if (!isAdminActive(context)) {
                Log.d(TAG, "Device Admin no está activo")
                return
            }

            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as android.app.admin.DevicePolicyManager
            devicePolicyManager.removeActiveAdmin(getComponentName(context))
            Log.d(TAG, "Device Admin desactivado")
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin habilitado - Protección anti-desinstalación activa")
        Toast.makeText(
            context,
            context.getString(R.string.device_admin_enabled_toast),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Solicitud de desactivación de Device Admin")
        // Mensaje de advertencia cuando el usuario intenta desactivar el admin
        return context.getString(R.string.device_admin_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin deshabilitado - Protección anti-desinstalación desactivada")
        Toast.makeText(
            context,
            context.getString(R.string.device_admin_disabled_toast),
            Toast.LENGTH_SHORT
        ).show()
    }
}

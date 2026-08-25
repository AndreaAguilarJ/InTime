package com.momentummm.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/** Enlaces legales externos verificados de la app. */
object AppLinks {

    private const val TAG = "AppLinks"

    const val REPOSITORY_URL = "https://github.com/AndreaAguilarJ/InTime"

    /** Documento público verificado el 24/08/2026. */
    const val PRIVACY_POLICY_URL = "$REPOSITORY_URL/blob/main/PRIVACY_POLICY.md"

    /**
     * No existe todavía un documento de términos en el repositorio ni un asset
     * local. Una cadena vacía representa esa ausencia de forma explícita; no se
     * dirige al usuario a una URL inventada.
     */
    const val TERMS_URL = ""

    /** Permite a la UI deshabilitar un enlace que aún no está publicado. */
    fun isConfigured(url: String): Boolean {
        if (url.isBlank()) return false
        val parsed = Uri.parse(url)
        return parsed.scheme == "https" && !parsed.host.isNullOrBlank()
    }

    /**
     * Abre [url] y devuelve si Android pudo entregar el intent.
     *
     * La firma sigue siendo compatible con quienes ignoran el resultado, pero
     * una UI puede usar el Boolean para mostrar un estado de error real.
     */
    fun open(context: Context, url: String, errorMessage: String): Boolean {
        if (!isConfigured(url)) {
            Log.w(TAG, "Enlace legal no configurado")
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            return false
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Log.e(TAG, "No se pudo abrir el enlace legal: $url", error)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            false
        }
    }
}

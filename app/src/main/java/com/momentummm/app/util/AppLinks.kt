package com.momentummm.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Enlaces externos de la app, en un solo sitio.
 *
 * ─── POR QUÉ EXISTE ──────────────────────────────────────────────────────
 * Los botones "Política de privacidad" y "Términos del servicio" de la pantalla
 * "Acerca de" tenían `onClick = { /* TODO */ }`: se podían pulsar, mostraban
 * flecha de navegación y no hacían nada. Además Google Play **exige** un enlace
 * accesible a la política de privacidad para cualquier app que recoja datos
 * personales, y esta recoge estadísticas de uso y datos de cuenta.
 *
 * ⚠️ [PRIVACY_POLICY_URL] y [TERMS_URL] apuntan al repositorio del proyecto
 * como destino provisional. Antes de publicar hay que sustituirlos por las URL
 * de los documentos reales.
 */
object AppLinks {

    private const val TAG = "AppLinks"

    const val REPOSITORY_URL = "https://github.com/AndreaAguilarJ/InTime"

    /** TODO(producto): sustituir por la URL de la política de privacidad real. */
    const val PRIVACY_POLICY_URL = "$REPOSITORY_URL/blob/main/PRIVACY.md"

    /** TODO(producto): sustituir por la URL de los términos reales. */
    const val TERMS_URL = "$REPOSITORY_URL/blob/main/TERMS.md"

    /**
     * Abre [url] en el navegador.
     *
     * Si no hay ninguna app capaz de abrirla se avisa al usuario en lugar de
     * fallar en silencio, que es lo que hacía la pantalla anterior.
     */
    fun open(context: Context, url: String, errorMessage: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) {
            Log.w(TAG, "No se pudo abrir $url")
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}

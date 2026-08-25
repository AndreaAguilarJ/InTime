package com.momentummm.app.util

import com.momentummm.app.R
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object NetworkUtils {
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}

object ErrorHandler {
    
    fun handleError(
        error: Throwable,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        context: Context
    ) {
        val message = getDetailedErrorMessage(error, context)
        
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    private fun getDetailedErrorMessage(error: Throwable, context: Context): String {
        val errorMsg = error.message?.lowercase() ?: ""
        // Los mensajes se resuelven desde recursos para que aparezcan en el idioma
        // del usuario; antes estaban fijos en español y todos los idiomas veían
        // el error en español.
        val res = errorMessageRes(errorMsg, NetworkUtils.isNetworkAvailable(context))
        return context.getString(res)
    }

    /**
     * Mapeo puro de un mensaje de error (ya en minúsculas) a un recurso de texto.
     * Se extrae de [getDetailedErrorMessage] para poder probarlo sin un Context:
     * un reordenamiento de las ramas o una colisión de palabras clave mostraría el
     * error equivocado, y esto es un fallo invisible al compilar.
     *
     * @param errorMsgLower el mensaje de la excepción en minúsculas ("" si es null)
     * @param networkAvailable si hay conexión; si no la hay, gana sobre todo lo demás
     */
    @androidx.annotation.StringRes
    fun errorMessageRes(errorMsgLower: String, networkAvailable: Boolean): Int = when {
        !networkAvailable -> R.string.err_no_internet
        errorMsgLower.contains("invalid credentials") || errorMsgLower.contains("invalid_credentials") -> R.string.err_invalid_credentials
        errorMsgLower.contains("user_not_found") || errorMsgLower.contains("user not found") -> R.string.err_user_not_found
        errorMsgLower.contains("user_already_exists") || errorMsgLower.contains("already exists") -> R.string.err_user_exists
        errorMsgLower.contains("password") && errorMsgLower.contains("invalid") -> R.string.err_password_invalid
        errorMsgLower.contains("email") && errorMsgLower.contains("invalid") -> R.string.err_email_invalid
        errorMsgLower.contains("rate_limit") || errorMsgLower.contains("too many") -> R.string.err_rate_limit
        errorMsgLower.contains("authentication") -> R.string.err_authentication
        errorMsgLower.contains("network") -> R.string.err_network
        errorMsgLower.contains("timeout") -> R.string.err_timeout
        else -> R.string.err_unexpected
    }
    
    fun getErrorMessage(error: Throwable, context: Context): String {
        return getDetailedErrorMessage(error, context)
    }
}
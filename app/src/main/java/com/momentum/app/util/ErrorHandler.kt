package com.momentummm.app.util

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
        val message = when {
            !NetworkUtils.isNetworkAvailable(context) -> 
                "Sin conexión a internet. Verifica tu conexión y vuelve a intentar."
            error.message?.contains("authentication", ignoreCase = true) == true ->
                "Error de autenticación. Verifica tus credenciales."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Error de red. Verifica tu conexión a internet."
            error.message?.contains("timeout", ignoreCase = true) == true ->
                "La operación tardó demasiado. Inténtalo de nuevo."
            else -> 
                "Ha ocurrido un error inesperado. Inténtalo de nuevo."
        }
        
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    fun getErrorMessage(error: Throwable, context: Context): String {
        return when {
            !NetworkUtils.isNetworkAvailable(context) -> 
                "Sin conexión a internet"
            error.message?.contains("authentication", ignoreCase = true) == true ->
                "Error de autenticación"
            error.message?.contains("network", ignoreCase = true) == true ->
                "Error de red"
            error.message?.contains("timeout", ignoreCase = true) == true ->
                "Tiempo de espera agotado"
            else -> 
                "Error inesperado"
        }
    }
}
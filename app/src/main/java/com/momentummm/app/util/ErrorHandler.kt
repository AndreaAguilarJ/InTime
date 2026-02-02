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
        
        return when {
            !NetworkUtils.isNetworkAvailable(context) -> 
                "Sin conexión a internet. Verifica tu conexión y vuelve a intentar."
            errorMsg.contains("invalid credentials") || errorMsg.contains("invalid_credentials") ->
                "Email o contraseña incorrectos"
            errorMsg.contains("user_not_found") || errorMsg.contains("user not found") ->
                "No existe una cuenta con este email"
            errorMsg.contains("user_already_exists") || errorMsg.contains("already exists") ->
                "Ya existe una cuenta con este email"
            errorMsg.contains("password") && errorMsg.contains("invalid") ->
                "La contraseña es incorrecta"
            errorMsg.contains("email") && errorMsg.contains("invalid") ->
                "El formato del email no es válido"
            errorMsg.contains("rate_limit") || errorMsg.contains("too many") ->
                "Demasiados intentos. Espera unos minutos"
            errorMsg.contains("authentication") ->
                "Error de autenticación. Verifica tus credenciales."
            errorMsg.contains("network") ->
                "Error de red. Verifica tu conexión a internet."
            errorMsg.contains("timeout") ->
                "La operación tardó demasiado. Inténtalo de nuevo."
            else -> 
                "Ha ocurrido un error inesperado. Inténtalo de nuevo."
        }
    }
    
    fun getErrorMessage(error: Throwable, context: Context): String {
        return getDetailedErrorMessage(error, context)
    }
}
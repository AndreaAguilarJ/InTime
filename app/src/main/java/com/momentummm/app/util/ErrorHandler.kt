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
        val res = when {
            !NetworkUtils.isNetworkAvailable(context) -> R.string.err_no_internet
            errorMsg.contains("invalid credentials") || errorMsg.contains("invalid_credentials") -> R.string.err_invalid_credentials
            errorMsg.contains("user_not_found") || errorMsg.contains("user not found") -> R.string.err_user_not_found
            errorMsg.contains("user_already_exists") || errorMsg.contains("already exists") -> R.string.err_user_exists
            errorMsg.contains("password") && errorMsg.contains("invalid") -> R.string.err_password_invalid
            errorMsg.contains("email") && errorMsg.contains("invalid") -> R.string.err_email_invalid
            errorMsg.contains("rate_limit") || errorMsg.contains("too many") -> R.string.err_rate_limit
            errorMsg.contains("authentication") -> R.string.err_authentication
            errorMsg.contains("network") -> R.string.err_network
            errorMsg.contains("timeout") -> R.string.err_timeout
            else -> R.string.err_unexpected
        }
        return context.getString(res)
    }
    
    fun getErrorMessage(error: Throwable, context: Context): String {
        return getDetailedErrorMessage(error, context)
    }
}
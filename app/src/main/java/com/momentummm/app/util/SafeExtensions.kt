package com.momentummm.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Extension functions for crash-safe operations
 * 
 * These extensions wrap common crash-prone operations with proper exception handling
 * to prevent crashes in production.
 */

private const val TAG = "SafeExtensions"

/**
 * Safely starts an activity, handling common exceptions:
 * - ActivityNotFoundException: No activity found to handle the intent
 * - SecurityException: Permission denied to start the activity
 * - Other exceptions: Logged and handled gracefully
 * 
 * @param intent The intent to start
 * @param showToastOnError Whether to show a toast message when the activity cannot be started
 * @param errorMessage Custom error message for the toast
 * @return true if the activity was started successfully, false otherwise
 */
fun Context.safeStartActivity(
    intent: Intent,
    showToastOnError: Boolean = true,
    errorMessage: String = "No se pudo abrir esta aplicación"
): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No activity found for intent: ${intent.action}", e)
        if (showToastOnError) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "Security exception starting activity", e)
        if (showToastOnError) {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
        false
    } catch (e: Exception) {
        Log.e(TAG, "Error starting activity", e)
        if (showToastOnError) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
        false
    }
}

/**
 * Safely navigates to a destination, handling navigation exceptions.
 * 
 * Checks that the NavController is in a valid state before navigating,
 * and catches any IllegalArgumentException or IllegalStateException.
 * 
 * @param route The destination route
 * @param builder Optional navigation options builder
 * @return true if navigation was successful, false otherwise
 */
fun NavController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    return try {
        // Check if the current back stack entry is in a valid state
        val currentEntry = currentBackStackEntry
        val isValidState = currentEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        
        if (isValidState || currentEntry == null) {
            navigate(route, builder)
            true
        } else {
            Log.w(TAG, "Cannot navigate, current state is not valid: ${currentEntry.lifecycle.currentState}")
            false
        }
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Navigation failed - invalid route: $route", e)
        false
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Navigation failed - invalid state", e)
        false
    } catch (e: Exception) {
        Log.e(TAG, "Navigation failed unexpectedly", e)
        false
    }
}

/**
 * Safely pops the back stack, checking if there's a previous entry.
 * 
 * @return true if the back stack was popped successfully, false otherwise
 */
fun NavController.safePopBackStack(): Boolean {
    return try {
        if (previousBackStackEntry != null) {
            popBackStack()
        } else {
            Log.w(TAG, "Cannot pop back stack - no previous entry")
            false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error popping back stack", e)
        false
    }
}

/**
 * Extension to safely parse a color string to an Int.
 * Returns the default color if parsing fails.
 * 
 * @param defaultColor The default color to return if parsing fails
 * @return The parsed color or the default color
 */
fun String.parseColorSafe(defaultColor: Int): Int {
    return try {
        android.graphics.Color.parseColor(this)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Failed to parse color: $this", e)
        defaultColor
    } catch (e: Exception) {
        Log.w(TAG, "Unexpected error parsing color: $this", e)
        defaultColor
    }
}

/**
 * Extension to safely parse a color string to a Compose Color.
 * Returns the default color if parsing fails.
 * 
 * @param defaultColor The default Compose Color to return if parsing fails
 * @return The parsed Color or the default Color
 */
fun String.toComposeColorSafe(defaultColor: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Failed to parse color: $this", e)
        defaultColor
    } catch (e: Exception) {
        Log.w(TAG, "Unexpected error parsing color: $this", e)
        defaultColor
    }
}

/**
 * Safely gets a system service with proper null handling and type safety.
 * 
 * @param T The type of the system service
 * @param name The name of the system service
 * @return The system service or null if not available
 */
inline fun <reified T> Context.getSystemServiceSafe(name: String): T? {
    return try {
        getSystemService(name) as? T
    } catch (e: Exception) {
        Log.e("SafeExtensions", "Failed to get system service: $name", e)
        null
    }
}

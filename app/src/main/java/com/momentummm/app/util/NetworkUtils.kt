package com.momentummm.app.util

import android.util.Log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.math.pow

private const val TAG = "NetworkUtils"

/**
 * Constantes de configuración de red
 */
object NetworkConfig {
    const val DEFAULT_TIMEOUT_MS = 30_000L
    const val SHORT_TIMEOUT_MS = 10_000L
    const val LONG_TIMEOUT_MS = 60_000L
    
    const val DEFAULT_MAX_RETRIES = 3
    const val DEFAULT_INITIAL_DELAY_MS = 1000L
    const val DEFAULT_MAX_DELAY_MS = 10_000L
}

/**
 * Ejecuta un bloque de código con timeout y manejo de errores robusto.
 * 
 * @param timeoutMs Tiempo máximo de espera en milisegundos
 * @param onTimeout Acción a ejecutar si ocurre timeout (opcional)
 * @param block Bloque de código a ejecutar
 * @return El resultado del bloque o null si falla
 */
suspend fun <T> withTimeoutSafe(
    timeoutMs: Long = NetworkConfig.DEFAULT_TIMEOUT_MS,
    onTimeout: (() -> Unit)? = null,
    block: suspend () -> T
): T? {
    return try {
        withTimeout(timeoutMs) {
            block()
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Operation timed out after ${timeoutMs}ms")
        onTimeout?.invoke()
        null
    } catch (e: Exception) {
        Log.e(TAG, "Operation failed: ${e.message}", e)
        null
    }
}

/**
 * Ejecuta un bloque de código con reintentos exponenciales.
 * 
 * @param maxRetries Número máximo de reintentos
 * @param initialDelayMs Delay inicial entre reintentos
 * @param maxDelayMs Delay máximo entre reintentos
 * @param shouldRetry Función que determina si se debe reintentar basado en la excepción
 * @param onRetry Callback opcional que se llama antes de cada reintento
 * @param block Bloque de código a ejecutar
 * @return El resultado del bloque
 * @throws Exception si todos los reintentos fallan
 */
suspend fun <T> retryWithExponentialBackoff(
    maxRetries: Int = NetworkConfig.DEFAULT_MAX_RETRIES,
    initialDelayMs: Long = NetworkConfig.DEFAULT_INITIAL_DELAY_MS,
    maxDelayMs: Long = NetworkConfig.DEFAULT_MAX_DELAY_MS,
    shouldRetry: (Exception) -> Boolean = { true },
    onRetry: ((attempt: Int, exception: Exception) -> Unit)? = null,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            
            if (!shouldRetry(e) || attempt == maxRetries - 1) {
                throw e
            }
            
            Log.w(TAG, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms: ${e.message}")
            onRetry?.invoke(attempt + 1, e)
            
            delay(currentDelay)
            currentDelay = (currentDelay * 2.0.pow(1.0)).toLong().coerceAtMost(maxDelayMs)
        }
    }
    
    throw lastException ?: IllegalStateException("Retry failed without exception")
}

/**
 * Combina timeout con reintentos exponenciales.
 * 
 * @param timeoutMs Tiempo máximo por intento
 * @param maxRetries Número máximo de reintentos
 * @param block Bloque de código a ejecutar
 * @return El resultado del bloque
 */
suspend fun <T> withTimeoutAndRetry(
    timeoutMs: Long = NetworkConfig.DEFAULT_TIMEOUT_MS,
    maxRetries: Int = NetworkConfig.DEFAULT_MAX_RETRIES,
    block: suspend () -> T
): T {
    return retryWithExponentialBackoff(
        maxRetries = maxRetries,
        shouldRetry = { e ->
            // Reintentar en timeouts y errores de red temporales
            e is TimeoutCancellationException ||
            e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("connection", ignoreCase = true) == true ||
            e.message?.contains("network", ignoreCase = true) == true
        }
    ) {
        withTimeout(timeoutMs) {
            block()
        }
    }
}

/**
 * Ejecuta una operación de red de forma segura, retornando Result.
 * 
 * @param timeoutMs Tiempo máximo de espera
 * @param block Bloque de código a ejecutar
 * @return Result con el valor o el error
 */
suspend fun <T> safeNetworkCall(
    timeoutMs: Long = NetworkConfig.DEFAULT_TIMEOUT_MS,
    block: suspend () -> T
): Result<T> {
    return try {
        withTimeout(timeoutMs) {
            Result.success(block())
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Network call timed out after ${timeoutMs}ms")
        Result.failure(NetworkTimeoutException("Connection timed out. Please check your internet connection."))
    } catch (e: Exception) {
        Log.e(TAG, "Network call failed: ${e.message}", e)
        Result.failure(e)
    }
}

/**
 * Excepción personalizada para timeouts de red
 */
class NetworkTimeoutException(message: String) : Exception(message)

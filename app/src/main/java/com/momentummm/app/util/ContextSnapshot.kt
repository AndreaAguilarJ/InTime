package com.momentummm.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Lectura del contexto físico del dispositivo: red Wi-Fi actual y última
 * ubicación conocida.
 *
 * POR QUÉ EXISTE
 *
 * `ContextBlockingService` sabía leer el SSID, pero la lógica era privada del
 * servicio. La pantalla necesita esos mismos datos para poder CREAR una regla
 * de ubicación o de Wi-Fi —algo que antes era imposible: el diálogo sólo sabía
 * crear reglas de horario aunque la sección prometiera las tres.
 *
 * Duplicar la lectura del SSID en la pantalla habría sido peor: es un caso
 * lleno de detalles por versión de Android, y dos copias divergen.
 */
object ContextSnapshot {

    private const val TAG = "ContextSnapshot"

    /**
     * Nombre de la red Wi-Fi conectada, o `null` si no hay Wi-Fi o el sistema
     * lo censura por falta de permiso de ubicación.
     */
    fun currentSsid(context: Context): String? {
        val fromCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ssidFromCapabilities(context)
        } else {
            null
        }
        return fromCapabilities ?: ssidFromWifiManager(context)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun ssidFromCapabilities(context: Context): String? {
        return try {
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
                ?: return null
            val network = connectivity.activeNetwork ?: return null
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
            val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return null
            sanitizeSsid(wifiInfo.ssid)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer el SSID desde NetworkCapabilities", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun ssidFromWifiManager(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            sanitizeSsid(wifiManager.connectionInfo?.ssid)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer el SSID desde WifiManager", e)
            null
        }
    }

    /**
     * Limpia el SSID que devuelve el sistema.
     *
     * Llega entre comillas y, sin permiso de ubicación, como el literal
     * `<unknown ssid>`; tratarlo como nombre de red crearía una regla que nunca
     * coincide.
     */
    fun sanitizeSsid(raw: String?): String? {
        val ssid = raw?.trim()?.removeSurrounding("\"") ?: return null
        if (ssid.isBlank()) return null
        if (ssid.equals(WifiManager.UNKNOWN_SSID, ignoreCase = true)) return null
        if (ssid.contains("unknown ssid", ignoreCase = true)) return null
        return ssid
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Última ubicación conocida como par (latitud, longitud), o `null`.
     *
     * Se usa [LocationManager] y no una petición activa a propósito: aquí sólo
     * hace falta un punto para anclar una regla mientras el usuario tiene el
     * diálogo abierto, y pedir una localización nueva tardaría segundos.
     */
    @SuppressLint("MissingPermission")
    fun lastKnownLocation(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            // getProviders(true) = sólo los proveedores habilitados. Se recorre
            // la lista y se toma la lectura más reciente en vez de fijar GPS,
            // que en interiores suele no tener ninguna.
            val best: Location = manager.getProviders(true)
                .mapNotNull { provider ->
                    runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull { location -> location.time }
                ?: return null
            best.latitude to best.longitude
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la última ubicación conocida", e)
            null
        }
    }
}

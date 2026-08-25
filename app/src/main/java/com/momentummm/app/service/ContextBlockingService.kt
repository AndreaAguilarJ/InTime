package com.momentummm.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.momentummm.app.MainActivity
import com.momentummm.app.R
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.data.dao.ContextBlockRuleDao
import com.momentummm.app.data.entity.ContextBlockRule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Servicio de Bloqueo por Contexto.
 * 
 * CARACTERÍSTICAS:
 * - Detección de ubicación GPS para geofencing
 * - Detección de WiFi (ej: WiFi del trabajo)
 * - Activación automática de reglas por contexto
 * - Funciona en segundo plano
 */
@AndroidEntryPoint
class ContextBlockingService : Service() {
    
    companion object {
        private const val TAG = "ContextBlockingService"
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "context_blocking_channel"
        private const val LOCATION_UPDATE_INTERVAL = 60000L // 1 minuto
        private const val WIFI_CHECK_INTERVAL = 30000L // 30 segundos
        
        /**
         * Arranca el servicio sólo si tiene sentido.
         *
         * BUG CORREGIDO: se arrancaba sin comprobar el permiso de ubicación en
         * tiempo de ejecución. El servicio se declara con
         * `foregroundServiceType="location"`, y desde Android 14 llamar a
         * `startForeground()` con ese tipo sin el permiso concedido lanza
         * SecurityException; el servicio moría al instante y la función de
         * bloqueo por contexto aparentaba estar activa sin hacer nada.
         *
         * @return true si el servicio se arrancó.
         */
        fun startIfPossible(context: Context): Boolean {
            if (!hasLocationPermission(context)) {
                Log.w(
                    TAG,
                    "Sin permiso de ubicación: no se arranca el bloqueo por contexto"
                )
                return false
            }
            return try {
                start(context)
                true
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo arrancar el bloqueo por contexto", e)
                false
            }
        }

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun start(context: Context) {
            val intent = Intent(context, ContextBlockingService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, ContextBlockingService::class.java)
            context.stopService(intent)
        }
    }
    
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var locationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var wifiCheckJob: Job? = null
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _currentWifiSsid = MutableStateFlow<String?>(null)
    val currentWifiSsid: StateFlow<String?> = _currentWifiSsid.asStateFlow()
    
    private val _activeLocationRules = MutableStateFlow<List<ContextBlockRule>>(emptyList())
    val activeLocationRules: StateFlow<List<ContextBlockRule>> = _activeLocationRules.asStateFlow()
    
    private val _activeWifiRules = MutableStateFlow<List<ContextBlockRule>>(emptyList())
    val activeWifiRules: StateFlow<List<ContextBlockRule>> = _activeWifiRules.asStateFlow()
    
    private lateinit var contextBlockRuleDao: ContextBlockRuleDao
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Sin permiso de ubicación este servicio no puede promoverse a
        // foreground con tipo "location" en Android 14+ (SecurityException), y
        // aunque pudiera no tendría datos que leer. Mejor no arrancar.
        if (!hasLocationPermission()) {
            Log.w(TAG, "Sin permiso de ubicación: el bloqueo por contexto no puede funcionar")
            stopSelf()
            return
        }

        // CRÍTICO: Llamar startForeground inmediatamente en onCreate para Android 12+
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground in onCreate", e)
            stopSelf()
            return
        }
        
        // Inicializar database en background thread para evitar ANR
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                contextBlockRuleDao = database.contextBlockRuleDao()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing database", e)
            }
        }
        
        try {
            locationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location client", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // El servicio ya está en foreground desde onCreate
        startLocationUpdates()
        startWifiChecks()
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        wifiCheckJob?.cancel()
        serviceScope.cancel()
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission")
            return
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    checkLocationRules(location)
                }
            }
        }
        
        @Suppress("DEPRECATION")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_UPDATE_INTERVAL / 2
        }
        
        val callback = locationCallback
        if (callback == null) {
            Log.e(TAG, "Location callback is null, cannot start updates")
            return
        }
        
        try {
            locationClient?.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }
    
    private fun stopLocationUpdates() {
        try {
            locationCallback?.let { callback ->
                locationClient?.removeLocationUpdates(callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
        locationCallback = null
    }
    
    private fun startWifiChecks() {
        wifiCheckJob?.cancel()
        wifiCheckJob = serviceScope.launch {
            while (isActive) {
                checkCurrentWifi()
                delay(WIFI_CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * SSID de la red WiFi actual.
     *
     * BUG CORREGIDO: se leía con `WifiManager.getConnectionInfo()`, que está
     * obsoleto desde Android 12 y devuelve `<unknown ssid>` a las apps que no
     * son la propia app de ajustes. El resultado era que las reglas por WiFi
     * nunca coincidían: se descartaba el SSID y `_currentWifiSsid` quedaba en
     * null para siempre.
     *
     * La vía válida en API 31+ es leer el [WifiInfo] del `transportInfo` de las
     * capacidades de la red activa. Requiere permiso de ubicación (ya lo
     * comprobamos al arrancar); si no, el sistema lo devuelve censurado.
     */
    @SuppressLint("MissingPermission")
    private fun checkCurrentWifi() {
        try {
            val ssid = currentSsid()

            if (ssid != null) {
                _currentWifiSsid.value = ssid
                checkWifiRules(ssid)
            } else {
                _currentWifiSsid.value = null
                _activeWifiRules.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi", e)
        }
    }

    private fun currentSsid(): String? {
        val fromCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ssidFromCapabilities()
        } else {
            null
        }
        return fromCapabilities ?: ssidFromWifiManager()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun ssidFromCapabilities(): String? {
        return try {
            val connectivity = getSystemService(ConnectivityManager::class.java) ?: return null
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
    private fun ssidFromWifiManager(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
                ?: return null
            sanitizeSsid(wifiManager.connectionInfo?.ssid)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer el SSID desde WifiManager", e)
            null
        }
    }

    /** Quita las comillas y descarta los valores censurados por el sistema. */
    private fun sanitizeSsid(raw: String?): String? {
        val ssid = raw?.removeSurrounding("\"")?.trim()
        if (ssid.isNullOrEmpty()) return null
        if (ssid == WifiManager.UNKNOWN_SSID || ssid == "<unknown ssid>" || ssid == "0x") return null
        return ssid
    }
    
    private fun checkLocationRules(location: Location) {
        serviceScope.launch {
            try {
                // CRITICAL FIX: Verificar que el DAO está inicializado antes de usarlo
                if (!::contextBlockRuleDao.isInitialized) {
                    Log.w(TAG, "contextBlockRuleDao not yet initialized, skipping location check")
                    return@launch
                }
                
                val locationRules = contextBlockRuleDao.getActiveLocationRules()
                val matchingRules = locationRules.filter { rule ->
                    rule.latitude != null && rule.longitude != null &&
                    isWithinRadius(location, rule.latitude, rule.longitude, rule.radiusMeters)
                }
                
                _activeLocationRules.value = matchingRules
                
                if (matchingRules.isNotEmpty()) {
                    Log.d(TAG, "Active location rules: ${matchingRules.map { it.ruleName }}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking location rules", e)
            }
        }
    }
    
    private fun checkWifiRules(currentSsid: String) {
        serviceScope.launch {
            try {
                // CRITICAL FIX: Verificar que el DAO está inicializado antes de usarlo
                if (!::contextBlockRuleDao.isInitialized) {
                    Log.w(TAG, "contextBlockRuleDao not yet initialized, skipping WiFi check")
                    return@launch
                }
                
                val wifiRules = contextBlockRuleDao.getActiveWifiRules()
                val matchingRules = wifiRules.filter { rule ->
                    rule.wifiSsid?.equals(currentSsid, ignoreCase = true) == true
                }
                
                _activeWifiRules.value = matchingRules
                
                if (matchingRules.isNotEmpty()) {
                    Log.d(TAG, "Active WiFi rules: ${matchingRules.map { it.ruleName }}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking WiFi rules", e)
            }
        }
    }
    
    private fun isWithinRadius(
        currentLocation: Location,
        targetLat: Double,
        targetLng: Double,
        radiusMeters: Int
    ): Boolean {
        val targetLocation = Location("").apply {
            latitude = targetLat
            longitude = targetLng
        }
        val distance = currentLocation.distanceTo(targetLocation)
        return distance <= radiusMeters
    }
    
    private fun hasLocationPermission(): Boolean = hasLocationPermission(this)
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.svc_context_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.svc_context_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val activeRulesCount = _activeLocationRules.value.size + _activeWifiRules.value.size
        val statusText = if (activeRulesCount > 0) {
            resources.getQuantityString(R.plurals.svc_context_rules_active, activeRulesCount, activeRulesCount)
        } else {
            getString(R.string.svc_context_monitoring)
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.svc_context_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

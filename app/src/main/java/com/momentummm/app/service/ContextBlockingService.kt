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
        
        fun start(context: Context) {
            val intent = Intent(context, ContextBlockingService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, ContextBlockingService::class.java)
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        
        val database = AppDatabase.getDatabase(applicationContext)
        contextBlockRuleDao = database.contextBlockRuleDao()
        
        try {
            locationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location client", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
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
        
        try {
            locationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let {
            locationClient?.removeLocationUpdates(it)
        }
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
    
    @SuppressLint("MissingPermission")
    private fun checkCurrentWifi() {
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid?.removeSurrounding("\"")
            
            if (ssid != null && ssid != "<unknown ssid>") {
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
    
    private fun checkLocationRules(location: Location) {
        serviceScope.launch {
            try {
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
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloqueo por Contexto",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detecta tu ubicación y WiFi para aplicar reglas de bloqueo"
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
            "$activeRulesCount regla(s) activa(s)"
        } else {
            "Monitoreando contexto..."
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Bloqueo por Contexto")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

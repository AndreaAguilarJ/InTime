package com.momentum.app.minimal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "minimal_phone_prefs")

class MinimalPhoneManager(private val context: Context) {
    
    private object PreferencesKeys {
        val IS_MINIMAL_MODE_ENABLED = booleanPreferencesKey("minimal_mode_enabled")
        val ALLOWED_APPS = stringSetPreferencesKey("allowed_apps")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val START_TIME = stringSetPreferencesKey("start_time")
        val END_TIME = stringSetPreferencesKey("end_time")
        val EMERGENCY_CONTACTS = stringSetPreferencesKey("emergency_contacts")
        val ALLOWED_DURING_SCHEDULE = stringSetPreferencesKey("allowed_during_schedule")
    }
    
    private val _isMinimalModeEnabled = MutableStateFlow(false)
    val isMinimalModeEnabled: StateFlow<Boolean> = _isMinimalModeEnabled.asStateFlow()
    
    private val _allowedApps = MutableStateFlow(getDefaultAllowedApps())
    val allowedApps: StateFlow<List<String>> = _allowedApps.asStateFlow()
    
    private val _scheduleEnabled = MutableStateFlow(false)
    val scheduleEnabled: StateFlow<Boolean> = _scheduleEnabled.asStateFlow()
    
    private val _emergencyContacts = MutableStateFlow<List<String>>(emptyList())
    val emergencyContacts: StateFlow<List<String>> = _emergencyContacts.asStateFlow()
    
    init {
        // Load preferences
        loadPreferences()
    }
    
    private fun loadPreferences() {
        // This would be properly implemented with coroutines
        // For now, using default values
    }
    
    suspend fun enableMinimalMode() {
        _isMinimalModeEnabled.value = true
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MINIMAL_MODE_ENABLED] = true
        }
    }
    
    suspend fun disableMinimalMode() {
        _isMinimalModeEnabled.value = false
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MINIMAL_MODE_ENABLED] = false
        }
    }
    
    suspend fun setScheduledMode(enabled: Boolean, startTime: String? = null, endTime: String? = null) {
        _scheduleEnabled.value = enabled
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULE_ENABLED] = enabled
            startTime?.let { preferences[PreferencesKeys.START_TIME] = setOf(it) }
            endTime?.let { preferences[PreferencesKeys.END_TIME] = setOf(it) }
        }
    }
    
    suspend fun addAllowedApp(packageName: String) {
        val currentList = _allowedApps.value.toMutableList()
        if (!currentList.contains(packageName)) {
            currentList.add(packageName)
            _allowedApps.value = currentList
            
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALLOWED_APPS] = currentList.toSet()
            }
        }
    }
    
    suspend fun removeAllowedApp(packageName: String) {
        val currentList = _allowedApps.value.toMutableList()
        currentList.remove(packageName)
        _allowedApps.value = currentList
        
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOWED_APPS] = currentList.toSet()
        }
    }
    
    suspend fun addEmergencyContact(phoneNumber: String) {
        val currentList = _emergencyContacts.value.toMutableList()
        if (!currentList.contains(phoneNumber)) {
            currentList.add(phoneNumber)
            _emergencyContacts.value = currentList
            
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.EMERGENCY_CONTACTS] = currentList.toSet()
            }
        }
    }
    
    suspend fun removeEmergencyContact(phoneNumber: String) {
        val currentList = _emergencyContacts.value.toMutableList()
        currentList.remove(phoneNumber)
        _emergencyContacts.value = currentList
        
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_CONTACTS] = currentList.toSet()
        }
    }
    
    fun isAppAllowed(packageName: String): Boolean {
        if (!_isMinimalModeEnabled.value) return true
        
        // Always allow emergency and core system apps
        if (isCoreSystemApp(packageName)) return true
        
        // Check if we're in scheduled minimal mode
        if (_scheduleEnabled.value && !isInScheduledTime()) return true
        
        return _allowedApps.value.contains(packageName)
    }
    
    private fun isCoreSystemApp(packageName: String): Boolean {
        val coreApps = listOf(
            "com.android.dialer",
            "com.android.mms", 
            "com.android.contacts",
            "com.android.settings",
            "com.android.emergency",
            "com.momentum.app" // Our app
        )
        return coreApps.contains(packageName)
    }
    
    private fun isInScheduledTime(): Boolean {
        // This would check current time against start/end time preferences
        // For now, returning true as placeholder
        return true
    }
    
    fun makePhoneCall(phoneNumber: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun makeEmergencyCall(contactIndex: Int): Boolean {
        val contacts = _emergencyContacts.value
        return if (contactIndex < contacts.size) {
            makePhoneCall(contacts[contactIndex])
        } else {
            false
        }
    }
    
    fun sendSMS(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun openContacts(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CONTACTS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun openCamera(): Boolean {
        return try {
            val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun openCalculator(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.android.calculator2")
            intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent?.let { context.startActivity(it) }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun openClock(): Boolean {
        return try {
            val intent = Intent("android.intent.action.MAIN").apply {
                addCategory("android.intent.category.LAUNCHER")
                setClassName("com.android.deskclock", "com.android.deskclock.DeskClock")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getDefaultAllowedApps(): List<String> {
        return listOf(
            "com.android.dialer", // Phone app
            "com.android.mms", // Messages app
            "com.android.contacts", // Contacts app
            "com.android.settings", // Settings
            "com.android.emergency", // Emergency calls
            "com.momentum.app", // Our app
            "com.android.calculator2", // Calculator
            "com.android.clock", // Clock/Alarm
            "com.android.camera2", // Camera for emergencies
            "com.android.calendar", // Calendar
            "com.google.android.apps.maps" // Maps for emergencies
        )
    }
    
    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return packages.filter { 
            it.packageName != context.packageName && // Exclude our own app
            packageManager.getLaunchIntentForPackage(it.packageName) != null // Only launchable apps
        }.map { appInfo ->
            AppInfo(
                packageName = appInfo.packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                isAllowed = isAppAllowed(appInfo.packageName),
                isCore = isCoreSystemApp(appInfo.packageName),
                category = getAppCategory(appInfo.packageName)
            )
        }.sortedWith(compareBy<AppInfo> { !it.isCore }.thenBy { it.appName })
    }
    
    private fun getAppCategory(packageName: String): AppCategory {
        return when {
            packageName.contains("dialer") || packageName.contains("phone") -> AppCategory.COMMUNICATION
            packageName.contains("mms") || packageName.contains("message") -> AppCategory.COMMUNICATION
            packageName.contains("contacts") -> AppCategory.COMMUNICATION
            packageName.contains("camera") -> AppCategory.UTILITIES
            packageName.contains("calculator") -> AppCategory.UTILITIES
            packageName.contains("clock") || packageName.contains("alarm") -> AppCategory.UTILITIES
            packageName.contains("calendar") -> AppCategory.PRODUCTIVITY
            packageName.contains("maps") || packageName.contains("navigation") -> AppCategory.UTILITIES
            packageName.contains("settings") -> AppCategory.SYSTEM
            packageName.contains("game") -> AppCategory.ENTERTAINMENT
            packageName.contains("music") || packageName.contains("media") -> AppCategory.ENTERTAINMENT
            packageName.contains("browser") || packageName.contains("chrome") -> AppCategory.COMMUNICATION
            else -> AppCategory.OTHER
        }
    }
    
    fun getAppsByCategory(): Map<AppCategory, List<AppInfo>> {
        return getInstalledApps().groupBy { it.category }
    }
    
    fun getMinimalModeStats(): MinimalModeStats {
        val totalApps = getInstalledApps().size
        val allowedApps = _allowedApps.value.size
        val blockedApps = totalApps - allowedApps
        
        return MinimalModeStats(
            totalApps = totalApps,
            allowedApps = allowedApps,
            blockedApps = blockedApps,
            isActive = _isMinimalModeEnabled.value,
            emergencyContactsCount = _emergencyContacts.value.size
        )
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean,
    val isCore: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory {
    COMMUNICATION, UTILITIES, PRODUCTIVITY, ENTERTAINMENT, SYSTEM, OTHER
}

data class MinimalModeStats(
    val totalApps: Int,
    val allowedApps: Int,
    val blockedApps: Int,
    val isActive: Boolean,
    val emergencyContactsCount: Int
)
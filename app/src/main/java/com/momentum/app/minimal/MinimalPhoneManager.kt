package com.momentummm.app.minimal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        val CUSTOM_APP = stringSetPreferencesKey("custom_app")
    }
    
    private val _isMinimalModeEnabled = MutableStateFlow(false)
    val isMinimalModeEnabled: StateFlow<Boolean> = _isMinimalModeEnabled.asStateFlow()
    
    private val _allowedApps = MutableStateFlow(getDefaultAllowedApps())
    val allowedApps: StateFlow<List<String>> = _allowedApps.asStateFlow()
    
    private val _scheduleEnabled = MutableStateFlow(false)
    val scheduleEnabled: StateFlow<Boolean> = _scheduleEnabled.asStateFlow()
    
    private val _emergencyContacts = MutableStateFlow<List<String>>(emptyList())
    val emergencyContacts: StateFlow<List<String>> = _emergencyContacts.asStateFlow()
    
    private val _customApp = MutableStateFlow<String?>(null)
    val customApp: StateFlow<String?> = _customApp.asStateFlow()

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
    
    suspend fun setCustomApp(packageName: String?) {
        _customApp.value = packageName
        context.dataStore.edit { preferences ->
            if (packageName != null) {
                preferences[PreferencesKeys.CUSTOM_APP] = setOf(packageName)
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_APP)
            }
        }
    }

    fun getCustomAppInfo(): AppInfo? {
        val packageName = _customApp.value ?: return null
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                isAllowed = true,
                isCore = false,
                category = getAppCategory(packageName)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun openCustomApp(): Boolean {
        val packageName = _customApp.value ?: return false
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
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
            "com.momentummm.app" // Our app
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
                    data = "tel:$phoneNumber".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
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
                data = "smsto:$phoneNumber".toUri()
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            false
        }
    }
    
    fun openCalculator(): Boolean {
        return try {
            // Try common calculator package names
            val calculatorPackages = listOf(
                "com.android.calculator2",
                "com.google.android.calculator",
                "com.samsung.android.app.calculator",
                "com.miui.calculator",
                "com.huawei.calculator"
            )

            for (packageName in calculatorPackages) {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return true
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            // Fallback: try generic calculator intent
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory("android.intent.category.APP_CALCULATOR")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    fun openClock(): Boolean {
        return try {
            // Try common clock app package names
            val clockPackages = listOf(
                "com.android.deskclock",
                "com.google.android.deskclock",
                "com.samsung.android.app.clockpackage",
                "com.android.alarmclock"
            )

            for (packageName in clockPackages) {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        return true
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            // Fallback: try alarm intent
            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    fun openSettings(): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: try different settings intents
            try {
                val fallbackIntent = Intent().apply {
                    action = "android.settings.SETTINGS"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openMessages(): Boolean {
        return try {
            // Try to open default SMS app
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: try common messaging apps
            val messagingApps = listOf(
                "com.android.mms",
                "com.google.android.apps.messaging",
                "com.samsung.android.messaging"
            )

            for (packageName in messagingApps) {
                try {
                    val fallbackIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (fallbackIntent != null) {
                        fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(fallbackIntent)
                        return true
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            // Last fallback: compose SMS
            try {
                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = "sms:".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openDialer(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: try to open phone app directly
            try {
                val phoneApps = listOf(
                    "com.android.dialer",
                    "com.google.android.dialer",
                    "com.samsung.android.dialer"
                )

                for (packageName in phoneApps) {
                    val fallbackIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (fallbackIntent != null) {
                        fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(fallbackIntent)
                        return true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun getDefaultAllowedApps(): List<String> {
        return listOf(
            "com.android.dialer", // Phone app
            "com.android.mms", // Messages app
            "com.android.contacts", // Contacts app
            "com.android.settings", // Settings
            "com.android.emergency", // Emergency calls
            "com.momentummm.app", // Our app
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
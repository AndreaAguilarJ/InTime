package com.momentum.app.minimal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MinimalPhoneManager(private val context: Context) {
    
    private val _isMinimalModeEnabled = MutableStateFlow(false)
    val isMinimalModeEnabled: StateFlow<Boolean> = _isMinimalModeEnabled.asStateFlow()
    
    private val _allowedApps = MutableStateFlow(getDefaultAllowedApps())
    val allowedApps: StateFlow<List<String>> = _allowedApps.asStateFlow()
    
    fun enableMinimalMode() {
        _isMinimalModeEnabled.value = true
    }
    
    fun disableMinimalMode() {
        _isMinimalModeEnabled.value = false
    }
    
    fun addAllowedApp(packageName: String) {
        val currentList = _allowedApps.value.toMutableList()
        if (!currentList.contains(packageName)) {
            currentList.add(packageName)
            _allowedApps.value = currentList
        }
    }
    
    fun removeAllowedApp(packageName: String) {
        val currentList = _allowedApps.value.toMutableList()
        currentList.remove(packageName)
        _allowedApps.value = currentList
    }
    
    fun isAppAllowed(packageName: String): Boolean {
        return !_isMinimalModeEnabled.value || _allowedApps.value.contains(packageName)
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
            "com.android.camera2" // Camera for emergencies
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
                isAllowed = isAppAllowed(appInfo.packageName)
            )
        }.sortedBy { it.appName }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean
)
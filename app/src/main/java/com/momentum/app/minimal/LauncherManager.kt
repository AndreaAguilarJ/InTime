package com.momentummm.app.minimal

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

class LauncherManager(private val context: Context) {

    private object PreferencesKeys {
        val IS_DEFAULT_LAUNCHER = booleanPreferencesKey("is_default_launcher")
        val AUTO_ENABLE_MINIMAL = booleanPreferencesKey("auto_enable_minimal")
    }

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _autoEnableMinimal = MutableStateFlow(false)
    val autoEnableMinimal: StateFlow<Boolean> = _autoEnableMinimal.asStateFlow()

    init {
        checkIfDefaultLauncher()
        loadPreferences()
    }

    private fun loadPreferences() {
        // Load preferences from DataStore
        // This should be implemented with proper coroutines in a real app
    }

    suspend fun setAsDefaultLauncher(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use RoleManager
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        // This will open a system dialog asking user to set as default launcher
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        // Note: This needs to be called from an Activity with startActivityForResult
                        context.startActivity(intent)
                    }
                    true
                } else {
                    requestLauncherSelectionLegacy()
                }
            } else {
                requestLauncherSelectionLegacy()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun requestLauncherSelectionLegacy(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun checkIfDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            val isDefault = resolveInfo?.activityInfo?.packageName == context.packageName
            _isDefaultLauncher.value = isDefault
            isDefault
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setAutoEnableMinimal(enabled: Boolean) {
        _autoEnableMinimal.value = enabled
        context.launcherDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_ENABLE_MINIMAL] = enabled
        }
    }

    suspend fun shouldAutoEnableMinimal(): Boolean {
        return context.launcherDataStore.data.map { preferences ->
            preferences[PreferencesKeys.AUTO_ENABLE_MINIMAL] ?: false
        }.first()
    }

    fun getLauncherSetupIntent(): Intent {
        return Intent().apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createHomeScreenShortcut() {
        try {
            val shortcutIntent = Intent(context, context::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                putExtra("launch_minimal_mode", true)
            }

            // Create shortcut (this would need proper implementation for different Android versions)
            val addIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, "Modo Mínimo")
                putExtra("duplicate", false)
                action = "com.android.launcher.action.INSTALL_SHORTCUT"
            }

            context.sendBroadcast(addIntent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun getInstalledLaunchers(): List<LauncherInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)

        return resolveInfos.map { resolveInfo ->
            LauncherInfo(
                packageName = resolveInfo.activityInfo.packageName,
                name = resolveInfo.loadLabel(context.packageManager).toString(),
                isDefault = resolveInfo.activityInfo.packageName == getDefaultLauncherPackage(),
                isCurrentApp = resolveInfo.activityInfo.packageName == context.packageName
            )
        }.sortedWith(compareBy<LauncherInfo> { !it.isCurrentApp }.thenBy { !it.isDefault }.thenBy { it.name })
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        return resolveInfo?.activityInfo?.packageName
    }

    fun openLauncherSettings() {
        try {
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_HOME_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val fallbackIntent = Intent().apply {
                    action = android.provider.Settings.ACTION_SETTINGS
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                // Handle error
            }
        }
    }
}

data class LauncherInfo(
    val packageName: String,
    val name: String,
    val isDefault: Boolean,
    val isCurrentApp: Boolean
)

package com.momentummm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.momentummm.app.data.manager.ThemeManager
import com.momentummm.app.data.manager.AutoSyncManager
import com.momentummm.app.ui.screen.MomentumApp
import com.momentummm.app.ui.theme.MomentumTheme
import com.momentummm.app.minimal.LauncherManager
import com.momentummm.app.minimal.MinimalPhoneManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var launcherManager: LauncherManager
    private lateinit var minimalPhoneManager: MinimalPhoneManager

    @Inject
    lateinit var autoSyncManager: AutoSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        launcherManager = LauncherManager(this)
        minimalPhoneManager = MinimalPhoneManager(this)

        val themeManager = ThemeManager(this)
        
        // Check if we should auto-enable minimal mode when coming from launcher
        val launchMinimalMode = intent?.getBooleanExtra("launch_minimal_mode", false) ?: false
        if (launchMinimalMode) {
            lifecycleScope.launch {
                if (launcherManager.shouldAutoEnableMinimal()) {
                    minimalPhoneManager.enableMinimalMode()
                }
            }
        }

        setContent {
            MomentumTheme(themeManager = themeManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MomentumApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if we're still the default launcher
        launcherManager.checkIfDefaultLauncher()
    }

    override fun onPause() {
        super.onPause()
        // Guardar datos cuando la app pasa a background
        lifecycleScope.launch {
            autoSyncManager.forceSyncNow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSyncManager.cleanup()
    }
}
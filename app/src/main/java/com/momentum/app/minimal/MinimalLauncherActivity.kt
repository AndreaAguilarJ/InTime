package com.momentummm.app.minimal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.manager.ThemeManager
import com.momentummm.app.ui.theme.MomentumTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MinimalLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = applicationContext as MomentumApplication
        val minimalPhoneManager = application.minimalPhoneManager
        val themeManager = ThemeManager(this)

        setContent {
            // Prevent going back when in launcher mode
            BackHandler {
                // User can exit minimal mode through settings
                // Do nothing on back press
            }

            MomentumTheme(themeManager = themeManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinimalPhoneScreen(
                        minimalPhoneManager = minimalPhoneManager,
                        onSettingsClick = {
                            // Open settings in main app
                            finish()
                        }
                    )
                }
            }
        }
    }
}

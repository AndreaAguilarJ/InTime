package com.momentummm.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.momentummm.app.MainActivity
import com.momentummm.app.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity puente lanzada desde el widget.
 * Marca un flag en DataStore para suprimir el tutorial una sola vez
 * y redirige inmediatamente a MainActivity.
 */
class WidgetLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            // Suprimir tutorial solo en este lanzamiento
            UserPreferencesRepository.setSuppressTutorialOnce(this@WidgetLaunchActivity, true)
            // Redirigir a MainActivity
            val intent = Intent(this@WidgetLaunchActivity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}

package com.momentummm.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.momentummm.app.data.manager.ThemeManager
import com.momentummm.app.data.manager.AutoSyncManager
import com.momentummm.app.data.repository.AppLimitRepository
import com.momentummm.app.service.AppMonitoringService
import com.momentummm.app.service.NuclearModeService
import com.momentummm.app.ui.screen.MomentumApp
import com.momentummm.app.ui.theme.MomentumTheme
import com.momentummm.app.minimal.LauncherManager
import com.momentummm.app.minimal.MinimalPhoneManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var launcherManager: LauncherManager
    private lateinit var minimalPhoneManager: MinimalPhoneManager
    
    // Flag para verificar si la activity está activa
    private var isActivityActive = false

    @Inject
    lateinit var autoSyncManager: AutoSyncManager

    @Inject
    lateinit var appLimitRepository: AppLimitRepository

    // Para saber si alguna función de Bloqueo inteligente exige el monitor,
    // aunque el usuario no haya configurado ningún límite de app.
    @Inject
    lateinit var smartBlockingManager: com.momentummm.app.data.manager.SmartBlockingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        isActivityActive = true
        
        // Initialize managers
        launcherManager = LauncherManager(this)
        // Usa el MinimalPhoneManager compartido de la Application. Antes se creaba
        // aquí una segunda instancia con `MinimalPhoneManager(this)`: escribía en el
        // mismo DataStore, pero su StateFlow en memoria era distinto del que observa
        // la UI (que lee el de la Application), así que activar el modo mínimo por el
        // intent del lanzador no se reflejaba en la interfaz hasta reiniciar.
        minimalPhoneManager = (application as MomentumApplication).minimalPhoneManager

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

        ensureMonitoringServiceRunning()
        notifyNuclearForegroundState(inForeground = true)
    }

    /**
     * Informa al Modo nuclear de si la app está visible.
     *
     * BUG CORREGIDO: `NuclearModeService` sólo hacía avanzar la espera de
     * desbloqueo mientras `isAppInForeground` fuera `true`, pero NADIE llamaba
     * nunca a `notifyAppForeground()`. La bandera arrancaba en `false` y se
     * quedaba así, de modo que la espera no progresaba ni un segundo y el
     * desbloqueo del modo nuclear era inalcanzable por diseño accidental.
     *
     * Se envía siempre, sin comprobar antes si el modo está activo: el propio
     * servicio se detiene si no lo está, y consultar la base desde onResume
     * añadiría una lectura de disco en el arranque de cada pantalla.
     */
    private fun notifyNuclearForegroundState(inForeground: Boolean) {
        try {
            if (inForeground) {
                NuclearModeService.notifyAppForeground(this)
            } else {
                NuclearModeService.notifyAppBackground(this)
            }
        } catch (e: Exception) {
            // startService puede fallar si el servicio no está corriendo porque
            // el modo nuclear no está activo. No es un error para el usuario.
            Log.d("MainActivity", "Modo nuclear no activo: ${e.message}")
        }
    }

    /**
     * Arranca el servicio de monitoreo si hay límites configurados.
     *
     * BUG CORREGIDO: el servicio sólo se iniciaba al crear o editar un límite
     * (AppLimitRepository) o tras reiniciar el dispositivo (BootReceiver). Si el
     * proceso moría —cierre forzado, limpieza de memoria, optimización del
     * fabricante— el monitor no volvía a arrancar hasta que el usuario entrara
     * de nuevo en la pantalla de límites, y durante todo ese tiempo no se
     * bloqueaba ninguna app.
     *
     * Se hace desde onResume (no desde Application.onCreate) porque en
     * Android 12+ iniciar un foreground service desde segundo plano lanza
     * ForegroundServiceStartNotAllowedException; aquí la app está en primer
     * plano por definición.
     */
    private fun ensureMonitoringServiceRunning() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val hasLimits = appLimitRepository.getAllLimits().firstOrNull()
                    ?.any { it.isEnabled } == true

                // Las siete funciones de Bloqueo inteligente se aplican DENTRO
                // de este monitor. Antes sólo se miraban los límites de app, así
                // que activar el Modo nuclear o Solo comunicación sin ningún
                // límite configurado dejaba los interruptores encendidos y sin
                // ningún efecto.
                val smartBlockingNeedsIt = runCatching {
                    smartBlockingManager.requiresMonitoringNow()
                }.getOrDefault(false)

                if (!hasLimits && !smartBlockingNeedsIt) return@launch

                withContext(Dispatchers.Main) {
                    AppMonitoringService.startService(this@MainActivity)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "No se pudo asegurar el servicio de monitoreo", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        notifyNuclearForegroundState(inForeground = false)
        // Guardar datos cuando la app pasa a background (no bloqueante)
        if (isActivityActive && ::autoSyncManager.isInitialized) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(3000L) {
                        autoSyncManager.forceSyncNow()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error en sync onPause", e)
                }
            }
        }
    }

    override fun onDestroy() {
        isActivityActive = false
        // CRITICAL FIX: Limpiar recursos cuando la activity termina
        // Antes la lógica estaba invertida: solo limpiaba en config changes (!isFinishing)
        // pero NUNCA cuando el activity realmente finalizaba (isFinishing = true)
        if (::autoSyncManager.isInitialized) {
            try {
                autoSyncManager.cleanup()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en cleanup onDestroy", e)
            }
        }
        super.onDestroy()
    }
}
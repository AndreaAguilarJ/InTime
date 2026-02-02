package com.momentummm.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.manager.SmartNotificationManager
import com.momentummm.app.data.repository.WebsiteBlockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebsiteBlockService : AccessibilityService() {

    @Inject
    lateinit var websiteBlockRepository: WebsiteBlockRepository

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("WebsiteBlockService", "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler) // CAMBIO: Usar Dispatchers.IO para operaciones de BD
    
    // CRÍTICO: Throttling para evitar ANR por spam de eventos
    private var lastEventTime = 0L
    private val EVENT_THROTTLE_MS = 300L // Mínimo 300ms entre procesamiento
    
    // Acceso al SmartNotificationManager
    private val smartNotificationManager: SmartNotificationManager?
        get() = try {
            (application as? MomentumApplication)?.smartNotificationManager
        } catch (e: Exception) {
            null
        }

    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.android.browser",
        "com.UCMobile.intl",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "org.chromium.chrome",
        "com.kiwibrowser.browser",
        "com.duckduckgo.mobile.android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service connected
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Solo procesar eventos de navegadores
        if (!browserPackages.contains(packageName)) return
        
        // CRÍTICO: Throttle para evitar ANR por spam de eventos (TYPE_WINDOW_CONTENT_CHANGED se dispara cientos de veces/segundo durante scroll)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTime < EVENT_THROTTLE_MS) {
            return
        }
        lastEventTime = currentTime

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                checkUrlAndBlock()
            }
            else -> {} // Ignorar otros eventos
        }
    }

    private fun checkUrlAndBlock() {
        val rootNode = rootInActiveWindow ?: return
        
        // Verificar si el repositorio está inicializado antes de procesar
        if (!::websiteBlockRepository.isInitialized) {
            rootNode.recycle() // CRÍTICO: Reciclar antes de return temprano
            return
        }
        
        val url = try {
            extractUrl(rootNode)
        } catch (e: Exception) {
            null
        } finally {
            // CRÍTICO: Siempre reciclar el rootNode para evitar memory leak
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                // Ignorar si ya fue reciclado
            }
        } ?: return

        serviceScope.launch {
            try {
                // Timeout de 2 segundos para evitar ANR
                kotlinx.coroutines.withTimeoutOrNull(2000L) {
                    val blockedInfo = websiteBlockRepository.getBlockedUrlInfo(url)
                    if (blockedInfo != null) {
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            blockWebsite()
                        }
                        // Usar SmartNotificationManager para notificaciones dinámicas
                        if (blockedInfo.category != null) {
                            smartNotificationManager?.showWebsiteCategoryBlockedNotification(blockedInfo.category)
                        } else {
                            smartNotificationManager?.showWebsiteBlockedNotification(url)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignorar errores silenciosamente
            }
        }
    }

    private fun extractUrl(node: AccessibilityNodeInfo?, depth: Int = 0): String? {
        // Límite de profundidad para evitar StackOverflow en árboles muy profundos
        if (node == null || depth > 15) return null

        try {
            // Intentar obtener la URL de la barra de direcciones
            if (node.viewIdResourceName?.contains("url_bar") == true ||
                node.viewIdResourceName?.contains("address") == true ||
                node.viewIdResourceName?.contains("search") == true) {
                node.text?.toString()?.let { return it }
            }

            // Buscar recursivamente en los nodos hijos con límite de profundidad
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val url = extractUrl(child, depth + 1)
                    child.recycle() // CRÍTICO: Reciclar el nodo hijo para evitar memory leak
                    if (url != null) return url
                }
            }
        } catch (e: Exception) {
            // Ignorar excepciones de nodos inválidos
        }

        return null
    }

    private fun blockWebsite() {
        // Cerrar la actividad del navegador actual
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

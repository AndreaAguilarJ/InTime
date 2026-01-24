package com.momentummm.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import com.momentummm.app.data.repository.InAppBlockRepository
import com.momentummm.app.ui.InAppBlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MomentumAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var inAppBlockRepository: InAppBlockRepository

    // Usar Dispatchers.Default para operaciones de background, no Main
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastBlockTime: Long = 0
    private var lastProcessedTime: Long = 0
    // Reducimos el cooldown a 1.5s para ser más agresivos pero permitir salir
    private val BLOCK_COOLDOWN = 1500L
    private val PROCESS_THROTTLE = 300L // Throttle de eventos para evitar sobrecarga 

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "MomentumAccessibilityService conectado y listo para bloquear")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Procesamos más tipos de eventos para no perder nada
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED, // Detectar scroll dentro de feeds
            AccessibilityEvent.TYPE_VIEW_CLICKED -> { // Detectar clicks en tabs
                processAccessibilityEvent(event)
            }
        }
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Ignorar nuestra propia app
        if (packageName == this.packageName) return

        // Throttling: no procesar eventos muy rápido
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < PROCESS_THROTTLE) return
        lastProcessedTime = currentTime

        serviceScope.launch {
            try {
                // Timeout de 1 segundo para prevenir ANR
                withTimeoutOrNull(1000L) {
                    // Optimización: Verificar primero si hay reglas para este paquete antes de escanear el árbol
                    val rules = withContext(Dispatchers.IO) {
                        inAppBlockRepository.getEnabledRulesForPackage(packageName)
                    }
                    if (rules.isEmpty()) return@withTimeoutOrNull

                    // Obtenemos el nodo raíz de la ventana activa
                    // Usamos rootInActiveWindow que es más fiable que event.source
                    val rootNode = rootInActiveWindow ?: event.source ?: return@withTimeoutOrNull

                    for (rule in rules) {
                        if (shouldBlockContent(rootNode, rule.ruleId)) {
                            handleBlock(rule.appName, rule.featureName)
                            break // Si bloqueamos una, ya no es necesario seguir buscando
                        }
                    }
                } ?: Log.w(TAG, "processAccessibilityEvent timeout - operación cancelada")
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando evento de accesibilidad", e)
            }
        }
    }

    private fun handleBlock(appName: String, featureName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime > BLOCK_COOLDOWN) {
            lastBlockTime = currentTime
            
            Log.d(TAG, " BLOQUEO DETECTADO: $featureName en $appName")
            
            // 1. Acción Inmediata: Volver atrás para salir de la función
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            // 2. Acción Visual: Mostrar pantalla de bloqueo
            showBlockScreen(appName, featureName)
        }
    }

    private fun shouldBlockContent(rootNode: AccessibilityNodeInfo, ruleId: String): Boolean {
        return when (ruleId) {
            // Instagram
            "instagram_reels" -> detectInstagramReels(rootNode)
            "instagram_explore" -> detectInstagramExplore(rootNode)

            // YouTube
            "youtube_shorts" -> detectYouTubeShorts(rootNode)
            "youtube_search" -> detectYouTubeSearch(rootNode)

            // Facebook
            "facebook_reels" -> detectFacebookReels(rootNode)

            // Snapchat
            "snapchat_discover" -> detectSnapchatDiscover(rootNode)

            // TikTok
            "tiktok_foryou" -> detectTikTokForYou(rootNode)

            // X (Twitter)
            "x_explore" -> detectXExplore(rootNode)

            else -> false
        }
    }

    // --- LÓGICA DE DETECCIÓN MEJORADA ---

    // Instagram Reels
    private fun detectInstagramReels(rootNode: AccessibilityNodeInfo): Boolean {
        // Estrategia: Buscar combinaciones de texto, ID y descripción
        // 1. IDs específicos de contenedores de video
        // "clips" suele ser el identificador interno de Reels en Instagram
        if (hasViewId(rootNode, "clips_video_container")) return true

        // 2. Tab de Reels seleccionado (evitar falsos positivos en Home)
        if (hasSelectedViewId(rootNode, "clips_tab")) return true // Botón Reels seleccionado
        if (hasSelectedContentDescription(rootNode, "Reels tab")) return true
        if (hasSelectedContentDescription(rootNode, "Pestaña Reels")) return true
        if (hasSelectedText(rootNode, "Reels")) return true
        if (hasSelectedText(rootNode, "Reel")) return true
        
        return false
    }

    // Instagram Explorar
    private fun detectInstagramExplore(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Explorar") || hasText(rootNode, "Explore")) return true
        if (hasViewId(rootNode, "search_tab")) return true // Botón lupa
        if (hasContentDescription(rootNode, "Search and explore")) return true
        if (hasContentDescription(rootNode, "Buscar y explorar")) return true
        return false
    }

    // YouTube Shorts
    private fun detectYouTubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        // YouTube es complejo porque mezcla contenidos.
        // Buscar botón "Shorts" seleccionado o contenedor de Shorts
        if (hasText(rootNode, "Shorts")) return true
        if (hasViewId(rootNode, "shorts_container")) return true
        if (hasViewId(rootNode, "shorts_player")) return true
        if (hasContentDescription(rootNode, "Shorts tab")) return true
        return false
    }

    // YouTube Búsqueda
    private fun detectYouTubeSearch(rootNode: AccessibilityNodeInfo): Boolean {
        // Detectar si estamos en la pantalla de resultados o escribiendo
        val isSearching = hasViewId(rootNode, "search_edit_text") || 
                          hasText(rootNode, "Search YouTube") ||
                          hasText(rootNode, "Buscar en YouTube")
        
        // Confirmar con resultados
        val hasResults = hasText(rootNode, "Resultados de búsqueda") ||
                         hasText(rootNode, "Search results")
                         
        return isSearching || hasResults
    }

    // Facebook Reels
    private fun detectFacebookReels(rootNode: AccessibilityNodeInfo): Boolean {
        // Solo bloquear si la pestaña de Reels está explícitamente seleccionada
        if (hasSelectedText(rootNode, "Reels")) return true
        if (hasSelectedContentDescription(rootNode, "Reels")) return true
        return false
    }

    // Snapchat Discover
    private fun detectSnapchatDiscover(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Discover") || hasText(rootNode, "Descubrir")) return true
        if (hasContentDescription(rootNode, "Discover Page")) return true
        return false
    }

    // TikTok For You
    private fun detectTikTokForYou(rootNode: AccessibilityNodeInfo): Boolean {
        // En TikTok casi todo es For You, pero buscamos la pestaña activa
        if (hasText(rootNode, "Para ti") || hasText(rootNode, "For You")) {
             // Verificar si está seleccionado podría ser difícil, asumimos presencia
             // A menudo el texto "Para ti" está en la parte superior
             return true 
        }
        return false
    }

    // X (Twitter) Explorar
    private fun detectXExplore(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Explore") || hasText(rootNode, "Explorar")) return true
        if (hasText(rootNode, "Trending") || hasText(rootNode, "Tendencias")) return true
        return false
    }

    // --- FUNCIONES DE BÚSQUEDA RECURSIVA OPTIMIZADAS ---

    private fun hasText(rootNode: AccessibilityNodeInfo?, text: String): Boolean {
        if (rootNode == null) return false
        
        // 1. Chequeo directo
        if (rootNode.text?.contains(text, ignoreCase = true) == true) return true
        
        // 2. Recursión
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasText(child, text)) {
                    child.recycle() // Importante reciclar nodos
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun hasViewId(rootNode: AccessibilityNodeInfo?, viewIdPart: String): Boolean {
        if (rootNode == null) return false

        // 1. Chequeo directo (contains porque el ID completo incluye el paquete)
        if (rootNode.viewIdResourceName?.contains(viewIdPart, ignoreCase = true) == true) return true

        // 2. Recursión
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasViewId(child, viewIdPart)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun hasContentDescription(rootNode: AccessibilityNodeInfo?, description: String): Boolean {
        if (rootNode == null) return false

        // 1. Chequeo directo
        if (rootNode.contentDescription?.contains(description, ignoreCase = true) == true) return true

        // 2. Recursión
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasContentDescription(child, description)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedText(rootNode: AccessibilityNodeInfo?, text: String): Boolean {
        if (rootNode == null) return false

        if (rootNode.text?.contains(text, ignoreCase = true) == true &&
            (rootNode.isSelected || rootNode.isChecked)
        ) {
            return true
        }

        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedText(child, text)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedContentDescription(rootNode: AccessibilityNodeInfo?, description: String): Boolean {
        if (rootNode == null) return false

        if (rootNode.contentDescription?.contains(description, ignoreCase = true) == true &&
            (rootNode.isSelected || rootNode.isChecked)
        ) {
            return true
        }

        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedContentDescription(child, description)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedViewId(rootNode: AccessibilityNodeInfo?, viewIdPart: String): Boolean {
        if (rootNode == null) return false

        if (rootNode.viewIdResourceName?.contains(viewIdPart, ignoreCase = true) == true &&
            (rootNode.isSelected || rootNode.isChecked)
        ) {
            return true
        }

        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedViewId(child, viewIdPart)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }

    private fun showBlockScreen(appName: String, featureName: String) {
        try {
            val intent = Intent(this, InAppBlockedActivity::class.java).apply {
                putExtra("app_name", appName)
                putExtra("feature_name", featureName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando pantalla de bloqueo", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servicio interrumpido")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Servicio destruido")
    }

    companion object {
        private const val TAG = "MomentumA11yService"
    }
}

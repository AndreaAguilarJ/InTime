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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentPackage: String = ""
    private var lastBlockTime: Long = 0
    private val BLOCK_COOLDOWN = 2000L // 2 segundos entre bloqueos

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "MomentumAccessibilityService conectado con detección de funciones")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Solo procesar eventos de cambio de ventana o contenido
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                processAccessibilityEvent(event)
            }
        }
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Ignorar nuestra propia app
        if (packageName == this.packageName) return

        currentPackage = packageName

        serviceScope.launch {
            try {
                // Obtener reglas habilitadas para este paquete
                val rules = inAppBlockRepository.getEnabledRulesForPackage(packageName)
                if (rules.isEmpty()) return@launch

                // Verificar si alguna regla coincide con el contenido actual
                val rootNode = rootInActiveWindow ?: return@launch

                for (rule in rules) {
                    if (shouldBlockContent(rootNode, rule.ruleId, packageName)) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBlockTime > BLOCK_COOLDOWN) {
                            lastBlockTime = currentTime
                            showBlockScreen(rule.appName, rule.featureName)
                            Log.d(TAG, "Bloqueando ${rule.featureName} en ${rule.appName}")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando evento de accesibilidad", e)
            }
        }
    }

    private fun shouldBlockContent(rootNode: AccessibilityNodeInfo, ruleId: String, packageName: String): Boolean {
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

    // Detección de Instagram Reels
    private fun detectInstagramReels(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Reels").isNotEmpty() ||
               findNodesByViewId(rootNode, "clips").isNotEmpty() ||
               findNodesByClassName(rootNode, "android.widget.VideoView").any {
                   it.parent?.viewIdResourceName?.contains("clips") == true
               }
    }

    // Detección de Instagram Explorar
    private fun detectInstagramExplore(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Explorar").isNotEmpty() ||
               findNodesByText(rootNode, "Explore").isNotEmpty() ||
               findNodesByViewId(rootNode, "explore").isNotEmpty()
    }

    // Detección de YouTube Shorts
    private fun detectYouTubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Shorts").isNotEmpty() ||
               findNodesByViewId(rootNode, "shorts").isNotEmpty() ||
               findNodesByContentDescription(rootNode, "Shorts").isNotEmpty()
    }

    // Detección de búsqueda de YouTube
    private fun detectYouTubeSearch(rootNode: AccessibilityNodeInfo): Boolean {
        val hasSearchField = findNodesByViewId(rootNode, "search").isNotEmpty() ||
                            findNodesByClassName(rootNode, "android.widget.EditText").any {
                                it.text?.contains("Buscar") == true ||
                                it.text?.contains("Search") == true ||
                                it.hint?.contains("Buscar") == true ||
                                it.hint?.contains("Search") == true
                            }

        // Verificar si estamos en la actividad de búsqueda
        val hasSearchActivity = findNodesByText(rootNode, "Resultados de búsqueda").isNotEmpty() ||
                               findNodesByText(rootNode, "Search results").isNotEmpty()

        return hasSearchField && hasSearchActivity
    }

    // Detección de Facebook Reels
    private fun detectFacebookReels(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Reels").isNotEmpty() ||
               findNodesByViewId(rootNode, "reels").isNotEmpty()
    }

    // Detección de Snapchat Discover
    private fun detectSnapchatDiscover(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Discover").isNotEmpty() ||
               findNodesByText(rootNode, "Descubrir").isNotEmpty() ||
               findNodesByViewId(rootNode, "discover").isNotEmpty()
    }

    // Detección de TikTok For You
    private fun detectTikTokForYou(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "For You").isNotEmpty() ||
               findNodesByText(rootNode, "Para ti").isNotEmpty() ||
               findNodesByViewId(rootNode, "foryou").isNotEmpty()
    }

    // Detección de X (Twitter) Explorar
    private fun detectXExplore(rootNode: AccessibilityNodeInfo): Boolean {
        return findNodesByText(rootNode, "Explore").isNotEmpty() ||
               findNodesByText(rootNode, "Explorar").isNotEmpty() ||
               findNodesByContentDescription(rootNode, "Explore").isNotEmpty()
    }

    // Funciones auxiliares para búsqueda en el árbol de accesibilidad
    private fun findNodesByText(rootNode: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes != null) {
            result.addAll(nodes)
        }
        return result
    }

    private fun findNodesByViewId(rootNode: AccessibilityNodeInfo, viewId: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        searchNodesByViewId(rootNode, viewId, result)
        return result
    }

    private fun searchNodesByViewId(node: AccessibilityNodeInfo?, viewId: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        if (node.viewIdResourceName?.contains(viewId, ignoreCase = true) == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            searchNodesByViewId(node.getChild(i), viewId, result)
        }
    }

    private fun findNodesByClassName(rootNode: AccessibilityNodeInfo, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        searchNodesByClassName(rootNode, className, result)
        return result
    }

    private fun searchNodesByClassName(node: AccessibilityNodeInfo?, className: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        if (node.className?.toString() == className) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            searchNodesByClassName(node.getChild(i), className, result)
        }
    }

    private fun findNodesByContentDescription(rootNode: AccessibilityNodeInfo, description: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        searchNodesByContentDescription(rootNode, description, result)
        return result
    }

    private fun searchNodesByContentDescription(node: AccessibilityNodeInfo?, description: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        if (node.contentDescription?.contains(description, ignoreCase = true) == true) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            searchNodesByContentDescription(node.getChild(i), description, result)
        }
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

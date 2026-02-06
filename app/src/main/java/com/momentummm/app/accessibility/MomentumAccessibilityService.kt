package com.momentummm.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import com.momentummm.app.data.engine.AdvancedDetectionEngine
import com.momentummm.app.data.engine.AdvancedDetectionEngine.ContentType
import com.momentummm.app.data.engine.AdvancedDetectionEngine.DetectionResult
import com.momentummm.app.data.engine.AdaptiveBlockingManager
import com.momentummm.app.data.engine.BlockingEventType
import com.momentummm.app.data.engine.GradualAction
import com.momentummm.app.data.repository.InAppBlockRepository
import com.momentummm.app.ui.InAppBlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SERVICIO DE ACCESIBILIDAD MEJORADO CON DETECCIÓN MULTI-SEÑAL
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Mejoras clave sobre la versión anterior:
 * 1. Detección multi-señal con fingerprinting (AdvancedDetectionEngine)
 *    - Un solo pase del árbol en vez de múltiples recorridos recursivos
 *    - Detección basada en confianza (0.0-1.0) en vez de booleanos
 *    - Soporte para 13+ apps y 40+ tipos de contenido
 * 
 * 2. Bloqueo gradual adaptativo (AdaptiveBlockingManager)
 *    - 9 niveles: desde recordatorio suave hasta bloqueo nuclear
 *    - Intervenciones inteligentes basadas en patrones
 * 
 * 3. Detección de scroll infinito y video autoplay
 *    - Identifica comportamiento de scroll compulsivo
 *    - Detecta video reproduciéndose en pantalla completa
 * 
 * 4. Optimización de rendimiento
 *    - TreeSnapshot: escaneo único del árbol para todas las detecciones
 *    - Cache de resultados con TTL de 200ms
 *    - Throttling adaptativo según actividad
 */
@AndroidEntryPoint
class MomentumAccessibilityService : AccessibilityService() {

    @Inject lateinit var inAppBlockRepository: InAppBlockRepository
    @Inject lateinit var detectionEngine: AdvancedDetectionEngine
    @Inject lateinit var adaptiveBlockingManager: AdaptiveBlockingManager

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception in MomentumAccessibilityService", throwable)
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private var lastBlockTime: Long = 0
    private var lastProcessedTime: Long = 0
    private val BLOCK_COOLDOWN = 1500L
    private val PROCESS_THROTTLE = 250L
    
    // === NUEVO: Tracking de scroll infinito ===
    private var scrollEventCount = 0
    private var lastScrollResetTime = 0L
    private val SCROLL_WINDOW_MS = 10_000L   // Ventana de 10 segundos
    private val SCROLL_THRESHOLD = 8          // 8 scrolls en 10s = compulsivo
    
    // === NUEVO: Tracking de sesión por app ===
    private var currentSessionApp = ""
    private var sessionStartTime = 0L
    private val SESSION_WARNING_MINUTES = 15  // Advertir cada 15 min

    // === NUEVO: Último resultado de detección para evitar re-escaneo ===
    private var lastDetectionResult: DetectionResult? = null
    private var lastDetectionPackage = ""
    private var lastDetectionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "🛡️ MomentumAccessibilityService V2 conectado - Motor de detección multi-señal activo")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // Cambio de ventana: alta prioridad, siempre procesar
                    processAccessibilityEvent(event, highPriority = true)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    // Contenido cambiado o scroll: tracking + detección throttled
                    trackScrollBehavior(event)
                    processAccessibilityEvent(event, highPriority = false)
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    processAccessibilityEvent(event, highPriority = false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onAccessibilityEvent", e)
        }
    }

    /**
     * Detecta comportamiento de scroll compulsivo
     */
    private fun trackScrollBehavior(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        
        val now = System.currentTimeMillis()
        if (now - lastScrollResetTime > SCROLL_WINDOW_MS) {
            scrollEventCount = 0
            lastScrollResetTime = now
        }
        scrollEventCount++
        
        if (scrollEventCount >= SCROLL_THRESHOLD) {
            val pkg = event.packageName?.toString() ?: return
            Log.d(TAG, "⚠️ Scroll compulsivo detectado en $pkg ($scrollEventCount scrolls en ${SCROLL_WINDOW_MS/1000}s)")
            
            serviceScope.launch {
                adaptiveBlockingManager.trackEvent(
                    packageName = pkg,
                    eventType = BlockingEventType.GRADUAL_WARNING_SHOWN,
                    details = "Scroll compulsivo: $scrollEventCount en ${SCROLL_WINDOW_MS/1000}s"
                )
            }
            scrollEventCount = 0
        }
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent, highPriority: Boolean) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        val currentTime = System.currentTimeMillis()
        val throttle = if (highPriority) PROCESS_THROTTLE / 2 else PROCESS_THROTTLE
        if (currentTime - lastProcessedTime < throttle) return
        lastProcessedTime = currentTime
        
        // Tracking de sesión
        if (packageName != currentSessionApp) {
            currentSessionApp = packageName
            sessionStartTime = currentTime
        }

        if (!::inAppBlockRepository.isInitialized || !::detectionEngine.isInitialized) {
            return
        }

        serviceScope.launch {
            try {
                withTimeoutOrNull(1200L) {
                    // Verificar si hay reglas para este paquete O si el engine tiene perfil
                    val rules = withContext(Dispatchers.IO) {
                        inAppBlockRepository.getEnabledRulesForPackage(packageName)
                    }
                    val hasEngineProfile = detectionEngine.hasProfile(packageName)
                    
                    if (rules.isEmpty() && !hasEngineProfile) return@withTimeoutOrNull

                    val rootNode = try {
                        rootInActiveWindow ?: event.source
                    } catch (e: Exception) { null } ?: return@withTimeoutOrNull

                    try {
                        // ═══ DETECCIÓN V2: Motor multi-señal ═══
                        if (hasEngineProfile) {
                            val enabledRuleIds = rules.map { it.ruleId }.toSet()
                            val result = detectionEngine.quickDetect(rootNode, packageName, enabledRuleIds)
                            
                            if (result != null && result.confidence > 0.5f) {
                                lastDetectionResult = result
                                lastDetectionPackage = packageName
                                lastDetectionTime = currentTime
                                
                                handleAdvancedDetection(packageName, result, rootNode)
                            }
                        }
                        
                        // ═══ FALLBACK: Reglas clásicas para compatibilidad ═══
                        if (lastDetectionResult == null || lastDetectionPackage != packageName) {
                            for (rule in rules) {
                                if (shouldBlockContent(rootNode, rule.ruleId)) {
                                    handleBlock(rule.appName, rule.featureName)
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error checking block content", e)
                    } finally {
                        try { rootNode.recycle() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando evento de accesibilidad", e)
            }
        }
    }

    /**
     * Maneja detecciones del motor avanzado con bloqueo gradual.
     */
    private suspend fun handleAdvancedDetection(
        packageName: String,
        result: DetectionResult,
        rootNode: AccessibilityNodeInfo
    ) {
        val contentTypeName = result.contentType.name
        val confidencePercent = (result.confidence * 100).toInt()
        
        Log.d(TAG, "🎯 Detección: $contentTypeName en $packageName (confianza: $confidencePercent%)")
        
        // Verificar si este tipo de contenido debe bloquearse
        val shouldBlock = withContext(Dispatchers.IO) {
            val ruleId = mapContentTypeToRuleId(packageName, result.contentType)
            if (ruleId != null) {
                val rules = inAppBlockRepository.getEnabledRulesForPackage(packageName)
                rules.any { it.ruleId == ruleId }
            } else {
                // Si no hay regla específica pero el perfil adaptativo lo bloquea
                adaptiveBlockingManager.shouldBlockContentType(packageName, result.contentType)
            }
        }
        
        if (!shouldBlock) return
        
        // === BLOQUEO GRADUAL ===
        val stage = adaptiveBlockingManager.getCurrentGradualStage(packageName)
        val action = stage?.action ?: GradualAction.HARD_BLOCK
        
        when (action) {
            GradualAction.GENTLE_REMINDER -> {
                // Solo registrar, no bloquear
                Log.d(TAG, "📝 Recordatorio suave para $packageName")
                adaptiveBlockingManager.trackEvent(packageName, BlockingEventType.GRADUAL_WARNING_SHOWN,
                    "Gentle: $contentTypeName (${confidencePercent}%)")
            }
            GradualAction.WARNING_OVERLAY -> {
                // Mostrar advertencia pero no bloquear
                val appName = getAppNameFromPackage(packageName)
                showBlockScreen(appName, "Advertencia: $contentTypeName detectado", isWarning = true)
                adaptiveBlockingManager.trackEvent(packageName, BlockingEventType.GRADUAL_WARNING_SHOWN,
                    "Warning: $contentTypeName")
            }
            GradualAction.BLOCK_INFINITE_SCROLL -> {
                if (result.isInfiniteScroll) {
                    handleBlock(getAppNameFromPackage(packageName), "Scroll infinito bloqueado")
                    adaptiveBlockingManager.trackEvent(packageName, BlockingEventType.FEATURE_BLOCKED,
                        "Infinite scroll blocked")
                }
            }
            GradualAction.SLOW_DOWN_APP -> {
                // Aplicar delay antes del bloqueo (efecto disuasorio)
                delay(2000)
                handleBlock(getAppNameFromPackage(packageName), contentTypeName)
                adaptiveBlockingManager.trackEvent(packageName, BlockingEventType.GRADUAL_SLOWDOWN_APPLIED,
                    "Slowdown: $contentTypeName")
            }
            GradualAction.SOFT_BLOCK, GradualAction.HARD_BLOCK, 
            GradualAction.NUCLEAR_BLOCK, GradualAction.GRAYSCALE_CONTENT,
            GradualAction.BLOCK_NEW_CONTENT -> {
                // Bloqueo completo
                handleBlock(getAppNameFromPackage(packageName), contentTypeName)
                adaptiveBlockingManager.trackEvent(packageName, BlockingEventType.FEATURE_BLOCKED,
                    "$action: $contentTypeName (${confidencePercent}%)")
            }
        }
        
        // Detección extra: video reproduciéndose
        if (result.isVideoPlaying) {
            Log.d(TAG, "🎬 Video reproduciéndose en $packageName")
        }
    }
    
    /**
     * Mapea ContentType del engine a ruleId para compatibilidad con reglas existentes.
     */
    private fun mapContentTypeToRuleId(packageName: String, contentType: ContentType): String? {
        return when {
            packageName.contains("instagram") -> when (contentType) {
                ContentType.SHORT_VIDEO -> "instagram_reels"
                ContentType.EXPLORE_FEED -> "instagram_explore"
                ContentType.STORIES -> "instagram_stories"
                ContentType.SHOPPING -> "instagram_shopping"
                else -> null
            }
            packageName.contains("youtube") -> when (contentType) {
                ContentType.SHORT_VIDEO -> "youtube_shorts"
                ContentType.SEARCH_RESULTS -> "youtube_search"
                ContentType.LIVE_STREAM -> "youtube_live"
                ContentType.MAIN_FEED -> "youtube_feed"
                else -> null
            }
            packageName.contains("facebook") || packageName.contains("katana") -> when (contentType) {
                ContentType.SHORT_VIDEO -> "facebook_reels"
                ContentType.MAIN_FEED -> "facebook_feed"
                ContentType.SHOPPING -> "facebook_marketplace"
                ContentType.GAMING -> "facebook_gaming"
                else -> null
            }
            packageName.contains("tiktok") || packageName.contains("musically") || packageName.contains("trill") -> when (contentType) {
                ContentType.SHORT_VIDEO, ContentType.MAIN_FEED -> "tiktok_foryou"
                ContentType.EXPLORE_FEED -> "tiktok_explore"
                ContentType.LIVE_STREAM -> "tiktok_live"
                ContentType.SHOPPING -> "tiktok_shop"
                else -> null
            }
            packageName.contains("snapchat") -> when (contentType) {
                ContentType.EXPLORE_FEED -> "snapchat_discover"
                ContentType.SHORT_VIDEO -> "snapchat_spotlight"
                else -> null
            }
            packageName.contains("twitter") || packageName.contains("com.x.android") -> when (contentType) {
                ContentType.EXPLORE_FEED -> "x_explore"
                ContentType.MAIN_FEED -> "x_feed"
                else -> null
            }
            packageName.contains("reddit") -> when (contentType) {
                ContentType.MAIN_FEED -> "reddit_feed"
                ContentType.EXPLORE_FEED -> "reddit_explore"
                else -> null
            }
            else -> null
        }
    }

    private fun handleBlock(appName: String, featureName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime > BLOCK_COOLDOWN) {
            lastBlockTime = currentTime
            
            Log.d(TAG, "🚫 BLOQUEO: $featureName en $appName")
            
            performGlobalAction(GLOBAL_ACTION_BACK)
            showBlockScreen(appName, featureName)
        }
    }

    // === DETECCIÓN CLÁSICA (FALLBACK) ===
    // Se mantiene para compatibilidad con reglas existentes que no tienen perfil en el engine
    
    private fun shouldBlockContent(rootNode: AccessibilityNodeInfo, ruleId: String): Boolean {
        return when (ruleId) {
            "instagram_reels" -> detectInstagramReels(rootNode)
            "instagram_explore" -> detectInstagramExplore(rootNode)
            "youtube_shorts" -> detectYouTubeShorts(rootNode)
            "youtube_search" -> detectYouTubeSearch(rootNode)
            "facebook_reels" -> detectFacebookReels(rootNode)
            "snapchat_discover" -> detectSnapchatDiscover(rootNode)
            "tiktok_foryou" -> detectTikTokForYou(rootNode)
            "x_explore" -> detectXExplore(rootNode)
            else -> false
        }
    }

    private fun detectInstagramReels(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasViewId(rootNode, "clips_video_container")) return true
        if (hasSelectedViewId(rootNode, "clips_tab")) return true
        if (hasSelectedContentDescription(rootNode, "Reels tab")) return true
        if (hasSelectedContentDescription(rootNode, "Pestaña Reels")) return true
        if (hasSelectedText(rootNode, "Reels")) return true
        if (hasSelectedText(rootNode, "Reel")) return true
        return false
    }

    private fun detectInstagramExplore(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Explorar") || hasText(rootNode, "Explore")) return true
        if (hasViewId(rootNode, "search_tab")) return true
        if (hasContentDescription(rootNode, "Search and explore")) return true
        if (hasContentDescription(rootNode, "Buscar y explorar")) return true
        return false
    }

    private fun detectYouTubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Shorts")) return true
        if (hasViewId(rootNode, "shorts_container")) return true
        if (hasViewId(rootNode, "shorts_player")) return true
        if (hasContentDescription(rootNode, "Shorts tab")) return true
        return false
    }

    private fun detectYouTubeSearch(rootNode: AccessibilityNodeInfo): Boolean {
        val isSearching = hasViewId(rootNode, "search_edit_text") || 
                          hasText(rootNode, "Search YouTube") ||
                          hasText(rootNode, "Buscar en YouTube")
        val hasResults = hasText(rootNode, "Resultados de búsqueda") ||
                         hasText(rootNode, "Search results")
        return isSearching || hasResults
    }

    private fun detectFacebookReels(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasSelectedText(rootNode, "Reels")) return true
        if (hasSelectedContentDescription(rootNode, "Reels")) return true
        return false
    }

    private fun detectSnapchatDiscover(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Discover") || hasText(rootNode, "Descubrir")) return true
        if (hasContentDescription(rootNode, "Discover Page")) return true
        return false
    }

    private fun detectTikTokForYou(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Para ti") || hasText(rootNode, "For You")) return true
        return false
    }

    private fun detectXExplore(rootNode: AccessibilityNodeInfo): Boolean {
        if (hasText(rootNode, "Explore") || hasText(rootNode, "Explorar")) return true
        if (hasText(rootNode, "Trending") || hasText(rootNode, "Tendencias")) return true
        return false
    }

    // === BÚSQUEDA RECURSIVA (mantenida para fallback) ===

    private fun hasText(rootNode: AccessibilityNodeInfo?, text: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.text?.contains(text, ignoreCase = true) == true) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasText(child, text, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    private fun hasViewId(rootNode: AccessibilityNodeInfo?, viewIdPart: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.viewIdResourceName?.contains(viewIdPart, ignoreCase = true) == true) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasViewId(child, viewIdPart, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    private fun hasContentDescription(rootNode: AccessibilityNodeInfo?, description: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.contentDescription?.contains(description, ignoreCase = true) == true) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasContentDescription(child, description, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedText(rootNode: AccessibilityNodeInfo?, text: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.text?.contains(text, ignoreCase = true) == true && (rootNode.isSelected || rootNode.isChecked)) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedText(child, text, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedContentDescription(rootNode: AccessibilityNodeInfo?, description: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.contentDescription?.contains(description, ignoreCase = true) == true && (rootNode.isSelected || rootNode.isChecked)) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedContentDescription(child, description, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    private fun hasSelectedViewId(rootNode: AccessibilityNodeInfo?, viewIdPart: String, depth: Int = 0): Boolean {
        if (rootNode == null || depth > 20) return false
        if (rootNode.viewIdResourceName?.contains(viewIdPart, ignoreCase = true) == true && (rootNode.isSelected || rootNode.isChecked)) return true
        val count = rootNode.childCount
        for (i in 0 until count) {
            val child = rootNode.getChild(i)
            if (child != null) {
                if (hasSelectedViewId(child, viewIdPart, depth + 1)) { child.recycle(); return true }
                child.recycle()
            }
        }
        return false
    }

    // === UTILIDADES ===

    private fun showBlockScreen(appName: String, featureName: String, isWarning: Boolean = false) {
        try {
            val intent = Intent(this, InAppBlockedActivity::class.java).apply {
                putExtra("app_name", appName)
                putExtra("feature_name", featureName)
                putExtra("is_warning", isWarning)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando pantalla de bloqueo", e)
        }
    }
    
    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = this.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) { packageName.substringAfterLast(".") }
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

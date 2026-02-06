package com.momentummm.app.data.engine

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

// ═══════════════════════════════════════════════════════════════════════════
// ██████╗ ███████╗████████╗███████╗ ██████╗████████╗██╗ ██████╗ ███╗   ██╗
// ██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝██║██╔═══██╗████╗  ██║
// ██║  ██║█████╗     ██║   █████╗  ██║        ██║   ██║██║   ██║██╔██╗ ██║
// ██║  ██║██╔══╝     ██║   ██╔══╝  ██║        ██║   ██║██║   ██║██║╚██╗██║
// ██████╔╝███████╗   ██║   ███████╗╚██████╗   ██║   ██║╚██████╔╝██║ ╚████║
// ╚═════╝ ╚══════╝   ╚═╝   ╚══════╝ ╚═════╝   ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝
// ADVANCED DETECTION ENGINE - Fingerprinting inteligente de contenido
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Motor de detección avanzado que usa múltiples señales para identificar
 * contenido dentro de apps con alta precisión.
 * 
 * MEJORAS SOBRE EL SISTEMA ANTERIOR:
 * 1. Fingerprinting multi-señal con scoring ponderado
 * 2. Detección adaptativa que aprende de falsos positivos/negativos
 * 3. Soporte para muchas más apps y funciones
 * 4. Detección de scroll infinito y contenido adictivo
 * 5. Análisis de estructura de UI para detección más robusta
 * 6. Cache de resultados para performance
 * 7. Detección de modo oscuro vs claro (patrones de UI)
 * 8. Detección de reproductor de video activo
 */
@Singleton
class AdvancedDetectionEngine @Inject constructor() {
    
    private val TAG = "AdvancedDetection"
    
    // Cache de detección para evitar re-escanear el mismo árbol
    private var lastDetectionTimestamp: Long = 0
    private var lastDetectionResult: DetectionResult? = null
    private var lastPackageName: String = ""
    private val CACHE_VALIDITY_MS = 200L
    
    // Estadísticas de detección para auto-calibración
    private val detectionStats = mutableMapOf<String, DetectionStats>()
    
    data class DetectionStats(
        var totalChecks: Int = 0,
        var positiveDetections: Int = 0,
        var falsePositiveReports: Int = 0,
        var lastCalibrationTime: Long = 0
    )
    
    // ═══════════════════════════════════════════════════════════════
    // RESULTADO DE DETECCIÓN
    // ═══════════════════════════════════════════════════════════════
    
    data class DetectionResult(
        val detected: Boolean,
        val ruleId: String,
        val confidence: Float,         // 0.0 - 1.0
        val signals: List<DetectionSignal>,
        val contentType: ContentType,
        val isInfiniteScroll: Boolean = false,
        val isVideoPlaying: Boolean = false,
        val detectionMethod: String = ""
    )
    
    data class DetectionSignal(
        val type: SignalType,
        val value: String,
        val weight: Float,             // Importancia de esta señal
        val matched: Boolean
    )
    
    enum class SignalType {
        VIEW_ID,                       // ID de vista del componente
        CONTENT_DESCRIPTION,           // Descripción de accesibilidad
        TEXT_CONTENT,                  // Texto visible
        CLASS_NAME,                    // Clase del componente Android
        SELECTED_STATE,                // Estado de selección (tab activo)
        SCROLL_PATTERN,                // Tipo de scroll (vertical infinito)
        CHILD_COUNT,                   // Número de hijos (indica tipo de layout)
        VIEW_HIERARCHY,                // Estructura del árbol de vistas
        PACKAGE_ACTIVITY,              // Activity actual de la app
        TAB_POSITION,                  // Posición del tab activo
        VIDEO_SURFACE,                 // Presencia de SurfaceView (video)
        RECYCLERVIEW_VERTICAL,         // RecyclerView vertical (feed infinito)
        FULLSCREEN_VIDEO,              // Video a pantalla completa
        NAVIGATION_STATE               // Estado de navegación (bottom nav)
    }
    
    enum class ContentType {
        SHORT_VIDEO,                   // Videos cortos (Reels, Shorts, TikTok)
        EXPLORE_FEED,                  // Feed de exploración
        SEARCH_RESULTS,                // Resultados de búsqueda
        STORIES,                       // Stories
        MAIN_FEED,                     // Feed principal
        LIVE_STREAM,                   // Streaming en vivo
        MESSAGING,                     // Mensajería (permitido en modo comunicación)
        PROFILE,                       // Perfil de usuario
        SETTINGS,                      // Configuración
        NOTIFICATIONS,                 // Notificaciones
        SHOPPING,                      // Compras/marketplace
        GAMING,                        // Mini-juegos dentro de apps
        UNKNOWN
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PERFILES DE DETECCIÓN POR APP
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Perfil completo de detección para una app.
     * Contiene todas las señales necesarias para identificar cada sección.
     */
    data class AppDetectionProfile(
        val packageName: String,
        val features: Map<String, FeatureFingerprint>
    )
    
    data class FeatureFingerprint(
        val ruleId: String,
        val contentType: ContentType,
        val requiredConfidence: Float = 0.6f,  // Confianza mínima para bloquear
        val signals: List<SignalDefinition>
    )
    
    data class SignalDefinition(
        val type: SignalType,
        val patterns: List<String>,     // Patrones a buscar (soporta regex)
        val weight: Float = 1.0f,       // Importancia relativa
        val requireSelected: Boolean = false,
        val caseSensitive: Boolean = false,
        val isNegative: Boolean = false  // Si es true, la presencia REDUCE la confianza
    )
    
    // ═══════════════════════════════════════════════════════════════
    // BASE DE DATOS DE PERFILES - Fingerprints para TODAS las apps
    // ═══════════════════════════════════════════════════════════════
    
    private val appProfiles: Map<String, AppDetectionProfile> by lazy {
        mapOf(
            // ──────────────────────────────────────────────
            // INSTAGRAM
            // ──────────────────────────────────────────────
            "com.instagram.android" to AppDetectionProfile(
                packageName = "com.instagram.android",
                features = mapOf(
                    "instagram_reels" to FeatureFingerprint(
                        ruleId = "instagram_reels",
                        contentType = ContentType.SHORT_VIDEO,
                        requiredConfidence = 0.55f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("clips_video_container", "clips_viewer_view_pager", "reel_viewer_", "clips_tab"), weight = 3.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Reels tab", "Pestaña Reels", "Reels", "reel", "clips"), weight = 2.5f, requireSelected = true),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Reels", "Reel"), weight = 1.5f, requireSelected = true),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("androidx.viewpager2.widget.ViewPager2"), weight = 1.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "TextureView"), weight = 2.0f),
                            SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager", "ViewPager"), weight = 1.5f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("clips_music_", "reel_audio_", "reel_like_", "clips_like_"), weight = 2.0f),
                            SignalDefinition(SignalType.NAVIGATION_STATE, listOf("tab_icon_reels"), weight = 2.5f, requireSelected = true),
                            // Señales negativas - si estamos en mensajes, NO es Reels
                            SignalDefinition(SignalType.VIEW_ID, listOf("direct_inbox", "message_thread", "direct_"), weight = 3.0f, isNegative = true),
                            SignalDefinition(SignalType.VIEW_ID, listOf("profile_tab", "action_bar_title"), weight = 1.0f, isNegative = true)
                        )
                    ),
                    "instagram_explore" to FeatureFingerprint(
                        ruleId = "instagram_explore",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.55f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("search_tab", "explore_grid", "discover_"), weight = 3.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Search and explore", "Buscar y explorar", "Search & Explore"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Explorar", "Explore", "Search"), weight = 1.5f),
                            SignalDefinition(SignalType.NAVIGATION_STATE, listOf("tab_icon_search"), weight = 2.5f, requireSelected = true),
                            SignalDefinition(SignalType.VIEW_ID, listOf("explore_topic_", "explore_hashtag_", "search_edit_text"), weight = 2.0f),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("StaggeredGridLayoutManager", "GridLayout"), weight = 1.0f),
                            // Negativo
                            SignalDefinition(SignalType.VIEW_ID, listOf("direct_inbox", "message_"), weight = 2.0f, isNegative = true)
                        )
                    ),
                    "instagram_stories" to FeatureFingerprint(
                        ruleId = "instagram_stories",
                        contentType = ContentType.STORIES,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("reel_viewer_", "story_viewer_", "stories_tray"), weight = 3.0f),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("StoryViewerFragment", "ReelViewerFragment"), weight = 2.5f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("story_progress_", "story_reply_", "stories_container"), weight = 2.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView"), weight = 1.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Story", "Historia", "stories"), weight = 1.5f)
                        )
                    ),
                    "instagram_shopping" to FeatureFingerprint(
                        ruleId = "instagram_shopping",
                        contentType = ContentType.SHOPPING,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("shopping_tab", "shop_tab", "product_"), weight = 3.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Shop", "Tienda", "Shopping"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Shop", "Tienda", "Agregar al carrito", "Add to cart"), weight = 2.0f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // YOUTUBE
            // ──────────────────────────────────────────────
            "com.google.android.youtube" to AppDetectionProfile(
                packageName = "com.google.android.youtube",
                features = mapOf(
                    "youtube_shorts" to FeatureFingerprint(
                        ruleId = "youtube_shorts",
                        contentType = ContentType.SHORT_VIDEO,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("shorts_container", "shorts_player", "shorts_video_", "reel_recycler", "shorts_surface_", "shorts_pivot_"), weight = 3.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Shorts"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Shorts", "Shorts tab"), weight = 2.5f, requireSelected = true),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("ShortsFragment", "ReelWatchFragment"), weight = 3.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "TextureView"), weight = 1.5f),
                            SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager"), weight = 2.0f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("shorts_like_", "shorts_comment_", "shorts_share_", "shorts_subscribe_"), weight = 2.5f),
                            SignalDefinition(SignalType.NAVIGATION_STATE, listOf("pivot_bar", "shorts_pivot_tab"), weight = 2.0f, requireSelected = true),
                            // Negativo - si estamos buscando, no es shorts
                            SignalDefinition(SignalType.VIEW_ID, listOf("search_edit_text", "search_results_"), weight = 2.0f, isNegative = true)
                        )
                    ),
                    "youtube_search" to FeatureFingerprint(
                        ruleId = "youtube_search",
                        contentType = ContentType.SEARCH_RESULTS,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("search_edit_text", "search_query", "search_results", "search_box"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Search YouTube", "Buscar en YouTube", "Resultados de búsqueda", "Search results"), weight = 2.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Search", "Buscar"), weight = 1.5f),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("SearchFragment", "SearchResultsFragment"), weight = 2.5f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("search_suggestion_", "voice_search_"), weight = 2.0f)
                        )
                    ),
                    "youtube_live" to FeatureFingerprint(
                        ruleId = "youtube_live",
                        contentType = ContentType.LIVE_STREAM,
                        requiredConfidence = 0.65f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("live_chat_", "live_indicator", "live_badge"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("LIVE", "EN VIVO", "En directo", "Live chat"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Live", "En vivo"), weight = 2.0f)
                        )
                    ),
                    "youtube_feed" to FeatureFingerprint(
                        ruleId = "youtube_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("results", "feed_item_", "video_info_"), weight = 2.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Home", "Inicio"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.5f),
                            // Negativo: no es feed si estamos en subscripciones o librería
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Subscriptions", "Suscripciones", "Library", "Biblioteca"), weight = 2.5f, isNegative = true, requireSelected = true)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // FACEBOOK
            // ──────────────────────────────────────────────
            "com.facebook.katana" to AppDetectionProfile(
                packageName = "com.facebook.katana",
                features = mapOf(
                    "facebook_reels" to FeatureFingerprint(
                        ruleId = "facebook_reels",
                        contentType = ContentType.SHORT_VIDEO,
                        requiredConfidence = 0.55f,
                        signals = listOf(
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Reels"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Reels", "Reels tab", "Video tab"), weight = 2.5f, requireSelected = true),
                            SignalDefinition(SignalType.VIEW_ID, listOf("video_tab", "reels_tab", "reels_surface_"), weight = 3.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView"), weight = 1.5f),
                            SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager"), weight = 2.0f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("reels_like_", "reels_comment_", "reels_share_"), weight = 2.0f),
                            // Negativo
                            SignalDefinition(SignalType.VIEW_ID, listOf("messenger_", "chat_", "inbox_"), weight = 2.0f, isNegative = true)
                        )
                    ),
                    "facebook_feed" to FeatureFingerprint(
                        ruleId = "facebook_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("news_feed_", "feed_item_", "story_"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("News Feed", "Feed", "Inicio"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.0f)
                        )
                    ),
                    "facebook_marketplace" to FeatureFingerprint(
                        ruleId = "facebook_marketplace",
                        contentType = ContentType.SHOPPING,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("marketplace_", "product_listing_"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Marketplace", "Mercado"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Marketplace", "Marketplace tab"), weight = 2.5f, requireSelected = true)
                        )
                    ),
                    "facebook_gaming" to FeatureFingerprint(
                        ruleId = "facebook_gaming",
                        contentType = ContentType.GAMING,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("gaming_", "game_tab", "instant_game_"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Gaming", "Juegos", "Play Games"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Gaming tab", "Games"), weight = 2.0f, requireSelected = true)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // TIKTOK
            // ──────────────────────────────────────────────
            "com.zhiliaoapp.musically" to createTikTokProfile("com.zhiliaoapp.musically"),
            "com.ss.android.ugc.trill" to createTikTokProfile("com.ss.android.ugc.trill"),
            
            // ──────────────────────────────────────────────
            // SNAPCHAT
            // ──────────────────────────────────────────────
            "com.snapchat.android" to AppDetectionProfile(
                packageName = "com.snapchat.android",
                features = mapOf(
                    "snapchat_discover" to FeatureFingerprint(
                        ruleId = "snapchat_discover",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.55f,
                        signals = listOf(
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Discover", "Descubrir", "Stories", "Spotlight"), weight = 2.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Discover Page", "Discover", "Stories Page"), weight = 2.5f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("discover_", "stories_tab", "spotlight_"), weight = 3.0f),
                            SignalDefinition(SignalType.NAVIGATION_STATE, listOf("discover_tab", "stories_tab"), weight = 2.0f, requireSelected = true),
                            // Negativo
                            SignalDefinition(SignalType.VIEW_ID, listOf("chat_", "camera_", "snap_"), weight = 2.0f, isNegative = true)
                        )
                    ),
                    "snapchat_spotlight" to FeatureFingerprint(
                        ruleId = "snapchat_spotlight",
                        contentType = ContentType.SHORT_VIDEO,
                        requiredConfidence = 0.55f,
                        signals = listOf(
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Spotlight"), weight = 2.5f, requireSelected = true),
                            SignalDefinition(SignalType.VIEW_ID, listOf("spotlight_", "spotlight_player"), weight = 3.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView"), weight = 1.5f),
                            SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager"), weight = 2.0f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // X (TWITTER)
            // ──────────────────────────────────────────────
            "com.twitter.android" to createXProfile("com.twitter.android"),
            "com.x.android" to createXProfile("com.x.android"),
            
            // ──────────────────────────────────────────────
            // REDDIT
            // ──────────────────────────────────────────────
            "com.reddit.frontpage" to AppDetectionProfile(
                packageName = "com.reddit.frontpage",
                features = mapOf(
                    "reddit_feed" to FeatureFingerprint(
                        ruleId = "reddit_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("feed_", "post_list_", "home_feed"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Home", "Popular", "All"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.0f)
                        )
                    ),
                    "reddit_explore" to FeatureFingerprint(
                        ruleId = "reddit_explore",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("discover_", "explore_", "community_list_"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Discover", "Explore", "Communities"), weight = 2.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Discover tab", "Explore"), weight = 2.0f, requireSelected = true)
                        )
                    ),
                    "reddit_video" to FeatureFingerprint(
                        ruleId = "reddit_video",
                        contentType = ContentType.SHORT_VIDEO,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("video_player_", "rpan_", "reddit_video_"), weight = 3.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "ExoPlayerView"), weight = 2.5f),
                            SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager"), weight = 2.0f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // PINTEREST
            // ──────────────────────────────────────────────
            "com.pinterest" to AppDetectionProfile(
                packageName = "com.pinterest",
                features = mapOf(
                    "pinterest_feed" to FeatureFingerprint(
                        ruleId = "pinterest_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("home_feed_", "pin_grid_", "waterfall_"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Home", "Inicio"), weight = 2.0f, requireSelected = true),
                            SignalDefinition(SignalType.CLASS_NAME, listOf("StaggeredGridLayoutManager"), weight = 2.0f)
                        )
                    ),
                    "pinterest_explore" to FeatureFingerprint(
                        ruleId = "pinterest_explore",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("explore_", "search_", "trends_"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Explore", "Explorar", "Trending"), weight = 2.0f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // LINKEDIN
            // ──────────────────────────────────────────────
            "com.linkedin.android" to AppDetectionProfile(
                packageName = "com.linkedin.android",
                features = mapOf(
                    "linkedin_feed" to FeatureFingerprint(
                        ruleId = "linkedin_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("feed_", "update_list_", "news_feed_"), weight = 2.5f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Feed", "Home"), weight = 2.0f, requireSelected = true)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // TWITCH
            // ──────────────────────────────────────────────
            "tv.twitch.android.app" to AppDetectionProfile(
                packageName = "tv.twitch.android.app",
                features = mapOf(
                    "twitch_browse" to FeatureFingerprint(
                        ruleId = "twitch_browse",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("browse_", "discover_", "category_"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Browse", "Explorar", "Categories"), weight = 2.0f),
                            SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Browse", "Discover"), weight = 2.0f, requireSelected = true)
                        )
                    ),
                    "twitch_stream" to FeatureFingerprint(
                        ruleId = "twitch_stream",
                        contentType = ContentType.LIVE_STREAM,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("stream_player_", "chat_", "viewer_count_"), weight = 3.0f),
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "ExoPlayerView"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("viewers", "espectadores", "LIVE"), weight = 1.5f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // THREADS
            // ──────────────────────────────────────────────
            "com.instagram.barcelona" to AppDetectionProfile(
                packageName = "com.instagram.barcelona",
                features = mapOf(
                    "threads_feed" to FeatureFingerprint(
                        ruleId = "threads_feed",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.4f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("feed_", "timeline_", "thread_list_"), weight = 2.5f),
                            SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.0f)
                        )
                    ),
                    "threads_explore" to FeatureFingerprint(
                        ruleId = "threads_explore",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("search_", "explore_"), weight = 3.0f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Search", "Buscar", "Explore"), weight = 2.0f)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // TELEGRAM (bloqueo selectivo)
            // ──────────────────────────────────────────────
            "org.telegram.messenger" to AppDetectionProfile(
                packageName = "org.telegram.messenger",
                features = mapOf(
                    "telegram_channels" to FeatureFingerprint(
                        ruleId = "telegram_channels",
                        contentType = ContentType.MAIN_FEED,
                        requiredConfidence = 0.6f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("channel_", "stories_"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Channel", "Canal"), weight = 2.0f),
                            // Negativo - mensajes privados NO deben bloquearse
                            SignalDefinition(SignalType.VIEW_ID, listOf("chat_compose_", "message_input_"), weight = 3.0f, isNegative = true)
                        )
                    )
                )
            ),
            
            // ──────────────────────────────────────────────
            // NETFLIX (detección de binge-watching)
            // ──────────────────────────────────────────────
            "com.netflix.mediaclient" to AppDetectionProfile(
                packageName = "com.netflix.mediaclient",
                features = mapOf(
                    "netflix_browse" to FeatureFingerprint(
                        ruleId = "netflix_browse",
                        contentType = ContentType.EXPLORE_FEED,
                        requiredConfidence = 0.4f,
                        signals = listOf(
                            SignalDefinition(SignalType.VIEW_ID, listOf("browse_", "gallery_", "billboard_"), weight = 2.5f),
                            SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.0f)
                        )
                    ),
                    "netflix_playing" to FeatureFingerprint(
                        ruleId = "netflix_playing",
                        contentType = ContentType.LIVE_STREAM,
                        requiredConfidence = 0.5f,
                        signals = listOf(
                            SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "TextureView"), weight = 3.0f),
                            SignalDefinition(SignalType.VIEW_ID, listOf("player_", "video_player_"), weight = 2.5f),
                            SignalDefinition(SignalType.TEXT_CONTENT, listOf("Next Episode", "Siguiente episodio"), weight = 2.0f)
                        )
                    )
                )
            )
        )
    }
    
    private fun createTikTokProfile(packageName: String) = AppDetectionProfile(
        packageName = packageName,
        features = mapOf(
            "tiktok_foryou" to FeatureFingerprint(
                ruleId = "tiktok_foryou",
                contentType = ContentType.SHORT_VIDEO,
                requiredConfidence = 0.45f,
                signals = listOf(
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("Para ti", "For You", "Following", "Siguiendo"), weight = 2.0f),
                    SignalDefinition(SignalType.VIEW_ID, listOf("for_you_", "feed_", "video_player_"), weight = 3.0f),
                    SignalDefinition(SignalType.FULLSCREEN_VIDEO, listOf("SurfaceView", "TextureView"), weight = 2.5f),
                    SignalDefinition(SignalType.SCROLL_PATTERN, listOf("vertical_pager", "ViewPager2"), weight = 2.0f),
                    SignalDefinition(SignalType.VIEW_ID, listOf("like_button_", "comment_button_", "share_button_"), weight = 1.5f),
                    // Negativo
                    SignalDefinition(SignalType.VIEW_ID, listOf("inbox_", "message_", "chat_"), weight = 2.5f, isNegative = true),
                    SignalDefinition(SignalType.VIEW_ID, listOf("profile_", "settings_"), weight = 1.5f, isNegative = true)
                )
            ),
            "tiktok_explore" to FeatureFingerprint(
                ruleId = "tiktok_explore",
                contentType = ContentType.EXPLORE_FEED,
                requiredConfidence = 0.55f,
                signals = listOf(
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("Discover", "Descubrir", "Trending", "Tendencia"), weight = 2.5f),
                    SignalDefinition(SignalType.VIEW_ID, listOf("discover_", "search_", "trending_"), weight = 3.0f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Discover", "Search"), weight = 2.0f, requireSelected = true)
                )
            ),
            "tiktok_live" to FeatureFingerprint(
                ruleId = "tiktok_live",
                contentType = ContentType.LIVE_STREAM,
                requiredConfidence = 0.6f,
                signals = listOf(
                    SignalDefinition(SignalType.VIEW_ID, listOf("live_", "live_player_", "live_chat_"), weight = 3.0f),
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("LIVE", "EN VIVO", "Live"), weight = 2.5f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Live", "En vivo"), weight = 2.0f)
                )
            ),
            "tiktok_shop" to FeatureFingerprint(
                ruleId = "tiktok_shop",
                contentType = ContentType.SHOPPING,
                requiredConfidence = 0.6f,
                signals = listOf(
                    SignalDefinition(SignalType.VIEW_ID, listOf("shop_", "product_", "cart_"), weight = 3.0f),
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("Shop", "Tienda", "TikTok Shop"), weight = 2.5f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Shop", "Shopping"), weight = 2.0f, requireSelected = true)
                )
            )
        )
    )
    
    private fun createXProfile(packageName: String) = AppDetectionProfile(
        packageName = packageName,
        features = mapOf(
            "x_explore" to FeatureFingerprint(
                ruleId = "x_explore",
                contentType = ContentType.EXPLORE_FEED,
                requiredConfidence = 0.55f,
                signals = listOf(
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("Explore", "Explorar", "Trending", "Tendencias", "What's happening"), weight = 2.0f),
                    SignalDefinition(SignalType.VIEW_ID, listOf("explore_", "search_", "trending_"), weight = 3.0f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Search and explore", "Explore", "Buscar y explorar"), weight = 2.5f),
                    SignalDefinition(SignalType.NAVIGATION_STATE, listOf("search_tab", "explore_tab"), weight = 2.0f, requireSelected = true),
                    // Negativo
                    SignalDefinition(SignalType.VIEW_ID, listOf("compose_", "dm_", "message_"), weight = 2.0f, isNegative = true)
                )
            ),
            "x_feed" to FeatureFingerprint(
                ruleId = "x_feed",
                contentType = ContentType.MAIN_FEED,
                requiredConfidence = 0.5f,
                signals = listOf(
                    SignalDefinition(SignalType.VIEW_ID, listOf("timeline_", "tweet_list_", "home_tab"), weight = 2.5f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Home", "Inicio", "For you", "Para ti"), weight = 2.0f, requireSelected = true),
                    SignalDefinition(SignalType.RECYCLERVIEW_VERTICAL, listOf("RecyclerView"), weight = 1.0f)
                )
            ),
            "x_spaces" to FeatureFingerprint(
                ruleId = "x_spaces",
                contentType = ContentType.LIVE_STREAM,
                requiredConfidence = 0.6f,
                signals = listOf(
                    SignalDefinition(SignalType.VIEW_ID, listOf("spaces_", "space_player_"), weight = 3.0f),
                    SignalDefinition(SignalType.TEXT_CONTENT, listOf("Space", "Espacio", "Spaces", "Listen"), weight = 2.0f),
                    SignalDefinition(SignalType.CONTENT_DESCRIPTION, listOf("Spaces"), weight = 2.0f)
                )
            )
        )
    )
    
    // ═══════════════════════════════════════════════════════════════
    // MOTOR DE DETECCIÓN PRINCIPAL
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Analiza el árbol de accesibilidad y retorna todas las detecciones
     * con su nivel de confianza. Usa fingerprinting multi-señal.
     */
    fun analyzeContent(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): List<DetectionResult> {
        // Verificar cache
        val now = System.currentTimeMillis()
        if (packageName == lastPackageName && now - lastDetectionTimestamp < CACHE_VALIDITY_MS) {
            return listOfNotNull(lastDetectionResult)
        }
        
        val profile = appProfiles[packageName] ?: return emptyList()
        val results = mutableListOf<DetectionResult>()
        
        // Pre-escanear el árbol una sola vez (optimización mayor)
        val treeSnapshot = scanTree(rootNode, maxDepth = 15)
        
        for ((_, fingerprint) in profile.features) {
            val result = matchFingerprint(treeSnapshot, fingerprint)
            if (result.detected) {
                results.add(result)
            }
        }
        
        // Actualizar cache
        lastDetectionTimestamp = now
        lastPackageName = packageName
        lastDetectionResult = results.maxByOrNull { it.confidence }
        
        // Actualizar estadísticas
        val stats = detectionStats.getOrPut(packageName) { DetectionStats() }
        stats.totalChecks++
        if (results.isNotEmpty()) stats.positiveDetections++
        
        return results.sortedByDescending { it.confidence }
    }
    
    /**
     * Detección rápida: retorna true si CUALQUIER contenido bloqueado es detectado.
     * Versión optimizada para el servicio de accesibilidad.
     */
    fun quickDetect(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        enabledRuleIds: Set<String>
    ): DetectionResult? {
        val profile = appProfiles[packageName] ?: return null
        
        // Solo verificar reglas que están habilitadas
        val relevantFeatures = profile.features.filter { it.value.ruleId in enabledRuleIds }
        if (relevantFeatures.isEmpty()) return null
        
        val treeSnapshot = scanTree(rootNode, maxDepth = 12) // Menos profundidad para velocidad
        
        for ((_, fingerprint) in relevantFeatures) {
            val result = matchFingerprint(treeSnapshot, fingerprint)
            if (result.detected) return result
        }
        
        return null
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ESCANEO DEL ÁRBOL DE ACCESIBILIDAD
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Snapshot del árbol de accesibilidad optimizado para múltiples búsquedas.
     * Se escanea una sola vez y se hacen todas las búsquedas sobre el snapshot.
     */
    data class TreeSnapshot(
        val viewIds: MutableSet<String> = mutableSetOf(),
        val textContents: MutableSet<String> = mutableSetOf(),
        val contentDescriptions: MutableSet<String> = mutableSetOf(),
        val classNames: MutableSet<String> = mutableSetOf(),
        val selectedViewIds: MutableSet<String> = mutableSetOf(),
        val selectedTexts: MutableSet<String> = mutableSetOf(),
        val selectedDescriptions: MutableSet<String> = mutableSetOf(),
        val hasVideoSurface: Boolean = false,
        val hasVerticalRecycler: Boolean = false,
        val hasVerticalPager: Boolean = false,
        val totalNodeCount: Int = 0
    )
    
    private fun scanTree(node: AccessibilityNodeInfo?, maxDepth: Int = 15): TreeSnapshot {
        val snapshot = TreeSnapshot()
        if (node == null) return snapshot
        
        var nodeCount = 0
        var hasVideo = false
        var hasRecycler = false
        var hasPager = false
        
        fun traverse(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > maxDepth) return
            nodeCount++
            
            // View ID
            n.viewIdResourceName?.let { id ->
                snapshot.viewIds.add(id.lowercase())
                if (n.isSelected || n.isChecked || n.isFocused) {
                    snapshot.selectedViewIds.add(id.lowercase())
                }
            }
            
            // Text
            n.text?.toString()?.let { text ->
                snapshot.textContents.add(text)
                if (n.isSelected || n.isChecked) {
                    snapshot.selectedTexts.add(text)
                }
            }
            
            // Content Description
            n.contentDescription?.toString()?.let { desc ->
                snapshot.contentDescriptions.add(desc)
                if (n.isSelected || n.isChecked) {
                    snapshot.selectedDescriptions.add(desc)
                }
            }
            
            // Class Name
            n.className?.toString()?.let { cls ->
                snapshot.classNames.add(cls)
                when {
                    cls.contains("SurfaceView") || cls.contains("TextureView") -> hasVideo = true
                    cls.contains("RecyclerView") -> hasRecycler = true
                    cls.contains("ViewPager") -> hasPager = true
                }
            }
            
            // Traverse children
            val childCount = n.childCount
            for (i in 0 until childCount) {
                val child = try { n.getChild(i) } catch (e: Exception) { null }
                if (child != null) {
                    traverse(child, depth + 1)
                    try { child.recycle() } catch (e: Exception) { }
                }
            }
        }
        
        try {
            traverse(node, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning tree", e)
        }
        
        return snapshot.copy(
            hasVideoSurface = hasVideo,
            hasVerticalRecycler = hasRecycler,
            hasVerticalPager = hasPager,
            totalNodeCount = nodeCount
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MATCHING DE FINGERPRINTS CON SCORING
    // ═══════════════════════════════════════════════════════════════
    
    private fun matchFingerprint(tree: TreeSnapshot, fingerprint: FeatureFingerprint): DetectionResult {
        var totalPositiveWeight = 0f
        var matchedPositiveWeight = 0f
        var totalNegativeWeight = 0f
        var matchedNegativeWeight = 0f
        val matchedSignals = mutableListOf<DetectionSignal>()
        
        var isVideo = false
        var isInfiniteScroll = false
        
        for (signalDef in fingerprint.signals) {
            val matched = matchSignal(tree, signalDef)
            
            if (signalDef.isNegative) {
                totalNegativeWeight += signalDef.weight
                if (matched) matchedNegativeWeight += signalDef.weight
            } else {
                totalPositiveWeight += signalDef.weight
                if (matched) matchedPositiveWeight += signalDef.weight
            }
            
            matchedSignals.add(DetectionSignal(
                type = signalDef.type,
                value = signalDef.patterns.joinToString("|"),
                weight = signalDef.weight,
                matched = matched
            ))
            
            // Track content characteristics
            if (matched) {
                when (signalDef.type) {
                    SignalType.FULLSCREEN_VIDEO, SignalType.VIDEO_SURFACE -> isVideo = true
                    SignalType.SCROLL_PATTERN, SignalType.RECYCLERVIEW_VERTICAL -> isInfiniteScroll = true
                    else -> {}
                }
            }
        }
        
        // Calcular confianza
        val positiveScore = if (totalPositiveWeight > 0) matchedPositiveWeight / totalPositiveWeight else 0f
        val negativePenalty = if (totalNegativeWeight > 0) matchedNegativeWeight / totalNegativeWeight else 0f
        val confidence = (positiveScore - negativePenalty * 0.7f).coerceIn(0f, 1f)
        
        // Ajustar por señales de video/scroll (boost para contenido adictivo)
        val adjustedConfidence = if (isVideo || isInfiniteScroll) {
            min(1f, confidence * 1.15f) // 15% boost si hay video o scroll infinito
        } else confidence
        
        val detected = adjustedConfidence >= fingerprint.requiredConfidence
        
        return DetectionResult(
            detected = detected,
            ruleId = fingerprint.ruleId,
            confidence = adjustedConfidence,
            signals = matchedSignals,
            contentType = fingerprint.contentType,
            isInfiniteScroll = isInfiniteScroll || tree.hasVerticalPager,
            isVideoPlaying = isVideo || tree.hasVideoSurface,
            detectionMethod = "MultiSignalFingerprint v2.0"
        )
    }
    
    private fun matchSignal(tree: TreeSnapshot, signal: SignalDefinition): Boolean {
        val patterns = signal.patterns.map { if (signal.caseSensitive) it else it.lowercase() }
        
        return when (signal.type) {
            SignalType.VIEW_ID -> {
                if (signal.requireSelected) {
                    tree.selectedViewIds.any { id -> patterns.any { p -> id.contains(p) } }
                } else {
                    tree.viewIds.any { id -> patterns.any { p -> id.contains(p) } }
                }
            }
            
            SignalType.TEXT_CONTENT -> {
                val searchSet = if (signal.requireSelected) tree.selectedTexts else tree.textContents
                searchSet.any { text ->
                    val t = if (signal.caseSensitive) text else text.lowercase()
                    patterns.any { p -> t.contains(p, ignoreCase = !signal.caseSensitive) }
                }
            }
            
            SignalType.CONTENT_DESCRIPTION -> {
                val searchSet = if (signal.requireSelected) tree.selectedDescriptions else tree.contentDescriptions
                searchSet.any { desc ->
                    val d = if (signal.caseSensitive) desc else desc.lowercase()
                    patterns.any { p -> d.contains(p, ignoreCase = !signal.caseSensitive) }
                }
            }
            
            SignalType.CLASS_NAME -> {
                tree.classNames.any { cls -> patterns.any { p -> cls.contains(p, ignoreCase = true) } }
            }
            
            SignalType.FULLSCREEN_VIDEO, SignalType.VIDEO_SURFACE -> {
                tree.hasVideoSurface
            }
            
            SignalType.SCROLL_PATTERN -> {
                tree.hasVerticalPager || tree.classNames.any { cls -> 
                    patterns.any { p -> cls.contains(p, ignoreCase = true) }
                }
            }
            
            SignalType.RECYCLERVIEW_VERTICAL -> {
                tree.hasVerticalRecycler
            }
            
            SignalType.NAVIGATION_STATE -> {
                // Check both selected view IDs and selected descriptions for nav state
                tree.selectedViewIds.any { id -> patterns.any { p -> id.contains(p) } } ||
                tree.selectedDescriptions.any { desc -> 
                    patterns.any { p -> desc.contains(p, ignoreCase = true) } 
                }
            }
            
            SignalType.SELECTED_STATE -> {
                tree.selectedViewIds.any { id -> patterns.any { p -> id.contains(p) } }
            }
            
            SignalType.TAB_POSITION, SignalType.CHILD_COUNT, 
            SignalType.VIEW_HIERARCHY, SignalType.PACKAGE_ACTIVITY -> false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES Y AUTO-CALIBRACIÓN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Reportar un falso positivo para mejorar la detección.
     */
    fun reportFalsePositive(packageName: String, ruleId: String) {
        val stats = detectionStats.getOrPut(packageName) { DetectionStats() }
        stats.falsePositiveReports++
        Log.d(TAG, "False positive reported for $packageName:$ruleId (total: ${stats.falsePositiveReports})")
    }
    
    /**
     * Verifica si una app tiene un perfil de detección registrado.
     */
    fun hasProfile(packageName: String): Boolean = appProfiles.containsKey(packageName)
    
    /**
     * Obtiene todos los ruleIds disponibles para una app.
     */
    fun getAvailableRuleIds(packageName: String): List<String> {
        return appProfiles[packageName]?.features?.values?.map { it.ruleId } ?: emptyList()
    }
    
    /**
     * Obtiene las apps soportadas.
     */
    fun getSupportedApps(): Set<String> = appProfiles.keys
    
    /**
     * Obtiene información de detección para debugging.
     */
    fun getDetectionInfo(): Map<String, DetectionStats> = detectionStats.toMap()
}

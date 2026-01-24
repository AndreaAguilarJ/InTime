package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "website_blocks",
    indices = [
        Index(value = ["isEnabled"]),
        Index(value = ["url"], unique = true)
    ]
)
data class WebsiteBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String, // URL o dominio a bloquear (ej: "pornhub.com", "facebook.com")
    val displayName: String, // Nombre para mostrar al usuario
    val category: WebsiteCategory = WebsiteCategory.CUSTOM,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class WebsiteCategory {
    ADULT_CONTENT,      // Contenido para adultos
    SOCIAL_MEDIA,       // Redes sociales
    ENTERTAINMENT,      // Entretenimiento
    GAMING,             // Juegos
    NEWS,               // Noticias
    SHOPPING,           // Compras
    CUSTOM              // Personalizado por el usuario
}

// Categorías predefinidas con sitios comunes
object PredefinedWebsiteBlocks {
    fun getAdultContentSites(): List<Pair<String, String>> = listOf(
        "pornhub.com" to "Pornhub",
        "xvideos.com" to "XVideos",
        "xnxx.com" to "XNXX",
        "xhamster.com" to "xHamster",
        "redtube.com" to "RedTube",
        "youporn.com" to "YouPorn",
        "porn.com" to "Porn.com",
        "tube8.com" to "Tube8",
        "spankbang.com" to "SpankBang",
        "eporner.com" to "ePorner",
        "tnaflix.com" to "TNAFlix",
        "empflix.com" to "EMPFlix",
        "sunporno.com" to "SunPorno",
        "pornone.com" to "PornOne",
        "4tube.com" to "4Tube",
        "gotporn.com" to "GotPorn",
        "hotmovs.com" to "HotMovs",
        "nuvid.com" to "Nuvid",
        "drtuber.com" to "DrTuber",
        "youjizz.com" to "YouJizz"
    )

    fun getSocialMediaSites(): List<Pair<String, String>> = listOf(
        "facebook.com" to "Facebook",
        "instagram.com" to "Instagram",
        "twitter.com" to "Twitter/X",
        "x.com" to "X",
        "tiktok.com" to "TikTok",
        "snapchat.com" to "Snapchat",
        "reddit.com" to "Reddit",
        "linkedin.com" to "LinkedIn",
        "pinterest.com" to "Pinterest",
        "tumblr.com" to "Tumblr"
    )

    fun getEntertainmentSites(): List<Pair<String, String>> = listOf(
        "youtube.com" to "YouTube",
        "netflix.com" to "Netflix",
        "twitch.tv" to "Twitch",
        "disneyplus.com" to "Disney+",
        "hulu.com" to "Hulu",
        "primevideo.com" to "Prime Video"
    )

    fun getGamingSites(): List<Pair<String, String>> = listOf(
        "twitch.tv" to "Twitch",
        "steam.com" to "Steam",
        "epicgames.com" to "Epic Games",
        "roblox.com" to "Roblox",
        "miniclip.com" to "Miniclip"
    )
}


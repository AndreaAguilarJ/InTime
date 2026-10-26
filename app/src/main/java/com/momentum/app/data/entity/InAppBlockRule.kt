package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una regla de bloqueo dentro de una aplicación específica
 * Por ejemplo: bloquear Reels en Instagram, Shorts en YouTube, etc.
 */
@Entity(tableName = "in_app_block_rules")
data class InAppBlockRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Identificador único de la regla (ej: "instagram_reels", "youtube_shorts")
    val ruleId: String,

    // Paquete de la app (ej: "com.instagram.android")
    val packageName: String,

    // Nombre de la app
    val appName: String,

    // Tipo de función bloqueada
    val blockType: BlockType,

    // Nombre descriptivo de la función (ej: "Reels", "Shorts", "Explorar")
    val featureName: String,

    // Si la regla está habilitada
    val isEnabled: Boolean = false,

    // Patrones de URL o actividad para detectar (JSON string)
    val detectionPatterns: String = "[]",

    // Fecha de creación
    val createdAt: Long = System.currentTimeMillis(),

    // Última actualización
    val updatedAt: Long = System.currentTimeMillis()
)

enum class BlockType {
    REELS,              // Instagram Reels, Facebook Reels
    SHORTS,             // YouTube Shorts
    EXPLORE,            // Sección Explorar de Instagram
    SEARCH,             // Búsqueda en YouTube
    FOR_YOU,            // TikTok For You Page
    DISCOVER,           // Snapchat Discover
    STORIES,            // Stories de Instagram/Facebook
    FEED,               // Feed principal
    CUSTOM              // Regla personalizada
}


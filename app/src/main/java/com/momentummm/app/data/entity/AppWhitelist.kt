package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para aplicaciones en la lista blanca (whitelist)
 * Estas aplicaciones nunca serán bloqueadas, incluso si tienen límites configurados
 */
@Entity(tableName = "app_whitelist")
data class AppWhitelist(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val reason: String = "" // Razón por la cual está en whitelist (ej: "Emergencias", "Trabajo")
)


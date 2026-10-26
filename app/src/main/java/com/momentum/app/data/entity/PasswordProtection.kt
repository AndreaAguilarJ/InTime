package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_protection")
data class PasswordProtection(
    @PrimaryKey val id: Int = 1,
    val passwordHash: String? = null, // Hash SHA-256 de la contraseña
    val isEnabled: Boolean = false,
    val protectAppLimits: Boolean = true,
    val protectInAppBlocking: Boolean = true,
    val protectWebsiteBlocking: Boolean = true,
    val protectMinimalMode: Boolean = true,
    val failedAttempts: Int = 0,
    val lastFailedAttempt: Long = 0,
    val lockoutUntil: Long = 0 // Timestamp de cuando termina el bloqueo temporal
)


package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Configuración de bloqueo inteligente con múltiples características:
 * - Ventana de sueño (Sleep Hours)
 * - Ayuno Intermitente Digital (límites por horario)
 * - Bloqueo por ubicación/contexto
 * - Modo Nuclear
 * - Días de gracia para rachas
 */
@Entity(tableName = "smart_blocking_config")
data class SmartBlockingConfig(
    @PrimaryKey val id: Int = 1,
    
    // === VENTANA DE SUEÑO ===
    val sleepModeEnabled: Boolean = false,
    val sleepStartHour: Int = 23,       // 11 PM
    val sleepStartMinute: Int = 0,
    val sleepEndHour: Int = 7,          // 7 AM
    val sleepEndMinute: Int = 0,
    val sleepModeIgnoreTracking: Boolean = true,  // No contar tiempo durante sueño
    
    // === AYUNO INTERMITENTE DIGITAL ===
    val digitalFastingEnabled: Boolean = false,
    val fastingStartHour: Int = 9,      // 9 AM - inicio horario laboral
    val fastingStartMinute: Int = 0,
    val fastingEndHour: Int = 18,       // 6 PM - fin horario laboral
    val fastingEndMinute: Int = 0,
    val fastingDailyLimitMinutes: Int = 30, // Límite durante ayuno
    val fastingApplyToAllApps: Boolean = true, // Aplicar a todas las apps con límite
    val fastingDaysOfWeek: String = "1,2,3,4,5", // Lunes a Viernes (1-7)
    
    // === MODO NUCLEAR ===
    val nuclearModeEnabled: Boolean = false,
    val nuclearModeStartDate: Date? = null,
    val nuclearModeEndDate: Date? = null,
    val nuclearModeDurationDays: Int = 30, // 1-90 días
    val nuclearModeApps: String = "",  // Package names separados por coma
    val nuclearModeRequiresAppOpen: Boolean = true, // Timer solo corre con app abierta
    val nuclearModeUnlockWaitMinutes: Int = 30, // Minutos que debe esperar con app abierta
    val nuclearModeCurrentWaitSeconds: Int = 0, // Segundos acumulados esperando
    
    // === BLOQUEO POR CONTEXTO ===
    val contextBlockingEnabled: Boolean = false,
    
    // === PROTECCIÓN DE RACHAS ===
    val streakProtectionEnabled: Boolean = true,
    val graceDaysPerWeek: Int = 1,      // Fallos permitidos por semana
    val graceDaysUsedThisWeek: Int = 0,
    val lastGraceDayResetDate: Date? = null,
    val warningBeforeStreakBreak: Boolean = true,
    val warningMinutesBeforeLimit: Int = 5, // Avisar 5 min antes de límite
    
    // === TIMER FLOTANTE ===
    val floatingTimerEnabled: Boolean = false,
    val floatingTimerOpacity: Float = 0.8f, // 0.0 - 1.0
    val floatingTimerPosition: String = "TOP_RIGHT", // TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    val floatingTimerSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val floatingTimerShowForApps: String = "", // Package names, vacío = todas las apps con límite
    
    // === MODO SOLO COMUNICACIÓN ===
    val communicationOnlyModeEnabled: Boolean = false,
    val communicationOnlyApps: String = "", // Apps donde aplicar (ej: com.instagram.android)
    val communicationOnlyAllowDMs: Boolean = true,
    val communicationOnlyBlockFeed: Boolean = true,
    val communicationOnlyBlockStories: Boolean = true,
    val communicationOnlyBlockReels: Boolean = true,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT = SmartBlockingConfig()
    }
    
    /**
     * Verifica si estamos en horario de sueño
     */
    fun isInSleepHours(): Boolean {
        if (!sleepModeEnabled) return false
        
        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute
        
        val sleepStart = sleepStartHour * 60 + sleepStartMinute
        val sleepEnd = sleepEndHour * 60 + sleepEndMinute
        
        return if (sleepStart > sleepEnd) {
            // Sueño cruza medianoche (ej: 23:00 - 07:00)
            currentTime >= sleepStart || currentTime < sleepEnd
        } else {
            // Sueño en el mismo día
            currentTime in sleepStart until sleepEnd
        }
    }
    
    /**
     * Verifica si estamos en horario de ayuno digital
     */
    fun isInFastingHours(): Boolean {
        if (!digitalFastingEnabled) return false
        
        val now = java.util.Calendar.getInstance()
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) // 1=Domingo, 7=Sábado
        
        // Convertir a nuestro formato (1=Lunes, 7=Domingo)
        val adjustedDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1
        
        // Verificar si hoy es día de ayuno
        val fastingDays = fastingDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (adjustedDay !in fastingDays) return false
        
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute
        
        val fastingStart = fastingStartHour * 60 + fastingStartMinute
        val fastingEnd = fastingEndHour * 60 + fastingEndMinute
        
        return currentTime in fastingStart until fastingEnd
    }
    
    /**
     * Verifica si el modo nuclear está activo
     */
    fun isNuclearModeActive(): Boolean {
        if (!nuclearModeEnabled) return false
        val now = Date()
        val startDate = nuclearModeStartDate ?: return false
        val endDate = nuclearModeEndDate ?: return false
        return now.after(startDate) && now.before(endDate)
    }
    
    /**
     * Obtiene el límite efectivo de minutos considerando el ayuno digital
     */
    fun getEffectiveDailyLimit(originalLimitMinutes: Int): Int {
        return if (isInFastingHours()) {
            minOf(originalLimitMinutes, fastingDailyLimitMinutes)
        } else {
            originalLimitMinutes
        }
    }
    
    /**
     * Verifica si hay días de gracia disponibles esta semana
     */
    fun hasGraceDaysAvailable(): Boolean {
        return graceDaysUsedThisWeek < graceDaysPerWeek
    }
    
    /**
     * Lista de apps en modo nuclear
     */
    fun getNuclearModeAppsList(): List<String> {
        return if (nuclearModeApps.isBlank()) emptyList()
        else nuclearModeApps.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    /**
     * Lista de apps en modo solo comunicación
     */
    fun getCommunicationOnlyAppsList(): List<String> {
        return if (communicationOnlyApps.isBlank()) emptyList()
        else communicationOnlyApps.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
}

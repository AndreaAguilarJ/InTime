package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Configuración de bloqueo por contexto (ubicación/horario/día)
 */
@Entity(tableName = "context_block_rules")
data class ContextBlockRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    val ruleName: String,                    // "Trabajo", "Casa", "Escuela"
    val isEnabled: Boolean = true,
    
    // === TIPO DE CONTEXTO ===
    val contextType: String = "SCHEDULE",    // LOCATION, SCHEDULE, WIFI
    
    // === UBICACIÓN (GPS) ===
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 100,            // Radio de geofence
    val locationName: String? = null,        // "Mi Oficina"
    
    // === WIFI ===
    val wifiSsid: String? = null,            // Nombre de red WiFi
    
    // === HORARIO ===
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 18,
    val scheduleEndMinute: Int = 0,
    val scheduleDaysOfWeek: String = "1,2,3,4,5", // 1=Lunes, 7=Domingo
    
    // === APPS AFECTADAS ===
    val affectedApps: String = "",           // Package names separados por coma
    val applyToAllLimitedApps: Boolean = true, // Aplicar a todas las apps con límite
    
    // === LÍMITES EN ESTE CONTEXTO ===
    val overrideDailyLimit: Boolean = true,
    val contextDailyLimitMinutes: Int = 15,  // Límite cuando está en este contexto
    val blockCompletely: Boolean = false,    // Bloquear completamente en este contexto
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Verifica si estamos dentro del horario de esta regla
     */
    fun isActiveBySchedule(): Boolean {
        if (contextType != "SCHEDULE") return false
        
        val now = java.util.Calendar.getInstance()
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) // 1=Domingo, 7=Sábado
        
        // Convertir a nuestro formato (1=Lunes, 7=Domingo)
        val adjustedDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1
        
        // Verificar si hoy es día activo
        val activeDays = scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (adjustedDay !in activeDays) return false
        
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute
        
        val scheduleStart = scheduleStartHour * 60 + scheduleStartMinute
        val scheduleEnd = scheduleEndHour * 60 + scheduleEndMinute
        
        return currentTime in scheduleStart until scheduleEnd
    }
    
    /**
     * Lista de apps afectadas
     */
    fun getAffectedAppsList(): List<String> {
        return if (affectedApps.isBlank()) emptyList()
        else affectedApps.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    /**
     * Días de la semana como lista legible
     */
    fun getDaysAsText(): String {
        val dayNames = mapOf(
            1 to "Lun", 2 to "Mar", 3 to "Mié", 
            4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom"
        )
        val days = scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        return days.mapNotNull { dayNames[it] }.joinToString(", ")
    }
}

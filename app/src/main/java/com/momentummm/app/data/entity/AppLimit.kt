package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad para límites de aplicaciones.
 * 
 * Soporta dos tipos de límites:
 * 1. Límite por tiempo diario (dailyLimitMinutes) - El límite clásico
 * 2. Límite por horario (scheduleStartHour - scheduleEndHour) - Bloqueo por franja horaria
 * 
 * Feature solicitada: "block certain apps... at a certain time" 
 * (ej: bloquear Instagram solo de 9 AM a 5 PM)
 */
@Entity(
    tableName = "app_limits",
    indices = [Index(value = ["isEnabled"]), Index(value = ["categoryId"])]
)
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
    val iconUri: String? = null,
    val category: String = "Social", // Social, Games, Entertainment, etc.
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // ============================================================
    // BLOQUEO POR HORARIO - Feature solicitada por usuarios
    // ============================================================
    // "block certain apps... at a certain time" 
    // Permite bloquear apps solo durante ciertos horarios del día
    
    /**
     * Si el límite por horario está habilitado
     * Cuando es true, la app se bloquea durante el horario especificado,
     * independientemente del límite de minutos.
     */
    val hasScheduleLimit: Boolean = false,
    
    /**
     * Hora de inicio del bloqueo (formato 24h, 0-23)
     * Ejemplo: 9 = 9:00 AM
     */
    val scheduleStartHour: Int = 9,
    
    /**
     * Minuto de inicio del bloqueo (0-59)
     */
    val scheduleStartMinute: Int = 0,
    
    /**
     * Hora de fin del bloqueo (formato 24h, 0-23)
     * Ejemplo: 17 = 5:00 PM
     */
    val scheduleEndHour: Int = 17,
    
    /**
     * Minuto de fin del bloqueo (0-59)
     */
    val scheduleEndMinute: Int = 0,
    
    /**
     * Días de la semana cuando aplica el bloqueo por horario
     * Formato: "1,2,3,4,5" donde 1=Lunes, 7=Domingo
     * Por defecto: días de semana (Lun-Vie)
     */
    val scheduleDaysOfWeek: String = "1,2,3,4,5",
    
    // ============================================================
    // AGRUPACIÓN POR CATEGORÍA
    // ============================================================
    // Feature solicitada: "block apps via category" 
    // (bloquear todo "Entretenimiento")
    
    /**
     * ID de la categoría a la que pertenece esta app (si aplica)
     * null = sin categoría asignada
     */
    val categoryId: Int? = null,
    
    // ============================================================
    // PROTECCIÓN CONTRA EDICIÓN
    // ============================================================
    // Feature solicitada: Los usuarios se quejan de poder cambiar 
    // los límites cuando ya se han pasado.
    
    /**
     * Timestamp del último momento en que se excedió el límite.
     * Se usa para bloquear la edición del límite durante el resto del día.
     */
    val lastExceededAt: Long? = null
) {
    /**
     * Verifica si estamos dentro del horario de bloqueo.
     * Retorna true si la app debería estar bloqueada por horario.
     */
    fun isWithinScheduleBlock(): Boolean {
        if (!hasScheduleLimit) return false
        
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
     * Verifica si la edición del límite debería estar bloqueada.
     * Feature solicitada: Los usuarios se quejan de poder cambiar 
     * los límites cuando ya se han pasado.
     * 
     * Retorna true si el límite fue excedido hoy y no debería poder editarse.
     */
    fun isEditBlocked(): Boolean {
        val exceeded = lastExceededAt ?: return false
        
        // Verificar si la excedencia fue hoy
        val now = java.util.Calendar.getInstance()
        val exceededCal = java.util.Calendar.getInstance().apply {
            timeInMillis = exceeded
        }
        
        return now.get(java.util.Calendar.YEAR) == exceededCal.get(java.util.Calendar.YEAR) &&
               now.get(java.util.Calendar.DAY_OF_YEAR) == exceededCal.get(java.util.Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Obtiene los días de bloqueo como texto legible
     */
    fun getScheduleDaysAsText(): String {
        val dayNames = mapOf(
            1 to "Lun", 2 to "Mar", 3 to "Mié",
            4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom"
        )
        val days = scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        return days.mapNotNull { dayNames[it] }.joinToString(", ")
    }
    
    /**
     * Obtiene el horario de bloqueo formateado
     */
    fun getScheduleFormatted(): String {
        return String.format(
            "%02d:%02d - %02d:%02d",
            scheduleStartHour, scheduleStartMinute,
            scheduleEndHour, scheduleEndMinute
        )
    }
    
    /**
     * Crea una copia con el timestamp de excedencia actualizado
     */
    fun withExceeded(): AppLimit {
        return copy(
            lastExceededAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Crea una copia con el horario de bloqueo configurado
     */
    fun withScheduleLimit(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String
    ): AppLimit {
        return copy(
            hasScheduleLimit = enabled,
            scheduleStartHour = startHour,
            scheduleStartMinute = startMinute,
            scheduleEndHour = endHour,
            scheduleEndMinute = endMinute,
            scheduleDaysOfWeek = daysOfWeek,
            updatedAt = System.currentTimeMillis()
        )
    }
}

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

    /**
     * Bloquear las apps no esenciales durante la ventana de sueño.
     *
     * Antes este comportamiento existía pero NO era elegible: el monitor
     * bloqueaba toda app fuera de la whitelist siempre que la ventana estuviera
     * activa y `sleepModeIgnoreTracking` estuviese en `false`, acoplando dos
     * decisiones sin relación. Una función titulada "no contar el uso" dejaba
     * el teléfono inutilizable por la noche sin haberlo anunciado.
     *
     * Ahora es una opción explícita y desactivada por defecto: quien solo
     * quiera excluir el uso nocturno del recuento no pierde el acceso a sus
     * apps.
     */
    val sleepModeBlockApps: Boolean = false,
    
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

    /**
     * El usuario ha pedido desactivar el modo nuclear y está cumpliendo la
     * espera.
     *
     * Sin este campo el interruptor de la pantalla desactivaba el modo al
     * instante, aunque el texto prometiera «no podrás desactivarlo hasta que
     * termine»: la función entera era decorativa. Ahora apagarlo abre una
     * solicitud, y solo al completar la espera con la app abierta se permite
     * desactivar de verdad.
     */
    val nuclearModeUnlockRequested: Boolean = false,
    
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

        // Inicio y fin iguales describen una ventana vacía, no un día completo.
        // Se declara explícitamente para que coincida con la exclusión de uso
        // de DailyUsageCalculator, que aplica el mismo criterio.
        if (sleepStart == sleepEnd) return false

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
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute

        val fastingStart = fastingStartHour * 60 + fastingStartMinute
        val fastingEnd = fastingEndHour * 60 + fastingEndMinute

        // Inicio y fin iguales describen una franja vacía.
        if (fastingStart == fastingEnd) return false

        return if (fastingStart > fastingEnd) {
            // BUG CORREGIDO: la franja que cruza medianoche (p. ej. 23:00–07:00)
            // se evaluaba con `currentTime in start until end`, un rango vacío,
            // así que el ayuno nocturno NUNCA se activaba. La Ventana de sueño
            // sí lo hacía bien; aquí faltaba la misma rama.
            //
            // El día que cuenta es el del INICIO de la franja: a las 02:00 la
            // franja vigente empezó ayer, así que se comprueba el día de ayer.
            val dayOfStart = if (currentTime < fastingEnd) yesterdayIndex(now) else todayIndex(now)
            isFastingDay(dayOfStart) && (currentTime >= fastingStart || currentTime < fastingEnd)
        } else {
            isFastingDay(todayIndex(now)) && currentTime in fastingStart until fastingEnd
        }
    }

    /**
     * Instante en que empezó la franja de ayuno vigente, o `null` si no hay
     * ninguna activa ahora mismo.
     *
     * El límite de ayuno debe medirse contra lo consumido DENTRO de la franja.
     * Antes se comparaba contra el uso de todo el día: si el usuario ya había
     * gastado sus 30 minutos antes de que empezara el ayuno, la app se
     * bloqueaba en el primer segundo de la franja.
     */
    fun fastingWindowStartMillis(): Long? {
        if (!isInFastingHours()) return null

        val now = java.util.Calendar.getInstance()
        val currentTime = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            now.get(java.util.Calendar.MINUTE)
        val fastingStart = fastingStartHour * 60 + fastingStartMinute
        val fastingEnd = fastingEndHour * 60 + fastingEndMinute

        val start = (now.clone() as java.util.Calendar).apply {
            // Si la franja cruza medianoche y aún no hemos llegado a su hora de
            // inicio, la franja vigente arrancó ayer.
            if (fastingStart > fastingEnd && currentTime < fastingEnd) {
                add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            set(java.util.Calendar.HOUR_OF_DAY, fastingStartHour)
            set(java.util.Calendar.MINUTE, fastingStartMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return start.timeInMillis
    }

    /** Índice de día propio de la app: 1 = lunes … 7 = domingo. */
    private fun todayIndex(now: java.util.Calendar): Int {
        val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) // 1=Domingo, 7=Sábado
        return if (dayOfWeek == 1) 7 else dayOfWeek - 1
    }

    private fun yesterdayIndex(now: java.util.Calendar): Int {
        val yesterday = (now.clone() as java.util.Calendar)
            .apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        return todayIndex(yesterday)
    }

    private fun isFastingDay(dayIndex: Int): Boolean =
        dayIndex in fastingDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    
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

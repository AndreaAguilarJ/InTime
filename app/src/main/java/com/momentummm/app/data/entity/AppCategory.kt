package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad para agrupar apps por categorías.
 * 
 * Los usuarios piden: "block apps via category" (bloquear todo "Entretenimiento")
 * 
 * Esta entidad permite:
 * - Crear categorías personalizadas (Trabajo, Entretenimiento, Social, etc.)
 * - Asignar apps a categorías
 * - Aplicar límites a categorías completas en lugar de app por app
 */
@Entity(
    tableName = "app_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class AppCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    /**
     * Nombre de la categoría (ej: "Entretenimiento", "Social", "Trabajo")
     */
    val name: String,
    
    /**
     * Icono de la categoría (nombre del ícono Material)
     */
    val iconName: String = "Category",
    
    /**
     * Color de la categoría en formato hex (ej: "#FF5722")
     */
    val colorHex: String = "#6200EE",
    
    /**
     * Descripción opcional de la categoría
     */
    val description: String = "",
    
    /**
     * Si la categoría tiene un límite de tiempo activado
     */
    val isLimitEnabled: Boolean = false,
    
    /**
     * Límite diario en minutos para toda la categoría
     * Este límite aplica al tiempo COMBINADO de todas las apps en la categoría
     */
    val dailyLimitMinutes: Int = 60,
    
    /**
     * Si se debe bloquear por franja horaria
     */
    val hasScheduleLimit: Boolean = false,
    
    /**
     * Hora de inicio del bloqueo (formato 24h, ej: 9 para 9:00 AM)
     */
    val scheduleStartHour: Int = 9,
    
    /**
     * Minuto de inicio del bloqueo
     */
    val scheduleStartMinute: Int = 0,
    
    /**
     * Hora de fin del bloqueo (formato 24h, ej: 17 para 5:00 PM)
     */
    val scheduleEndHour: Int = 17,
    
    /**
     * Minuto de fin del bloqueo
     */
    val scheduleEndMinute: Int = 0,
    
    /**
     * Días de la semana cuando aplica el horario (formato: "1,2,3,4,5" donde 1=Lun, 7=Dom)
     */
    val scheduleDaysOfWeek: String = "1,2,3,4,5",
    
    /**
     * Lista de package names separados por coma
     */
    val packageNames: String = "",
    
    /**
     * Si es una categoría predefinida del sistema
     */
    val isSystemCategory: Boolean = false,
    
    /**
     * Orden de visualización
     */
    val displayOrder: Int = 0,
    
    /**
     * Timestamps
     */
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Obtiene la lista de package names como List
     */
    fun getPackageNamesList(): List<String> {
        return if (packageNames.isBlank()) emptyList()
        else packageNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    
    /**
     * Verifica si una app pertenece a esta categoría
     */
    fun containsApp(packageName: String): Boolean {
        return getPackageNamesList().contains(packageName)
    }
    
    /**
     * Agrega un package a la categoría
     */
    fun withApp(packageName: String): AppCategory {
        val currentApps = getPackageNamesList().toMutableList()
        if (!currentApps.contains(packageName)) {
            currentApps.add(packageName)
        }
        return copy(
            packageNames = currentApps.joinToString(","),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Remueve un package de la categoría
     */
    fun withoutApp(packageName: String): AppCategory {
        val currentApps = getPackageNamesList().toMutableList()
        currentApps.remove(packageName)
        return copy(
            packageNames = currentApps.joinToString(","),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Verifica si estamos dentro del horario de bloqueo
     */
    fun isWithinSchedule(): Boolean {
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
     * Obtiene los días como texto legible
     */
    fun getDaysAsText(): String {
        val dayNames = mapOf(
            1 to "Lun", 2 to "Mar", 3 to "Mié",
            4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom"
        )
        val days = scheduleDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        return days.mapNotNull { dayNames[it] }.joinToString(", ")
    }
    
    /**
     * Obtiene el horario formateado
     */
    fun getScheduleFormatted(): String {
        return String.format(
            "%02d:%02d - %02d:%02d",
            scheduleStartHour, scheduleStartMinute,
            scheduleEndHour, scheduleEndMinute
        )
    }
    
    companion object {
        /**
         * Categorías predefinidas del sistema
         */
        val SYSTEM_CATEGORIES = listOf(
            AppCategory(
                name = "Social",
                iconName = "People",
                colorHex = "#E91E63",
                description = "Redes sociales y mensajería",
                isSystemCategory = true,
                displayOrder = 1
            ),
            AppCategory(
                name = "Entretenimiento",
                iconName = "Movie",
                colorHex = "#9C27B0",
                description = "Videos, streaming y música",
                isSystemCategory = true,
                displayOrder = 2
            ),
            AppCategory(
                name = "Juegos",
                iconName = "SportsEsports",
                colorHex = "#673AB7",
                description = "Juegos y entretenimiento interactivo",
                isSystemCategory = true,
                displayOrder = 3
            ),
            AppCategory(
                name = "Productividad",
                iconName = "Work",
                colorHex = "#4CAF50",
                description = "Trabajo y herramientas",
                isSystemCategory = true,
                displayOrder = 4
            ),
            AppCategory(
                name = "Noticias",
                iconName = "Newspaper",
                colorHex = "#2196F3",
                description = "Noticias y actualidad",
                isSystemCategory = true,
                displayOrder = 5
            ),
            AppCategory(
                name = "Compras",
                iconName = "ShoppingCart",
                colorHex = "#FF9800",
                description = "E-commerce y compras",
                isSystemCategory = true,
                displayOrder = 6
            )
        )
        
        /**
         * Mapeo de packages conocidos a categorías
         */
        val KNOWN_APP_CATEGORIES = mapOf(
            // Social
            "com.instagram.android" to "Social",
            "com.facebook.katana" to "Social",
            "com.facebook.orca" to "Social",
            "com.twitter.android" to "Social",
            "com.x.android" to "Social",
            "com.whatsapp" to "Social",
            "org.telegram.messenger" to "Social",
            "com.snapchat.android" to "Social",
            "com.linkedin.android" to "Social",
            "com.pinterest" to "Social",
            "com.tumblr" to "Social",
            "com.reddit.frontpage" to "Social",
            "com.discord" to "Social",
            
            // Entretenimiento
            "com.google.android.youtube" to "Entretenimiento",
            "com.netflix.mediaclient" to "Entretenimiento",
            "com.spotify.music" to "Entretenimiento",
            "com.amazon.avod.thirdpartyclient" to "Entretenimiento",
            "com.disney.disneyplus" to "Entretenimiento",
            "com.hbo.hbonow" to "Entretenimiento",
            "tv.twitch.android.app" to "Entretenimiento",
            "com.zhiliaoapp.musically" to "Entretenimiento", // TikTok
            
            // Juegos (detectados dinámicamente por categoría de Play Store)
            
            // Noticias
            "com.google.android.apps.magazines" to "Noticias",
            "flipboard.app" to "Noticias",
            "com.twitter.android.lite" to "Noticias",
            
            // Compras
            "com.amazon.mShop.android.shopping" to "Compras",
            "com.ebay.mobile" to "Compras",
            "com.alibaba.aliexpresshd" to "Compras",
            "com.mercadolibre" to "Compras"
        )
    }
}

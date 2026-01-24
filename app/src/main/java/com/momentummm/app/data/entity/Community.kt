package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Representa una amistad entre usuarios para la comunidad.
 */
@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["friendUserId"]),
        Index(value = ["status"])
    ]
)
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    // ID del usuario amigo (del sistema Appwrite)
    val friendUserId: String,
    
    // Nombre del amigo
    val friendName: String,
    
    // Email del amigo (para buscar)
    val friendEmail: String? = null,
    
    // Avatar URL
    val avatarUrl: String? = null,
    
    // Estado de la solicitud
    val status: FriendStatus = FriendStatus.PENDING,
    
    // Quién envió la solicitud (true = yo la envié, false = la recibí)
    val sentByMe: Boolean = true,
    
    // Estadísticas del amigo (se actualizan periódicamente)
    val friendLevel: Int = 1,
    val friendStreak: Int = 0,
    val friendTotalFocusMinutes: Int = 0,
    val friendWeeklyFocusMinutes: Int = 0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = 0
)

enum class FriendStatus {
    PENDING,        // Solicitud enviada/recibida
    ACCEPTED,       // Amistad confirmada
    BLOCKED,        // Usuario bloqueado
    REJECTED        // Solicitud rechazada
}

/**
 * Entrada del leaderboard semanal.
 */
@Entity(
    tableName = "leaderboard_entries",
    indices = [Index(value = ["weekStartDate"]), Index(value = ["rank"])]
)
data class LeaderboardEntry(
    @PrimaryKey val odId: String,  // ID del documento de Appwrite
    
    val userId: String,
    val userName: String,
    val avatarUrl: String? = null,
    
    // Métricas para ranking
    val weeklyFocusMinutes: Int = 0,
    val weeklyPerfectDays: Int = 0,
    val currentStreak: Int = 0,
    val userLevel: Int = 1,
    
    // Ranking calculado
    val rank: Int = 0,
    val previousRank: Int = 0,  // Para mostrar cambios ⬆️⬇️
    
    // Semana del leaderboard
    val weekStartDate: Date,
    
    // Si es amigo del usuario actual
    val isFriend: Boolean = false,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Logro compartible para la comunidad.
 */
@Entity(tableName = "shared_achievements")
data class SharedAchievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    
    // Tipo de logro
    val achievementType: AchievementType,
    
    // Valor del logro (ej: "7" para racha de 7 días)
    val achievementValue: Int = 0,
    
    // Mensaje personalizado
    val message: String = "",
    
    // Si ya fue compartido
    val isShared: Boolean = false,
    
    // Cuántas veces fue compartido (para analytics)
    val shareCount: Int = 0,
    
    // Fecha del logro
    val achievedAt: Long = System.currentTimeMillis(),
    
    // ID del documento en Appwrite (si está sincronizado)
    val remoteId: String? = null
)

enum class AchievementType {
    STREAK_MILESTONE,       // Racha de X días
    LEVEL_UP,               // Subida de nivel
    PERFECT_WEEK,           // Semana perfecta
    FOCUS_MILESTONE,        // X horas de foco total
    NUCLEAR_COMPLETED,      // Completó modo nuclear
    TOP_LEADERBOARD,        // Top 3 en leaderboard
    FIRST_WEEK,             // Primera semana usando la app
    CUSTOM                  // Logro personalizado
}

/**
 * Configuración de la comunidad del usuario.
 */
@Entity(tableName = "community_settings")
data class CommunitySettings(
    @PrimaryKey val id: Int = 1,
    
    // Privacidad
    val profileVisibility: ProfileVisibility = ProfileVisibility.FRIENDS_ONLY,
    val showInGlobalLeaderboard: Boolean = true,
    val showStreakToFriends: Boolean = true,
    val showLevelToFriends: Boolean = true,
    val showFocusTimeToFriends: Boolean = true,
    
    // Notificaciones sociales
    val notifyFriendRequests: Boolean = true,
    val notifyFriendAchievements: Boolean = true,
    val notifyLeaderboardChanges: Boolean = true,
    
    // Shame/Glory mode
    val shameEnabled: Boolean = true,
    val gloryEnabled: Boolean = true,
    val autoShareAchievements: Boolean = false,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ProfileVisibility {
    PUBLIC,         // Cualquiera puede ver
    FRIENDS_ONLY,   // Solo amigos
    PRIVATE         // Nadie puede ver
}

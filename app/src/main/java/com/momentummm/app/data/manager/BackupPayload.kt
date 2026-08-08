package com.momentummm.app.data.manager

import com.momentummm.app.data.entity.Goal
import com.momentummm.app.data.entity.Quote
import com.momentummm.app.data.entity.UserSettings
import com.momentummm.app.data.repository.AppUsageInfo
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Formato de las copias de seguridad.
 *
 * ─── POR QUÉ EXISTE ESTE ARCHIVO ──────────────────────────────────────────
 * La estructura anterior vivía dentro de [BackupSyncManager] y tenía dos
 * problemas de fondo:
 *
 * 1. **No era serializable.** `BackupData` y todo lo que contenía (entidades de
 *    Room con `Date`, enums, etc.) no llevaban `@Serializable`, y el código
 *    llamaba a `Json.encodeToString(backupData)`. kotlinx.serialization
 *    necesita un serializador generado; sin él la copia no se puede producir.
 *
 * 2. **Las metas y las sesiones eran clases de relleno**, con un comentario que
 *    lo decía literalmente: *"Dummy data classes for compilation (these would be
 *    defined elsewhere)"*. Su forma (`goalType`, `target`, `achieved`, `date`)
 *    no se parece a una meta real: no había ni título, ni periodo, ni
 *    categoría. Aunque se hubieran guardado, no se podrían restaurar.
 *
 * Aquí el formato es explícito, de tipos primitivos y desacoplado de Room: así
 * una copia hecha hoy se sigue pudiendo leer si mañana cambia el esquema de la
 * base de datos, que es justo lo que se le pide a un backup.
 */
@Serializable
data class BackupPayload(
    /** Sube cuando el formato cambie de forma incompatible. */
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val userSettings: UserSettingsBackup? = null,
    val quotes: List<QuoteBackup> = emptyList(),
    val goals: List<GoalBackup> = emptyList(),
    /**
     * Sólo como registro histórico: el uso de apps lo posee Android
     * (UsageStatsManager) y no se puede escribir de vuelta.
     */
    val usageStats: List<UsageStatBackup> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

@Serializable
data class UserSettingsBackup(
    val birthDateMillis: Long? = null,
    val isOnboardingCompleted: Boolean = false,
    val hasSeenTutorial: Boolean = false,
    val livedWeeksColor: String = "#6366F1",
    val futureWeeksColor: String = "#E5E7EB",
    val backgroundColor: String = "#FFFFFF",
    val userLevel: Int = 1,
    val currentXp: Int = 0,
    val totalXp: Int = 0,
    val timeCoins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDateMillis: Long? = null,
    val totalFocusMinutes: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val perfectDaysCount: Int = 0,
    val gamificationEnabled: Boolean = true,
    val showXpNotifications: Boolean = true,
    val showStreakReminders: Boolean = true
)

@Serializable
data class QuoteBackup(
    val id: Int = 0,
    val text: String,
    val author: String? = null
)

@Serializable
data class GoalBackup(
    val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val period: String,
    val category: String,
    val isActive: Boolean,
    val createdDateMillis: Long,
    val endDateMillis: Long? = null,
    val currentStreak: Int = 0,
    val completionCount: Int = 0
)

@Serializable
data class UsageStatBackup(
    val packageName: String,
    val appName: String,
    val totalTimeInMillis: Long,
    val lastTimeUsed: Long
)

// ─── Conversión entidad → copia ───────────────────────────────────────────

fun UserSettings.toBackup(): UserSettingsBackup = UserSettingsBackup(
    birthDateMillis = birthDate?.time,
    isOnboardingCompleted = isOnboardingCompleted,
    hasSeenTutorial = hasSeenTutorial,
    livedWeeksColor = livedWeeksColor,
    futureWeeksColor = futureWeeksColor,
    backgroundColor = backgroundColor,
    userLevel = userLevel,
    currentXp = currentXp,
    totalXp = totalXp,
    timeCoins = timeCoins,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastActiveDateMillis = lastActiveDate?.time,
    totalFocusMinutes = totalFocusMinutes,
    totalSessionsCompleted = totalSessionsCompleted,
    perfectDaysCount = perfectDaysCount,
    gamificationEnabled = gamificationEnabled,
    showXpNotifications = showXpNotifications,
    showStreakReminders = showStreakReminders
)

fun Quote.toBackup(): QuoteBackup = QuoteBackup(id = id, text = text, author = author)

fun Goal.toBackup(): GoalBackup = GoalBackup(
    id = id,
    title = title,
    description = description,
    targetValue = targetValue,
    currentValue = currentValue,
    period = period,
    category = category,
    isActive = isActive,
    createdDateMillis = createdDate.time,
    endDateMillis = endDate?.time,
    currentStreak = currentStreak,
    completionCount = completionCount
)

fun AppUsageInfo.toBackup(): UsageStatBackup = UsageStatBackup(
    packageName = packageName,
    appName = appName,
    totalTimeInMillis = totalTimeInMillis,
    lastTimeUsed = lastTimeUsed
)

// ─── Conversión copia → entidad ───────────────────────────────────────────

/**
 * Reconstruye los ajustes sobre los actuales.
 *
 * Se parte de [current] a propósito: si una copia antigua no trae un campo, se
 * conserva el valor local en lugar de sobrescribirlo con el valor por defecto.
 */
fun UserSettingsBackup.toEntity(current: UserSettings): UserSettings = current.copy(
    birthDate = birthDateMillis?.let { Date(it) } ?: current.birthDate,
    isOnboardingCompleted = isOnboardingCompleted,
    hasSeenTutorial = hasSeenTutorial,
    livedWeeksColor = livedWeeksColor,
    futureWeeksColor = futureWeeksColor,
    backgroundColor = backgroundColor,
    userLevel = userLevel,
    currentXp = currentXp,
    totalXp = totalXp,
    timeCoins = timeCoins,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastActiveDate = lastActiveDateMillis?.let { Date(it) } ?: current.lastActiveDate,
    totalFocusMinutes = totalFocusMinutes,
    totalSessionsCompleted = totalSessionsCompleted,
    perfectDaysCount = perfectDaysCount,
    gamificationEnabled = gamificationEnabled,
    showXpNotifications = showXpNotifications,
    showStreakReminders = showStreakReminders
)

fun QuoteBackup.toEntity(): Quote = Quote(id = id, text = text, author = author)

fun GoalBackup.toEntity(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    targetValue = targetValue,
    currentValue = currentValue,
    period = period,
    category = category,
    isActive = isActive,
    createdDate = Date(createdDateMillis),
    endDate = endDateMillis?.let { Date(it) },
    currentStreak = currentStreak,
    completionCount = completionCount
)

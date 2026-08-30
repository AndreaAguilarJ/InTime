package com.momentummm.app.data.dao

import androidx.room.*
import com.momentummm.app.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(userSettings: UserSettings)

    @Update
    suspend fun updateUserSettings(userSettings: UserSettings)

    // === GAMIFICATION QUERIES ===

    /**
     * Actualiza XP y verifica si subió de nivel
     */
    @Query("UPDATE user_settings SET currentXp = currentXp + :xpAmount, totalXp = totalXp + :xpAmount WHERE id = 1")
    suspend fun addXp(xpAmount: Int)

    /**
     * Actualiza TimeCoins
     */
    @Query("UPDATE user_settings SET timeCoins = timeCoins + :coinsAmount WHERE id = 1")
    suspend fun addTimeCoins(coinsAmount: Int)

    /**
     * Resta XP (para penalizaciones)
     */
    @Query("UPDATE user_settings SET currentXp = CASE WHEN currentXp - :xpAmount < 0 THEN 0 ELSE currentXp - :xpAmount END WHERE id = 1")
    suspend fun subtractXp(xpAmount: Int)

    /**
     * Actualiza nivel del usuario
     */
    @Query("UPDATE user_settings SET userLevel = :newLevel WHERE id = 1")
    suspend fun updateLevel(newLevel: Int)

    /**
     * Incrementa la racha actual, una sola vez por día.
     *
     * El `AND (lastActiveDate IS NULL OR lastActiveDate < :date)` no es
     * decorativo: `updateDailyStreak()` lee los ajustes y luego escribe, y se
     * llama desde dos sitios a la vez al abrir la app (GamificationManager y
     * DashboardViewModel). Sin la guarda, las dos lecturas veían
     * `lastActiveDate == null` antes de que ninguna escribiera y la racha subía
     * a 2 en una instalación recién hecha, sin un solo día de uso.
     *
     * Al comparar contra la medianoche del día en curso, un segundo intento del
     * mismo día no afecta a ninguna fila. Devuelve las filas modificadas para
     * que quien llama sepa si el incremento ocurrió de verdad.
     */
    @Query("UPDATE user_settings SET currentStreak = currentStreak + 1, longestStreak = CASE WHEN currentStreak + 1 > longestStreak THEN currentStreak + 1 ELSE longestStreak END, lastActiveDate = :date WHERE id = 1 AND (lastActiveDate IS NULL OR lastActiveDate < :date)")
    suspend fun incrementStreak(date: Date): Int

    /**
     * Resetea la racha (cuando se rompe)
     */
    @Query("UPDATE user_settings SET currentStreak = 0, lastActiveDate = :date WHERE id = 1")
    suspend fun resetStreak(date: Date)

    /**
     * Conserva la racha actual y adelanta la última fecha activa.
     *
     * Es lo que hace un día de gracia: el usuario faltó, así que la racha NO
     * sube, pero tampoco se pierde. Sin esta consulta no existía forma de
     * expresar «mantener» —sólo incrementar o resetear— y por eso la
     * protección de rachas no podía implementarse.
     */
    @Query("UPDATE user_settings SET lastActiveDate = :date WHERE id = 1")
    suspend fun preserveStreak(date: Date)

    /**
     * Incrementa minutos de foco totales
     */
    @Query("UPDATE user_settings SET totalFocusMinutes = totalFocusMinutes + :minutes WHERE id = 1")
    suspend fun addFocusMinutes(minutes: Int)

    /**
     * Incrementa contador de sesiones completadas
     */
    @Query("UPDATE user_settings SET totalSessionsCompleted = totalSessionsCompleted + 1 WHERE id = 1")
    suspend fun incrementSessionsCompleted()

    /**
     * Incrementa días perfectos
     */
    @Query("UPDATE user_settings SET perfectDaysCount = perfectDaysCount + 1 WHERE id = 1")
    suspend fun incrementPerfectDays()

    /**
     * Obtiene la racha actual
     */
    @Query("SELECT currentStreak FROM user_settings WHERE id = 1")
    suspend fun getCurrentStreak(): Int?

    /**
     * Obtiene el nivel actual
     */
    @Query("SELECT userLevel FROM user_settings WHERE id = 1")
    suspend fun getUserLevel(): Int?

    /**
     * Obtiene XP actual
     */
    @Query("SELECT currentXp FROM user_settings WHERE id = 1")
    suspend fun getCurrentXp(): Int?

    /**
     * Obtiene TimeCoins
     */
    @Query("SELECT timeCoins FROM user_settings WHERE id = 1")
    suspend fun getTimeCoins(): Int?

    /**
     * Gasta TimeCoins
     */
    @Query("UPDATE user_settings SET timeCoins = CASE WHEN timeCoins - :amount < 0 THEN 0 ELSE timeCoins - :amount END WHERE id = 1")
    suspend fun spendTimeCoins(amount: Int)
    
    // === GAMIFICATION SETTINGS QUERIES ===
    
    /**
     * Actualiza si la gamificación está habilitada
     */
    @Query("UPDATE user_settings SET gamificationEnabled = :enabled WHERE id = 1")
    suspend fun updateGamificationEnabled(enabled: Boolean)
    
    /**
     * Actualiza si se muestran notificaciones de XP
     */
    @Query("UPDATE user_settings SET showXpNotifications = :enabled WHERE id = 1")
    suspend fun updateShowXpNotifications(enabled: Boolean)
    
    /**
     * Actualiza si se muestran recordatorios de racha
     */
    @Query("UPDATE user_settings SET showStreakReminders = :enabled WHERE id = 1")
    suspend fun updateShowStreakReminders(enabled: Boolean)
    
    /**
     * Resetea todos los datos de gamificación
     */
    @Query("UPDATE user_settings SET userLevel = 1, currentXp = 0, totalXp = 0, timeCoins = 0, currentStreak = 0, longestStreak = 0, lastActiveDate = NULL, totalFocusMinutes = 0, totalSessionsCompleted = 0, perfectDaysCount = 0 WHERE id = 1")
    suspend fun resetGamificationProgress()
}
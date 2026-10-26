package com.momentummm.app.data.appwrite.repository

import com.momentummm.app.data.appwrite.AppwriteConfig
import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.appwrite.models.AppwriteFocusSession
import com.momentummm.app.data.appwrite.models.FocusSessionStats
import io.appwrite.Query
import io.appwrite.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class AppwriteFocusSessionRepository(
    private val appwriteService: AppwriteService
) {

    suspend fun saveFocusSession(session: AppwriteFocusSession): Result<AppwriteFocusSession> {
        return try {
            val sessionData = mapOf(
                "userId" to session.userId,
                "sessionId" to session.sessionId,
                "sessionType" to session.sessionType,
                "date" to session.date,
                "startTime" to session.startTime,
                "endTime" to session.endTime,
                "plannedDuration" to session.plannedDuration,
                "actualDuration" to session.actualDuration,
                "wasCompleted" to session.wasCompleted,
                "distractions" to session.distractions,
                "blockedApps" to session.blockedApps,
                "breakDuration" to session.breakDuration,
                "createdAt" to getCurrentTimestamp(),
                "updatedAt" to getCurrentTimestamp()
            )

            val document = appwriteService.databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.FOCUS_SESSIONS_COLLECTION_ID,
                documentId = session.sessionId,
                data = sessionData
            )

            val savedSession = documentToFocusSession(document)
            Result.success(savedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFocusSessionStats(userId: String): Flow<FocusSessionStats> = flow {
        try {
            val today = getCurrentDate()

            // Obtener sesiones de hoy
            val todaySessions = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.FOCUS_SESSIONS_COLLECTION_ID,
                queries = listOf(
                    Query.equal("userId", userId),
                    Query.equal("date", today)
                )
            )

            // Calcular estadísticas de hoy
            val sessionsToday = todaySessions.documents.map { documentToFocusSession(it) }
            val completedToday = sessionsToday.count { it.wasCompleted }
            val totalFocusTimeToday = sessionsToday.sumOf { it.actualDuration }

            // Calcular racha de días consecutivos
            val streakDays = calculateStreakDays(userId)

            emit(FocusSessionStats(
                completedToday = completedToday,
                totalFocusTimeToday = totalFocusTimeToday,
                streakDays = streakDays
            ))
        } catch (e: Exception) {
            emit(FocusSessionStats()) // Emitir estadísticas vacías en caso de error
        }
    }

    fun getFocusSessionHistory(userId: String, limit: Int = 10): Flow<List<AppwriteFocusSession>> = flow {
        try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.FOCUS_SESSIONS_COLLECTION_ID,
                queries = listOf(
                    Query.equal("userId", userId),
                    Query.orderDesc("createdAt"),
                    Query.limit(limit)
                )
            )

            val sessions = documents.documents.map { documentToFocusSession(it) }
            emit(sessions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    private suspend fun calculateStreakDays(userId: String): Int {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.FOCUS_SESSIONS_COLLECTION_ID,
                queries = listOf(
                    Query.equal("userId", userId),
                    Query.equal("wasCompleted", true),
                    Query.orderDesc("date")
                )
            )

            val sessions = documents.documents.map { documentToFocusSession(it) }
            val sessionsByDate = sessions.groupBy { it.date }

            var streak = 0
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Comenzar desde hoy y retroceder
            while (streak < 365) { // Máximo 365 días de racha
                val checkDate = dateFormat.format(calendar.time)

                if (sessionsByDate.containsKey(checkDate)) {
                    streak++
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            }

            streak
        } catch (e: Exception) {
            0
        }
    }

    private fun documentToFocusSession(document: Document<Map<String, Any>>): AppwriteFocusSession {
        return AppwriteFocusSession(
            userId = document.data["userId"] as String,
            sessionId = document.data["sessionId"] as String,
            sessionType = document.data["sessionType"] as String,
            date = document.data["date"] as String,
            startTime = document.data["startTime"] as? String,
            endTime = document.data["endTime"] as? String,
            plannedDuration = (document.data["plannedDuration"] as? Number)?.toInt() ?: 0,
            actualDuration = (document.data["actualDuration"] as? Number)?.toInt() ?: 0,
            wasCompleted = document.data["wasCompleted"] as? Boolean ?: false,
            distractions = (document.data["distractions"] as? Number)?.toInt() ?: 0,
            blockedApps = (document.data["blockedApps"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            breakDuration = (document.data["breakDuration"] as? Number)?.toInt(),
            createdAt = document.data["createdAt"] as? String ?: "",
            updatedAt = document.data["updatedAt"] as? String ?: ""
        )
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }
}

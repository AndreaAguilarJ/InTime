package com.momentummm.app.data.appwrite.models

import kotlinx.serialization.Serializable

@Serializable
data class AppwriteUserSettings(
    val userId: String,
    val birthDate: String, // ISO format date
    val isOnboardingCompleted: Boolean = false,
    val livedWeeksColor: String = "#FF6B6B",
    val futureWeeksColor: String = "#E8E8E8",
    val enableBiometric: Boolean = false,
    val minimalModeEnabled: Boolean = false,
    val allowedApps: List<String> = emptyList(),
    val dailyQuotesEnabled: Boolean = true,
    val onboardingStep: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class AppwriteQuote(
    val id: String,
    val text: String,
    val author: String? = null,
    val category: String = "motivation",
    val isActive: Boolean = true,
    val createdAt: String = ""
)

@Serializable
data class AppwriteAppUsage(
    val userId: String,
    val packageName: String,
    val appName: String,
    val timeInForeground: Long,
    val date: String, // ISO format date
    val createdAt: String = ""
)

@Serializable
data class AppwriteLifeWeeksData(
    val userId: String,
    val totalWeeks: Int = 4160,
    val weeksLived: Int,
    val weeksRemaining: Int,
    val currentAge: Int,
    val progressPercentage: Float,
    val updatedAt: String = ""
)

@Serializable
data class AppwriteFocusSession(
    val userId: String,
    val sessionId: String,
    val sessionType: String,
    val date: String, // formato YYYY-MM-DD
    val startTime: String? = null,
    val endTime: String? = null,
    val plannedDuration: Int, // en minutos
    val actualDuration: Int, // en minutos
    val wasCompleted: Boolean,
    val distractions: Int = 0,
    val blockedApps: List<String> = emptyList(),
    val breakDuration: Int? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class FocusSessionStats(
    val completedToday: Int = 0,
    val totalFocusTimeToday: Int = 0, // en minutos
    val streakDays: Int = 0
)

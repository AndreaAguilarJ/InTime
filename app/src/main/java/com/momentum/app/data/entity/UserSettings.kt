package com.momentum.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val birthDate: Date?,
    val isOnboardingCompleted: Boolean = false,
    val hasSeenTutorial: Boolean = false,
    val livedWeeksColor: String = "#6366F1",
    val futureWeeksColor: String = "#E5E7EB",
    val backgroundColor: String = "#FFFFFF"
)
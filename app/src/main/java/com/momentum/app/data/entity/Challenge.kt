package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "icon_type")
    val iconType: String, // PHONE_OFF, TIMER, NATURE, etc.

    @ColumnInfo(name = "difficulty")
    val difficulty: String, // EASY, MEDIUM, HARD

    @ColumnInfo(name = "duration_days")
    val durationDays: Int,

    @ColumnInfo(name = "progress")
    val progress: Float = 0.0f, // 0.0 to 1.0

    @ColumnInfo(name = "reward")
    val reward: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "start_date")
    val startDate: Date? = null,

    @ColumnInfo(name = "end_date")
    val endDate: Date? = null,

    @ColumnInfo(name = "created_date")
    val createdDate: Date = Date(),

    @ColumnInfo(name = "completion_date")
    val completionDate: Date? = null,

    @ColumnInfo(name = "days_completed")
    val daysCompleted: Int = 0,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0
)

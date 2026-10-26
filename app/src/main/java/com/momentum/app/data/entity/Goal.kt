package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "target_value")
    val targetValue: Int, // in minutes

    @ColumnInfo(name = "current_value")
    val currentValue: Int = 0, // in minutes

    @ColumnInfo(name = "period")
    val period: String, // DAILY, WEEKLY, MONTHLY

    @ColumnInfo(name = "category")
    val category: String, // SCREEN_TIME, SOCIAL_MEDIA, etc.

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_date")
    val createdDate: Date,

    @ColumnInfo(name = "end_date")
    val endDate: Date? = null,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),

    @ColumnInfo(name = "completion_count")
    val completionCount: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0
)

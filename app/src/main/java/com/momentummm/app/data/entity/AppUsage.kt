package com.momentummm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey val id: String, // packageName + date
    val packageName: String,
    val appName: String,
    val date: Date,
    val totalTimeInMillis: Long,
    val openCount: Int
)
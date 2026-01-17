package com.momentummm.app.data

import androidx.room.TypeConverter
import com.momentummm.app.data.entity.BlockType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromBlockType(value: BlockType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toBlockType(value: String?): BlockType? {
        return value?.let { BlockType.valueOf(it) }
    }
}
package com.momentummm.app.data

import androidx.room.TypeConverter
import com.momentummm.app.data.entity.AIPersonality
import com.momentummm.app.data.entity.BlockType
import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.ReactionType
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

    // ============== Motivational Messages Converters ==============

    @TypeConverter
    fun fromMessageCategory(value: MessageCategory?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMessageCategory(value: String?): MessageCategory? {
        return value?.let { 
            try { MessageCategory.valueOf(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromMessageTone(value: MessageTone?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMessageTone(value: String?): MessageTone? {
        return value?.let { 
            try { MessageTone.valueOf(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromReactionType(value: ReactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toReactionType(value: String?): ReactionType? {
        return value?.let { 
            try { ReactionType.valueOf(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromAIPersonality(value: AIPersonality?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAIPersonality(value: String?): AIPersonality? {
        return value?.let { 
            try { AIPersonality.valueOf(it) } catch (e: Exception) { AIPersonality.FRIENDLY_COACH }
        }
    }

    @TypeConverter
    fun fromMessageCategoryList(value: List<MessageCategory>?): String? {
        return value?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toMessageCategoryList(value: String?): List<MessageCategory>? {
        return value?.split(",")?.mapNotNull { 
            try { MessageCategory.valueOf(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromMessageToneList(value: List<MessageTone>?): String? {
        return value?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toMessageToneList(value: String?): List<MessageTone>? {
        return value?.split(",")?.mapNotNull { 
            try { MessageTone.valueOf(it) } catch (e: Exception) { null }
        }
    }
}
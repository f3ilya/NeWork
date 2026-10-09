package ru.netology.nework.entity

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.UserPreview

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromAttachment(value: Attachment?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAttachment(value: String?): Attachment? {
        return gson.fromJson(value, Attachment::class.java)
    }

    @TypeConverter
    fun fromMapUsers(value: Map<String, UserPreview>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMapUsers(value: String?): Map<String, UserPreview>? {
        if (value.isNullOrBlank()) return null

        val mapType = object : TypeToken<Map<String, UserPreview>>() {}.type
        return gson.fromJson(value, mapType)
    }
}
package com.xixi.notes.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 图片相对路径列表 <-> JSON 字符串 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
}

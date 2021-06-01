package com.byteseb.grafobook.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    val gson = Gson()
    val type = object: TypeToken<List<String>>(){}.type

    @TypeConverter
    fun toList(string: String): List<String> = gson.fromJson(string, type)

    @TypeConverter
    fun toString(list: List<String>): String = gson.toJson(list)
}
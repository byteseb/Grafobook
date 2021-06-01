package com.byteseb.grafobook.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.byteseb.grafobook.room.Converters

@Entity
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val name : String,
    var favorite: Boolean,
    val color: String?,
    val creationDate: Long,
    val lastDate: Long,
    @TypeConverters(Converters::class)
    var tags: List<String>,
    val reminder: Long,
    val content: String
)
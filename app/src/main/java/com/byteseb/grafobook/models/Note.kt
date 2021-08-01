package com.byteseb.grafobook.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.byteseb.grafobook.room.Converters
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true)
    var id : Int = 0,
    val name : String,
    var favorite: Boolean,
    val color: String?,
    val creationDate: Long,
    val lastDate: Long,
    @TypeConverters(Converters::class)
    var tags: List<String>,
    var reminder: Long,
    val content: String,
    val password: String?
) : Parcelable
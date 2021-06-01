package com.byteseb.grafobook.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.byteseb.grafobook.models.Note

@Database(entities = [Note::class], version = 1)
@TypeConverters(Converters::class)
abstract class NotesDB : RoomDatabase(){
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDB? = null

        fun getDB(context: Context): NotesDB{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(context.applicationContext,
                NotesDB::class.java,
                "notesDB").build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.byteseb.grafobook.room

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.byteseb.grafobook.models.Note

@Database(
    version = 2,
    entities = [Note::class],
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class NotesDB : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {

        //Migrations
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Note ADD COLUMN password TEXT")
            }
        }

        @Volatile
        private var INSTANCE: NotesDB? = null

        fun getDB(context: Context): NotesDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDB::class.java,
                    "notesDB"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.byteseb.grafobook.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.byteseb.grafobook.models.Note

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(notes: Note)

    @Query("SELECT * FROM note")
    fun getAll(): LiveData<List<Note>>

    @Query("SELECT tags from note")
    fun getTags(): LiveData<List<String>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNote(id: Int): Note
}
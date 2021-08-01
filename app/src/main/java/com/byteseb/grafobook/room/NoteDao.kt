package com.byteseb.grafobook.room

import androidx.lifecycle.LiveData
import androidx.room.*
import com.byteseb.grafobook.models.Note

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(notes: Note)

    @Query("SELECT * FROM note")
    fun getAll(): LiveData<List<Note>>

    @Query("SELECT * FROM note")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT id FROM note WHERE reminder != -1 AND reminder > :currentTime ORDER BY reminder ASC")
    suspend fun getIdsWithReminders(currentTime: Long): List<Int>

    @Query("SELECT * FROM note WHERE reminder != -1 AND reminder > :currentTime ORDER BY reminder ASC")
    suspend fun getNotesWithReminders(currentTime: Long): List<Note>

    @Query("SELECT tags from note")
    fun getTags(): LiveData<List<String>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNote(id: Int): Note
}
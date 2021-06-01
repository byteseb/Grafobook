package com.byteseb.grafobook.room

import androidx.lifecycle.LiveData
import com.byteseb.grafobook.models.Note

class NoteRepo(private val dao: NoteDao) {

    val allNotes: LiveData<List<Note>> = dao.getAll()
    val liveTags: LiveData<List<String>> = dao.getTags()

    suspend fun insert(note: Note){
        dao.insert(note)
    }

    suspend fun delete(notes: Note){
        dao.delete(notes)
    }

    suspend fun update(notes: Note){
        dao.update(notes)
    }

    suspend fun getNote(id: Int): Note{
        return dao.getNote(id)
    }
}
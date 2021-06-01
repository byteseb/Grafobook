package com.byteseb.grafobook.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.byteseb.grafobook.models.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    val repo: NoteRepo

    val allNotes: LiveData<List<Note>>
    val allTags: LiveData<List<String>>

    init {
        val dao = NotesDB.getDB(app).noteDao()
        repo = NoteRepo(dao)
        allNotes = repo.allNotes
        allTags = repo.liveTags
    }

    suspend fun getNote(id: Int): Note{
        return repo.getNote(id)
    }

    fun insert(note: Note){
        CoroutineScope(Dispatchers.IO).launch {
            repo.insert(note)
        }
    }

    fun update(note: Note){
        CoroutineScope(Dispatchers.IO).launch {
            repo.update(note)
        }
    }

    fun delete(note: Note){
        CoroutineScope(Dispatchers.IO).launch {
            repo.delete(note)
        }
    }
}
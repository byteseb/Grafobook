package com.byteseb.grafobook.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.byteseb.grafobook.models.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    val repo: NoteRepo

    val allNotes: LiveData<List<Note>>
    val allTags: LiveData<List<String>>
    var insertedId: MutableLiveData<Int>

    init {
        val dao = NotesDB.getDB(app).noteDao()
        repo = NoteRepo(dao)
        allNotes = repo.allNotes
        allTags = repo.liveTags
        insertedId = MutableLiveData<Int>()
    }

    fun insert(note: Note){
        var value = -1
        CoroutineScope(Dispatchers.IO).launch {
            value = repo.insert(note).toInt()
        }
        insertedId.value = value
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
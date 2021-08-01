package com.byteseb.grafobook.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_import.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class ImportActivity : BaseActivity(), MainInterface {

    private var viewModel: NoteViewModel? = null

    val gson = Gson()
    val type = object : TypeToken<ArrayList<Note>>() {}.type
    var adapter: NotesAdapter? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val notes = ArrayList(adapter?.currentList)
        val selection = ArrayList(adapter?.selection)
        outState.putParcelableArrayList("notes", notes)
        outState.putParcelableArrayList("selection", selection)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val notes = savedInstanceState.getParcelableArrayList<Note>("notes")
        adapter?.submitList(notes)
        if (notes != null) {
            if (notes.isNotEmpty()) {
                importButton.isEnabled = true
                importButton.text =
                    getString(R.string.import_amount, notes.size.toString())
                importDesc.visibility = View.GONE
                adapter?.selectAll()
            } else {
                importButton.isEnabled = false
                importButton.text = getString(R.string.import_amount, "0")
                importDesc.visibility = View.VISIBLE
            }
        }

        val selection = savedInstanceState.getParcelableArrayList<Note>("selection")
        adapter?.selection!!.clear()
        if (selection != null) {
            adapter?.selection!!.addAll(selection)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentTheme()
        setContentView(R.layout.activity_import)
        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        importButton.text = getString(R.string.import_amount, "0")
        importBack.setOnSingleClickListener {
            super.onBackPressed()
        }

        adapter = NotesAdapter(this, false, supportFragmentManager)
        adapter?.listener = this
        importRecycler.layoutManager =
            StaggeredGridLayoutManager(
                resources.getInteger(R.integer.column_count),
                StaggeredGridLayoutManager.VERTICAL
            )
        importRecycler.adapter = adapter

        val previewRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    refresh(it.data?.data!!)
                }
            }

        browseButton.setOnSingleClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            //Opening picker
            previewRequest.launch(intent)
        }

        importButton.setOnSingleClickListener {
            //Inserts data to database
            for (note in adapter?.selection!!) {
                insertNote(note)
            }
            //Returns to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        if (intent.data != null) {
            refresh(intent.data)
        }
    }

    fun refresh(uri: Uri?) {
        if (uri != null) {
            val stream = contentResolver.openInputStream(uri)
            try {
                var array = ArrayList<Note>()
                thread {
                    try {
                        val content = stream?.bufferedReader().use { it?.readText() }
                        array = gson.fromJson(content, type)
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                getString(R.string.error_reading_file),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    runOnUiThread {
                        adapter?.submitList(array)
                        if (array.isNotEmpty()) {
                            importButton.isEnabled = true
                            importButton.text =
                                getString(R.string.import_amount, array.size.toString())
                            importDesc.visibility = View.GONE
                            adapter?.selectAll()
                        } else {
                            importButton.isEnabled = false
                            importButton.text = getString(R.string.import_amount, "0")
                            importDesc.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.error_reading_file), Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            Toast.makeText(this, getString(R.string.error_reading_file), Toast.LENGTH_LONG).show()
        }
    }

    fun insertNote(note: Note) {
        viewModel!!.insert(
            Note(
                name = note.name,
                color = note.color,
                favorite = note.favorite,
                lastDate = note.lastDate,
                creationDate = System.currentTimeMillis(),
                tags = note.tags,
                reminder = note.reminder,
                content = note.content,
                password = note.password
            )
        )
    }

    override fun onFavFilterChecked(value: Boolean) {
    }

    override fun onTagFilterChecked(tag: String, value: Boolean) {
    }

    override fun onSelectionUpdated(canCheck: Boolean, selection: ArrayList<Note>) {
        importButton.text = getString(R.string.import_amount, adapter?.selection?.size.toString())
        importButton.isEnabled = adapter?.selection!!.isNotEmpty()
    }
}
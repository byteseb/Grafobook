package com.byteseb.grafobook.activities

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.room.NotesDB
import com.byteseb.grafobook.utils.ColorUtils
import com.byteseb.grafobook.utils.HtmlUtils
import com.byteseb.grafobook.utils.PrefUtils
import com.byteseb.grafobook.utils.WidgetUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_note_picker.*
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NotePicker : BaseActivity(), MainInterface {

    private var viewModel: NoteViewModel? = null
    val notesList = ArrayList<Note>()
    var adapter: NotesAdapter? = null

    var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent.extras != null){
            widgetId = intent.extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_CANCELED, resultValue)

        if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
            finish()
        }

        setCurrentTheme()
        setContentView(R.layout.activity_note_picker)
        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        pickBack.setOnSingleClickListener {
            super.onBackPressed()
        }

        adapter = NotesAdapter(this, false, supportFragmentManager)
        adapter?.listener = this
        adapter?.selecting = true
        adapter?.maxSelectionSize = 2
        pickerRecycler.layoutManager =
            StaggeredGridLayoutManager(
                resources.getInteger(R.integer.column_count),
                StaggeredGridLayoutManager.VERTICAL
            )
        pickerRecycler.adapter = adapter

        viewModel!!.allNotes.observe(this) {
            notesList.clear()

            for (note in it) {
                notesList.add(note)
            }

            emptyVisibility()
            adapter?.submitList(applyFilters())
        }

        pickButton.setOnSingleClickListener {
            applyConfig()
        }

        pickSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val search = s.toString().lowercase().trim()
                adapter?.submitList(applyFilters(search))
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selection = ArrayList(adapter?.selection)
        outState.putParcelableArrayList("selection", selection)
        outState.putString("query", pickSearch.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val query = savedInstanceState.getString("query")
        if (query.isNullOrEmpty()) {
            adapter?.submitList(applyFilters())
        } else {
            adapter?.submitList(applyFilters(query))
        }

        val selection = savedInstanceState.getParcelableArrayList<Note>("selection")
        adapter?.selection!!.clear()
        if (selection != null) {
            adapter?.selection!!.addAll(selection)
        }
    }

    fun applyConfig(){
        val manager = AppWidgetManager.getInstance(this)
        val noteId = adapter?.selection!![0].id
        PrefUtils.setPref(widgetId.toString(), noteId, this)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        manager.updateAppWidget(widgetId, WidgetUtils.getNoteWidget(this, widgetId, manager))
        finish()
    }

    fun emptyVisibility() {
        if (notesList.isEmpty()) {
            pickEmpty.visibility = View.VISIBLE
        } else {
            pickEmpty.visibility = View.GONE
        }
    }

    fun applyFilters(search: String = ""): ArrayList<Note> {
        val result = ArrayList<Note>()
        result.addAll(notesList)
        //Filter search results first

        if (search.isNotEmpty()) {
            result.clear()
            for (note in notesList) {
                val cleanContent =
                    HtmlUtils.fromHtml(note.content).toString().lowercase().trim()
                if (note.name.lowercase().trim().contains(search) || cleanContent.contains(search)
                ) {
                    result.add(note)
                }
            }
        }

        return result
    }

    override fun onFavFilterChecked(value: Boolean) {

    }

    override fun onTagFilterChecked(tag: String, value: Boolean) {
    }

    override fun onSelectionUpdated(canCheck: Boolean, selection: ArrayList<Note>) {
        pickButton.isEnabled = selection.isNotEmpty()
        adapter?.selecting = true
    }
}
package com.byteseb.grafobook.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_import.*
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.concurrent.thread

class ImportActivity : AppCompatActivity() {

    fun setCurrentTheme() {
        val defColor = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getInt("defaultColor", MainActivity.Colors.ORANGE.ordinal)!!
        val defBack = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getBoolean("nightTheme", false)!!

        when (defColor) {
            MainActivity.Colors.RED.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Red)
            }
            MainActivity.Colors.BLUE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Blue)
            }
            MainActivity.Colors.GREEN.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Green)
            }
            MainActivity.Colors.ORANGE.ordinal -> {
                setTheme(R.style.Theme_Grafobook)
            }
            MainActivity.Colors.YELLOW.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Yellow)
            }
            MainActivity.Colors.PINK.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Pink)
            }
            MainActivity.Colors.PURPLE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Purple)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //If it is a version lower than Q, set the theme manually
            if (!defBack) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private var viewModel: NoteViewModel? = null

    val gson = Gson()
    val type = object : TypeToken<ArrayList<Note>>() {}.type
    var adapter: NotesAdapter? = null

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
        importRecycler.layoutManager =
            GridLayoutManager(this, resources.getInteger(R.integer.column_count))
        importRecycler.adapter = adapter
        //When file has been picked

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
            for(note in adapter?.currentList!!){
                insertNote(note)
            }
            //Returns to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        if(intent.data != null){
            refresh(intent.data)
        }
    }

    fun refresh(uri: Uri?){
        if (uri != null) {
            val stream = contentResolver.openInputStream(uri)
            try {
                var array = ArrayList<Note>()
                thread {
                    try{
                        val content = stream?.bufferedReader().use { it?.readText() }
                        array = gson.fromJson(content, type)
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, getString(R.string.error_reading_file), Toast.LENGTH_LONG).show()
                        }
                    }
                    runOnUiThread {
                        adapter?.submitList(array)
                        if(array.isNotEmpty()){
                            importButton.isEnabled = true
                            importButton.text = getString(R.string.import_amount, array.size.toString())
                            importDesc.visibility = View.GONE
                        }
                        else{
                            importButton.isEnabled = false
                            importButton.text = getString(R.string.import_amount, "0")
                            importDesc.visibility = View.VISIBLE
                        }
                        importButton.isEnabled = array.isNotEmpty()
                    }
                }
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.error_reading_file), Toast.LENGTH_LONG).show()
            }
        }
        else{
            Toast.makeText(this, getString(R.string.error_reading_file), Toast.LENGTH_LONG).show()
        }
    }

    fun insertNote(note: Note){
        viewModel!!.insert(Note(
            name = note.name,
            color = note.color,
            favorite = note.favorite,
            lastDate = note.lastDate,
            creationDate = System.currentTimeMillis(),
            tags = note.tags,
            reminder = note.reminder,
            content = note.content
        ))
    }
}
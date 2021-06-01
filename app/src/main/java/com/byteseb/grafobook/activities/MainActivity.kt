package com.byteseb.grafobook.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.byteseb.grafobook.AlarmReceiver
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.FilterAdapter
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Filter
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.utils.HtmlUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking

interface MainInterface {
    fun onFavFilterChecked(value: Boolean)
    fun onTagFilterChecked(tag: String, value: Boolean)
    fun onCanCheckChanged(value: Boolean)
}

class MainActivity : AppCompatActivity(), MainInterface {

    lateinit var alarmManager: AlarmManager
    lateinit var notManager: NotificationManager
    lateinit var notChannel: NotificationChannel

    lateinit var viewModel: NoteViewModel

    var notesList = ArrayList<Note>()
    var notesAdapter: NotesAdapter? = null
    val remindersList = ArrayList<PendingIntent>()

    var filterAdapter: FilterAdapter? = null
    val filterList = ArrayList<Filter>()

    var currentFilters = ArrayList<String>()
    var filterFavorites: Boolean = false

    val CHANNEL_ID = "grafobook.reminderchannel"

    enum class Colors{
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    enum class Backgrounds{
        LIGHT, DARK
    }

    fun setCurrentTheme(){

        val defColor = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getInt("defaultColor", Colors.ORANGE.ordinal)!!
        val defBack = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getBoolean("nightTheme", false)!!

        when(defColor){
            Colors.RED.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Red)
            }
            Colors.BLUE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Blue)
            }
            Colors.GREEN.ordinal -> {
                setTheme(R.style.Theme_Grafobook)
            }
            Colors.ORANGE.ordinal -> {
                setTheme(R.style.Theme_Grafobook)
            }
            Colors.YELLOW.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Yellow)
            }
            Colors.PINK.ordinal-> {
                setTheme(R.style.Theme_Grafobook_Pink)
            }
            Colors.PURPLE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Purple)
            }
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            //If it is a version lower than Q, set the theme manually
            if(!defBack){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    fun applyFilters(search: String = ""): ArrayList<Note> {
        var result = ArrayList<Note>()
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

        //Then, filter by tags. If there are none, the items will remain the same
        result = result.filter {
            it.tags.containsAll(currentFilters)
        } as ArrayList<Note>

        //Filter favorites only if requested
        if (filterFavorites) {
            result = result.filter {
                it.favorite
            } as ArrayList<Note>
        }
        //Applying sort automatically
        applySort(result)
        return result
    }

    fun accentColor(): Int {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.colorAccent, value, true)
        return ContextCompat.getColor(this, value.resourceId)
    }

    fun clearAlarms() {
        for (alarm in remindersList) {
            alarmManager.cancel(alarm)
        }
    }

    fun getBundle(note: Note): Bundle {
        val bundle = Bundle()
        bundle.putInt("id", note.id)
        bundle.putString("name", note.name)
        bundle.putString("content", note.content)
        bundle.putString("color", accentColor(note))
        bundle.putBoolean("favorite", note.favorite)
        bundle.putLong("creationDate", note.creationDate)
        bundle.putLong("lastDate", note.lastDate)
        bundle.putLong("reminder", note.reminder)
        val tags = ArrayList<String>()
        tags.addAll(note.tags)
        bundle.putStringArrayList("tags", tags)
        return bundle
    }

    fun accentColor(note: Note): String {
        if (note.color == null) {
            val value = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, value, true)
            return String.format("#%06X", ContextCompat.getColor(this, value.resourceId))

        } else {
            return note.color
        }
    }

    fun setAlarm(note: Note): PendingIntent {
        val time: Long = note.reminder
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtras(getBundle(note))
        val pendingIntent =
            PendingIntent.getBroadcast(this, note.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (time != -1L && time > System.currentTimeMillis()) {
            //If reminder exists and has not passed
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
        return pendingIntent
    }

    fun createNotChannel() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notChannel.enableLights(true)
            notChannel.lightColor = accentColor()
            notChannel.enableVibration(true)

            notManager = getSystemService(NotificationManager::class.java)
            notManager.createNotificationChannel(notChannel)
        }
    }

    override fun onFavFilterChecked(value: Boolean) {
        filterFavorites = value
        notesAdapter?.submitList(applyFilters())
    }

    override fun onTagFilterChecked(tag: String, value: Boolean) {
        if (value && !currentFilters.contains(tag)) {
            currentFilters.add(tag)
        } else {
            currentFilters.remove(tag)
        }
        notesAdapter?.submitList(applyFilters())
    }

    override fun onCanCheckChanged(value: Boolean) {
        if(value){
            selectingText.text = getString(R.string.selecting)
        }
        else{
            selectingText.text = ""
        }
    }

    override fun onBackPressed() {
        if(notesAdapter?.canSelect!!){
            notesAdapter?.clearSelection()
            notesAdapter?.submitList(applyFilters())
        }
        else{
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedIndexes = ArrayList<Int>()
        val canSelect = notesAdapter?.canSelect
        if(canSelect!!){
            for(note in notesAdapter!!.selection){
                selectedIndexes.add(notesAdapter?.currentList!!.indexOf(note))
            }
            outState.putIntegerArrayList("selectedIndexes", selectedIndexes)
        }

        outState.putBoolean("canSelect", canSelect)
        outState.putBoolean("filterFavorites", filterFavorites)
        outState.putStringArrayList("currentFilters", currentFilters)
        outState.putString("query", editSearch.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val canSelect = savedInstanceState.getBoolean("canSelect")
        filterFavorites = savedInstanceState.getBoolean("filterFavorites")
        currentFilters = savedInstanceState.getStringArrayList("currentFilters") as ArrayList<String>

        if(canSelect){
            notesAdapter?.canSelect = canSelect
            onCanCheckChanged(canSelect)
            notesAdapter?.setSelection(savedInstanceState.getIntegerArrayList("selectedIndexes") as ArrayList<Int>)
        }

        val query = savedInstanceState.getString("query")
        if(query.isNullOrEmpty()){
            notesAdapter?.submitList(applyFilters())
        }
        else{
            notesAdapter?.submitList(applyFilters(query))
        }

        //Adds first filter chip, will always be the favorites filter
        if(filterList.isNotEmpty()){
            filterList[0].checked = filterFavorites
            runBlocking {
                for (filter in filterAdapter?.currentList!!){
                    if(!filter.favFilter){
                        filter.checked = currentFilters.contains(filter.text)
                    }
                }
                filterAdapter?.submitList(ArrayList(filterList))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentTheme()
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        notesAdapter = NotesAdapter(this, fragmentManager = supportFragmentManager, viewModel = viewModel, listener = this)
        recycler.adapter = notesAdapter
        recycler.layoutManager =
            GridLayoutManager(this, resources.getInteger(R.integer.column_count))

        //Loading notes & reminders
        viewModel.allNotes.observe(this) {
            notesList.clear()

            clearAlarms()
            remindersList.clear()

            filterList.clear()
            //Adds first filter chip, will always be the favorites filter
            filterList.add(Filter(getString(R.string.favorites), filterFavorites, true))
            for (note in it) {
                if (note.reminder != -1L) {
                    remindersList.add(setAlarm(note))
                }
                for (tag in note.tags) {
                    val filter = Filter(tag, currentFilters.contains(tag), false)
                    if(!filterList.contains(filter)){
                        filterList.add(filter)
                    }
                }
                notesList.add(note)
            }

            emptyVisibility()
            notesAdapter?.submitList(applyFilters())
            filterAdapter?.submitList(ArrayList(filterList))
        }

        createNotChannel()
        //recycler & filterRecycler


        filterAdapter = FilterAdapter(this)
        filterRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterRecycler.adapter = filterAdapter

        //Init UI
        newFAB.setOnSingleClickListener {
            notesAdapter?.clearSelection()
            val intent = Intent(this, NoteActivity::class.java)
            startActivity(intent)
        }

        val sortMenu = PopupMenu(this, sortButton)
        sortMenu.menuInflater.inflate(R.menu.sort_menu, sortMenu.menu)
        sortButton.setOnClickListener {
            initSortMenu(sortMenu.menu)
            sortMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.sortRecent -> {
                        saveSort(Sort.RECENT.ordinal)
                    }
                    R.id.sortCreation -> {
                        saveSort(Sort.CREATION.ordinal)
                    }
                    R.id.sortAlpha -> {
                        saveSort(Sort.ALPHA.ordinal)
                    }
                    R.id.sortReverse -> {
                        saveReverseSort(!isSortReversed())
                    }
                }
                notesAdapter?.submitList(applyFilters())
                true
            }
            sortMenu.show()
        }

        val selectMenu = PopupMenu(this, selectButton)
        selectMenu.menuInflater.inflate(R.menu.select_menu, selectMenu.menu)
        selectButton.setOnClickListener {
            selectMenu.menu.getItem(0).isEnabled = notesAdapter?.currentList!!.isNotEmpty()
            if (notesAdapter?.selection!!.isEmpty()) {
                for (index in 1..2) {
                    selectMenu.menu.getItem(index).isEnabled = false
                }
            }
            else{
                for(index in 1..2){
                    selectMenu.menu.getItem(index).isEnabled = true
                }
            }
            selectMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.selectAll -> {
                        notesAdapter?.selectAll()
                    }
                    R.id.selectClear -> {
                        notesAdapter?.clearSelection()
                    }
                    R.id.selectDelete -> {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(R.string.delete_notes)
                        builder.setMessage(getString(R.string.want_to_delete_notes))
                        builder.setPositiveButton(android.R.string.ok){dialog, which ->
                            notesAdapter?.deleteSelection()
                        }
                        builder.setNegativeButton(android.R.string.cancel){dialog, which ->
                            dialog.cancel()
                        }
                        builder.show()
                    }
                }
                true
            }
            selectMenu.show()
        }

        optionsButton.setOnSingleClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val search = s.toString().lowercase().trim()
                notesAdapter?.submitList(applyFilters(search))
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    fun emptyVisibility() {
        if (notesList.isEmpty()) {
            emptyLayout.visibility = View.VISIBLE
        } else {
            emptyLayout.visibility = View.GONE
        }
    }

    enum class Sort {
        RECENT, CREATION, ALPHA
    }

    fun sortRecent(list: ArrayList<Note>) {
        list.sortByDescending {
            it.lastDate
        }
        saveSort(Sort.RECENT.ordinal)
    }

    fun sortCreation(list: ArrayList<Note>) {
        list.sortByDescending {
            it.creationDate
        }
        saveSort(Sort.CREATION.ordinal)
    }

    fun sortAlpha(list: ArrayList<Note>) {
        list.sortBy {
            it.name
        }
        saveSort(Sort.ALPHA.ordinal)
    }

    fun isSortReversed(): Boolean {
        return getSharedPreferences("preferences", Context.MODE_PRIVATE)
            ?.getBoolean("reverseSort", false)!!
    }

    fun applySort(list: ArrayList<Note>) {
        val defSort = getSharedPreferences("preferences", Context.MODE_PRIVATE)
            ?.getInt("defaultSort", 0)

        val revSort = isSortReversed()

        when (defSort) {
            Sort.ALPHA.ordinal -> {
                sortAlpha(list)
            }
            Sort.RECENT.ordinal -> {
                sortRecent(list)
            }
            Sort.CREATION.ordinal -> {
                sortCreation(list)
            }
        }

        if (revSort) {
            //Reverse sort
            list.reverse()
        }
    }

    fun initSortMenu(menu: Menu) {
        val prevInvert = isSortReversed()
        val defSort = getSharedPreferences("preferences", Context.MODE_PRIVATE)
            ?.getInt("defaultSort", 0)
        when (defSort) {
            Sort.ALPHA.ordinal -> {
                checkItem(menu, Sort.ALPHA.ordinal)
            }
            Sort.RECENT.ordinal -> {
                checkItem(menu, Sort.RECENT.ordinal)
            }
            Sort.CREATION.ordinal -> {
                checkItem(menu, Sort.CREATION.ordinal)
            }
        }
        menu.getItem(3).isChecked = prevInvert
    }

    fun checkItem(menu: Menu, index: Int) {
        for (i in 0..2) {
            menu.getItem(i).isChecked = i == index
        }
    }

    fun saveSort(value: Int) {
        getSharedPreferences("preferences", Context.MODE_PRIVATE).edit()
            .putInt("defaultSort", value).apply()
    }

    private fun saveReverseSort(value: Boolean) {
        val pref = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        pref?.edit()?.putBoolean("reverseSort", value)?.apply()
    }
}
package com.byteseb.grafobook.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.byteseb.grafobook.AlarmReceiver
import com.byteseb.grafobook.BuildConfig
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.FilterAdapter
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Filter
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.sheets.ShareOptions
import com.byteseb.grafobook.utils.HtmlUtils
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface MainInterface {
    fun onFavFilterChecked(value: Boolean)
    fun onTagFilterChecked(tag: String, value: Boolean)
    fun onSelectionUpdated(canCheck: Boolean, selection: ArrayList<Note>)
}

class MainActivity : AppCompatActivity(), MainInterface, ShareOptions.ShareOptionsInterface {

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

    enum class Colors {
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    fun setCurrentTheme() {

        val defColor = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getInt("defaultColor", Colors.ORANGE.ordinal)!!
        val defBack = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getBoolean("nightTheme", false)!!

        when (defColor) {
            Colors.RED.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Red)
            }
            Colors.BLUE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Blue)
            }
            Colors.GREEN.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Green)
            }
            Colors.ORANGE.ordinal -> {
                setTheme(R.style.Theme_Grafobook)
            }
            Colors.YELLOW.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Yellow)
            }
            Colors.PINK.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Pink)
            }
            Colors.PURPLE.ordinal -> {
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
            PendingIntent.getBroadcast(this, note.id, intent, PendingIntent.FLAG_CANCEL_CURRENT)
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

    override fun onSelectionUpdated(canCheck: Boolean, selection: ArrayList<Note>) {

    }

    override fun onBackPressed() {
        if (notesAdapter?.selecting!!) {
            notesAdapter?.clearSelection()
            notesAdapter?.submitList(applyFilters())
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedIndexes = ArrayList<Int>()
        val canSelect = notesAdapter?.selecting
        if (canSelect!!) {
            for (note in notesAdapter!!.selection) {
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
        val selecting = savedInstanceState.getBoolean("canSelect")
        filterFavorites = savedInstanceState.getBoolean("filterFavorites")
        currentFilters =
            savedInstanceState.getStringArrayList("currentFilters") as ArrayList<String>

        if (selecting) {
            notesAdapter?.selecting = selecting
            onSelectionUpdated(selecting, notesAdapter?.selection!!)
            notesAdapter?.setSelection(savedInstanceState.getIntegerArrayList("selectedIndexes") as ArrayList<Int>)
        }

        val query = savedInstanceState.getString("query")
        if (query.isNullOrEmpty()) {
            notesAdapter?.submitList(applyFilters())
        } else {
            notesAdapter?.submitList(applyFilters(query))
        }

        //Adds first filter chip, will always be the favorites filter
        if (filterList.isNotEmpty()) {
            filterList[0].checked = filterFavorites
            runBlocking {
                for (filter in filterAdapter?.currentList!!) {
                    if (!filter.favFilter) {
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

        notesAdapter = NotesAdapter(
            this,
            fragmentManager = supportFragmentManager,
            viewModel = viewModel,
            listener = this
        )
        recycler.adapter = notesAdapter
        recycler.layoutManager =
            GridLayoutManager(this, resources.getInteger(R.integer.column_count))

        nestedScroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

            if (nestedScroll.scrollY > oldScrollY) {
                searchDock.animate()
                    .translationY(searchDock.height.toFloat())
                    .alpha(0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            searchDock.clearAnimation()
//                                searchDock.visibility = View.GONE
                        }
                    })
                    .duration = 150
            } else if (nestedScroll.scrollY < oldScrollY) {
                searchDock.visibility = View.VISIBLE
                searchDock.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                        }
                    })
                    .duration = 150
            }
        }

        refreshLayout.setColorSchemeColors(accentColor())
        refreshLayout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                this,
                R.color.cardBackground
            )
        )
        refreshLayout.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                notesList.clear()

                clearAlarms()
                remindersList.clear()

                filterList.clear()
                //Adds first filter chip, will always be the favorites filter
                filterList.add(Filter(getString(R.string.favorites), filterFavorites, true))
                for (note in viewModel.allNotes.value!!) {
                    if (note.reminder != -1L) {
                        remindersList.add(setAlarm(note))
                    }
                    for (tag in note.tags) {
                        val filter = Filter(tag, currentFilters.contains(tag), false)
                        if (!filterList.contains(filter)) {
                            filterList.add(filter)
                        }
                    }
                    notesList.add(note)
                }

                emptyVisibility()
                notesAdapter?.submitList(applyFilters())
                filterAdapter?.submitList(ArrayList(filterList))
                refreshLayout.isRefreshing = false
            }
        }

        recycler.setOnClickListener {
            println("Touched")
        }

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
                    if (!filterList.contains(filter)) {
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

        menuButton.setOnSingleClickListener {
            val menu = PopupMenu(this, menuButton)
            menu.menuInflater.inflate(R.menu.main_menu, menu.menu)

            val settingsIndex = 0
            val importIndex = 1
            val sortIndex = 2
            val selectIndex = 3

            //Sorting menu
            val prevInvert = isSortReversed()
            val defSort = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                ?.getInt("defaultSort", 0)
            menu.menu.getItem(sortIndex).subMenu.getItem(1).isChecked = defSort == Sort.ALPHA.ordinal
            menu.menu.getItem(sortIndex).subMenu.getItem(2).isChecked = defSort == Sort.CREATION.ordinal
            menu.menu.getItem(sortIndex).subMenu.getItem(3).isChecked = defSort == Sort.RECENT.ordinal
            //Reverse item
            menu.menu.getItem(sortIndex).subMenu.getItem(0).isChecked = prevInvert

            //Selection menu
            //Selection title
            val selectionTitle: String = if (notesAdapter?.selecting!!) {
                getString(R.string.selection_amount, notesAdapter?.selection?.size.toString())
            } else {
                getString(R.string.selection)
            }
            menu.menu.getItem(selectIndex).title = selectionTitle
            //Select all item
            menu.menu.getItem(selectIndex).subMenu.getItem(4).isEnabled = notesList.isNotEmpty()
            //All other selection options (cancel, duplicate, share, delete...)
            for (index in 0 until menu.menu.getItem(selectIndex).subMenu.size - 1) {
                //Apply this for all items excepting the select all item
                menu.menu.getItem(selectIndex).subMenu.getItem(index).isEnabled =
                    notesAdapter?.selecting!! && notesAdapter?.selection!!.isNotEmpty()
            }

            //Click
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.importItem -> {
                        if (notesAdapter?.selecting!!) {
                            notesAdapter?.clearSelection()
                        }
                        val intent = Intent(this, ImportActivity::class.java)
                        startActivity(intent)
                    }
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
                    R.id.selectAll -> {
                        notesAdapter?.selectAll()
                    }
                    R.id.selectClear -> {
                        notesAdapter?.clearSelection()
                    }
                    R.id.selectDuplicate -> {
                        notesAdapter?.duplicateSelection()
                    }
                    R.id.selectShare -> {
                        val shareOptions = ShareOptions()
                        shareOptions.show(supportFragmentManager, "shareOptions")
                    }
                    R.id.selectDelete -> {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(R.string.delete_notes)
                        builder.setMessage(getString(R.string.want_to_delete_notes))
                        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                            notesAdapter?.deleteSelection()
                        }
                        builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
                            dialog.cancel()
                        }
                        builder.show()
                    }
                    R.id.settingsItem -> {
                        if (notesAdapter?.selecting!!) {
                            notesAdapter?.clearSelection()
                        }
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                notesAdapter?.submitList(applyFilters())
                true
            }
            menu.show()
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

    fun saveSort(value: Int) {
        getSharedPreferences("preferences", Context.MODE_PRIVATE).edit()
            .putInt("defaultSort", value).apply()
    }

    private fun saveReverseSort(value: Boolean) {
        val pref = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        pref?.edit()?.putBoolean("reverseSort", value)?.apply()
    }

    override fun onOptionSelected(option: Int) {
        when (option) {
            ShareOptions.Options.GRAFO_FILE.ordinal -> {
                //Saving list into json
                val gson = Gson()
                val name = "notes"
                val extension = ".gfbk"
                val file = File.createTempFile(name, extension, cacheDir)
                val data = gson.toJson(notesAdapter?.selection)
                //Writing data to temporary file
                try {
                    val writer = FileWriter(file)
                    writer.append(data)
                    writer.flush()
                    writer.close()
                    val uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        file
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "application/grafobook"
                    }
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            resources.getText(R.string.send_to)
                        )
                    )
                } catch (ex: Exception) {
                    Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT).show()
                }
            }
            ShareOptions.Options.HTML_FILE.ordinal -> {
                if(notesAdapter?.selection?.size!! == 1){
                    //Make single html file
                    val name = "notes"
                    val extension = ".html"
                    val file = File.createTempFile(name, extension, cacheDir)
                    val data = notesAdapter?.selection!![0].content
                    //Writing data to temporary file
                    try {
                        val writer = FileWriter(file)
                        writer.append(data)
                        writer.flush()
                        writer.close()
                        val uri = FileProvider.getUriForFile(
                            this,
                            BuildConfig.APPLICATION_ID + ".fileprovider",
                            file
                        )
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "text/html"
                        }
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                resources.getText(R.string.send_to)
                            )
                        )
                    } catch (ex: Exception) {
                        Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT).show()
                    }
                }else if(notesAdapter?.selection?.size!! > 1){
                    //Make zip that contains html files
                    val zipName = "notes"
                    val zipExtension = ".zip"
                    val zipFile = File.createTempFile(zipName, zipExtension, cacheDir)
                    val zipStream =
                        ZipOutputStream(FileOutputStream(zipFile))
                    val extension = ".html"

                    for (index in notesAdapter?.selection!!.indices) {
                        try {
                            //Creating single html file
                            val number = index + 1
                            val name = "note$number"
                            val file = File.createTempFile(name, extension, cacheDir)
                            val note = notesAdapter?.selection!![index]

                            val writer = FileWriter(file)
                            writer.append(note.content)
                            writer.flush()
                            writer.close()

                            //Adding file to zip
                            val fi = FileInputStream(file)
                            val origin = BufferedInputStream(fi)
                            val entry = ZipEntry(name + extension)
                            zipStream.putNextEntry(entry)
                            origin.copyTo(zipStream, 1024)
                            origin.close()
                        } catch (ex: IOException) {
                            Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT).show()
                        }
                    }
                    zipStream.close()

                    val uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        zipFile
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "application/zip"
                    }
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            resources.getText(R.string.send_to)
                        )
                    )
                }
            }
        }
    }
}
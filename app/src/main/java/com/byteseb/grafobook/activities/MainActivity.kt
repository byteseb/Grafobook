package com.byteseb.grafobook.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.byteseb.grafobook.BuildConfig
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.FilterAdapter
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Filter
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.sheets.ShareOptions
import com.byteseb.grafobook.utils.*
import com.byteseb.grafobook.utils.ColorUtils.Companion.accentColor
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface MainInterface {
    fun onFavFilterChecked(value: Boolean)
    fun onTagFilterChecked(tag: String, value: Boolean)
    fun onSelectionUpdated(canCheck: Boolean, selection: ArrayList<Note>)
}

class MainActivity : BaseActivity(), MainInterface, ShareOptions.ShareOptionsInterface {

    lateinit var viewModel: NoteViewModel

    var notesList = ArrayList<Note>()
    var notesAdapter: NotesAdapter? = null

    var filterAdapter: FilterAdapter? = null
    val filterList = ArrayList<Filter>()

    var currentFilters = ArrayList<String>()
    var filterFavorites: Boolean = false

    fun applyFilters(search: String = ""): ArrayList<Note> {
        var result = ArrayList<Note>()
        result.addAll(notesList)

        runBlocking {
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
        }
        return result
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
        NotificationUtils.createChannels(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        notesAdapter = NotesAdapter(
            this,
            fragmentManager = supportFragmentManager,
            viewModel = viewModel,
            listener = this
        )
        recycler.adapter = notesAdapter
        recycler.layoutManager = StaggeredGridLayoutManager(
            resources.getInteger(R.integer.column_count),
            StaggeredGridLayoutManager.VERTICAL
        )

            WidgetUtils.refreshWidgets(this)

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

        refreshLayout.setColorSchemeColors(accentColor(this))
        refreshLayout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                this,
                R.color.cardBackground
            )
        )
        refreshLayout.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                notesList.clear()
                filterList.clear()
                //Adds first filter chip, will always be the favorites filter
                filterList.add(Filter(getString(R.string.favorites), filterFavorites, true))
                for (note in viewModel.allNotes.value!!) {
                    for (tag in note.tags) {
                        val filter = Filter(tag, currentFilters.contains(tag), false)
                        if (!filterList.contains(filter)) {
                            filterList.add(filter)
                        }
                    }
                    notesList.add(note)
                }
                ReminderUtils.refreshReminders(this@MainActivity)
                WidgetUtils.refreshWidgets(this@MainActivity)
                emptyVisibility()
                notesAdapter?.submitList(applyFilters())
                filterAdapter?.submitList(ArrayList(filterList))
                refreshLayout.isRefreshing = false
            }
        }

        //Loading notes & reminders
        viewModel.allNotes.observe(this) {
            notesList.clear()
            filterList.clear()
            //Adds first filter chip, will always be the favorites filter
            filterList.add(Filter(getString(R.string.favorites), filterFavorites, true))
            for (note in it) {
                for (tag in note.tags) {
                    val filter = Filter(tag, currentFilters.contains(tag), false)
                    if (!filterList.contains(filter)) {
                        filterList.add(filter)
                    }
                }
                notesList.add(note)
            }
            ReminderUtils.refreshReminders(this@MainActivity)
            emptyVisibility()
            notesAdapter?.submitList(applyFilters())
            filterAdapter?.submitList(ArrayList(filterList))
        }

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
            val menu = PopupMenu(this as Activity, menuButton)
            menu.menuInflater.inflate(R.menu.main_menu, menu.menu)

            val settingsIndex = 0
            val importIndex = 1
            val sortIndex = 2
            val selectIndex = 3

            //Sorting menu
            val prevInvert = isSortReversed()
            val defSort = PrefUtils.getPref("defaultSort", 0, this)
            var defIndex = 1
            if(defSort == Sort.RECENT.ordinal){
                defIndex = 3
            }
            else if(defSort == Sort.CREATION.ordinal){
                defIndex = 2
            }
            menu.menu.getItem(sortIndex).subMenu.getItem(defIndex).isChecked = true

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
            if (notesAdapter?.selection!!.isEmpty()) {
                //Make selection submenu invisible and add a select all item as that is the only option available
                // and it is not worth it to have a full submenu just for that
                menu.menu.getItem(selectIndex).isVisible = false
                val selectAll = menu.menu.add(R.string.select_all_item)
                selectAll.setOnMenuItemClickListener {
                    notesAdapter!!.selectAll()
                    true
                }
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
        return PrefUtils.getPref("reverseSort", false, this) as Boolean
    }

    fun applySort(list: ArrayList<Note>) {
        val defSort = PrefUtils.getPref("defaultSort", 0, this)
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
        PrefUtils.setPref("defaultSort", value, this)
    }

    private fun saveReverseSort(value: Boolean) {
        PrefUtils.setPref("reverseSort", value, this)
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
                    Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            ShareOptions.Options.HTML_FILE.ordinal -> {
                if (notesAdapter?.selection?.size!! == 1) {
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
                        Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (notesAdapter?.selection?.size!! > 1) {
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
                            Toast.makeText(
                                this,
                                getString(R.string.failed_create),
                                Toast.LENGTH_SHORT
                            ).show()
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
            ShareOptions.Options.PLAIN_TEXT.ordinal -> {
                if (notesAdapter?.selection?.size!! == 1) {
                    //Get note's content
                    val note = notesAdapter?.selection!![0]
                    val cleanContent =
                        HtmlUtils.fromHtml(note.content).toString().lowercase().trim()
                    //Writing data to temporary file
                    try {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            putExtra(Intent.EXTRA_TEXT, note.name + "\n" + cleanContent)
                            type = "text/plain"
                        }
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                resources.getText(R.string.send_to)
                            )
                        )
                    } catch (ex: Exception) {
                        Toast.makeText(this, getString(R.string.failed_create), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (notesAdapter?.selection?.size!! > 1) {
                    //Make zip that contains txt files
                    val zipName = "notes"
                    val zipExtension = ".zip"
                    val zipFile = File.createTempFile(zipName, zipExtension, cacheDir)
                    val zipStream =
                        ZipOutputStream(FileOutputStream(zipFile))
                    val extension = ".txt"

                    for (index in notesAdapter?.selection!!.indices) {
                        try {
                            //Creating single html file
                            val number = index + 1
                            val name = "note$number"
                            val file = File.createTempFile(name, extension, cacheDir)
                            val note = notesAdapter?.selection!![index]
                            val cleanContent =
                                HtmlUtils.fromHtml(note.content).toString().lowercase().trim()

                            val writer = FileWriter(file)
                            writer.append(note.name + "\n" + cleanContent)
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
                            Toast.makeText(
                                this,
                                getString(R.string.failed_create),
                                Toast.LENGTH_SHORT
                            ).show()
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
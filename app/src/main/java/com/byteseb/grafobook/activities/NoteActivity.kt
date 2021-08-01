package com.byteseb.grafobook.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.*
import android.app.backup.BackupManager
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.text.*
import android.text.format.DateUtils
import android.text.style.*
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.TagsAdapter
import com.byteseb.grafobook.interfaces.TagInterface
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.room.NotesDB
import com.byteseb.grafobook.utils.*
import com.byteseb.grafobook.utils.ColorUtils.Companion.isDarkColor
import com.byteseb.grafobook.utils.ColorUtils.Companion.tintBox
import com.byteseb.grafobook.utils.ColorUtils.Companion.tintImg
import com.byteseb.grafobook.utils.ColorUtils.Companion.tintImgButton
import com.byteseb.grafobook.utils.ColorUtils.Companion.tintText
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.activity_note.editFav
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class NoteActivity : BaseActivity(), TagInterface {

    private lateinit var viewModel: NoteViewModel

    var note: Note? = null

    var currentReminder: Long = -1
    var currentColor: String? = null
    var tagsList = ArrayList<String>()
    var currentPass: String? = null

    val allTags = ArrayList<String>()
    var edited: Boolean = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("currentReminder", currentReminder)
        outState.putString("currentColor", currentColor)
        outState.putStringArrayList("tagsList", tagsList)
        outState.putString("password", currentPass)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentReminder = savedInstanceState.getLong("currentReminder")
        currentColor = savedInstanceState.getString("currentColor")
        tagsList = savedInstanceState.getStringArrayList("tagsList")!!
        currentPass = savedInstanceState.getString("password")

        refreshDate()
        refreshReminder()
        refreshColors()
        refreshLock()
        refreshTags()
    }

    private var hasAutoSave: Boolean = true
    private var showBack: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentTheme()
        setContentView(R.layout.activity_note)
        hasAutoSave = PrefUtils.getPref("autoSave", true, this) as Boolean

        showBack =
            PrefUtils.getPref("showBack", true, this) as Boolean
        if (showBack) {
            backButton.visibility = View.VISIBLE
        } else {
            backButton.visibility = View.GONE
        }

        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        viewModel.allNotes.observe(this) {
            allTags.clear()

            for (note in it) {
                for (tag in note.tags) {
                    if (!allTags.contains(tag)) {
                        allTags.add(tag)
                    }
                }
            }
            ReminderUtils.refreshReminders(this@NoteActivity)
        }

        loadData()
        if (currentPass != null) {
            lockBackground.visibility = View.VISIBLE
            notePass.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (s.toString() == currentPass) {
                        lockBackground.animate().alpha(0f).setDuration(200)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    super.onAnimationEnd(animation)
                                    lockBackground.clearAnimation()
                                    lockBackground.visibility = View.GONE
                                }
                            })
                        editContent.requestFocus()
                        noteScroll.post { noteScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                }

            })
        } else {
            lockBackground.visibility = View.GONE
        }

        initRecycler()
        if (currentPass == null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            editContent.requestFocus()
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            notePass.requestFocus()
        }

        //Bottom sheet
//        val sheetBehavior = BottomSheetBehavior.from(contentSheet)

        backButton.setOnClickListener {
            super.onBackPressed()
        }

        editFav.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                autoSave()
            }
        }

        editName.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editorLayout.animate()
                    .translationY(editorLayout.height.toFloat())
                    .alpha(0f)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            editorLayout.clearAnimation()
                            editorLayout.visibility = View.GONE
                        }
                    })
                    .duration = 150
            }
        }

        editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                edited = true
                autoSave()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        editContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrBlank()) {
                    edited = true
                    autoSave()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        editContent.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && editorLayout.visibility == View.GONE) {
                editorLayout.visibility = View.VISIBLE
                editorLayout.animate()
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
        if (!hasAutoSave) {
            saveFAB.visibility = View.VISIBLE
            var oldScroll = 0
            noteScroll.viewTreeObserver.addOnScrollChangedListener {
                if (noteScroll.scrollY > oldScroll) {
                    saveFAB.hide()
                } else if (noteScroll.scrollY < oldScroll) {
                    saveFAB.show()
                }
                oldScroll = noteScroll.scrollY
            }
        } else {
            saveFAB.visibility = View.GONE
        }

        //Editor
        saveFAB.setOnClickListener {
            saveData()
        }

        boldButton.setOnClickListener {
            toggleSpan(StyleSpan(Typeface.BOLD))
            edited = true
            autoSave()
        }
        italButton.setOnClickListener {
            toggleSpan(StyleSpan(Typeface.ITALIC))
            edited = true
            autoSave()
        }
        underButton.setOnClickListener {
            toggleSpan(UnderlineSpan())
            edited = true
            autoSave()
        }
        strikeButton.setOnClickListener {
            toggleSpan(StrikethroughSpan())
            edited = true
            autoSave()
        }
        foregroundButton.setOnClickListener {
            val colorMenu = PopupMenu(this, foregroundButton)
            colorMenu.menuInflater.inflate(R.menu.color_menu, colorMenu.menu)
            var foreColor = 0
            colorMenu.setOnMenuItemClickListener {
                var canPaint = true
                when (it.itemId) {
                    R.id.noneItem -> {
                        if (selectionHasStyle(ForegroundColorSpan(0))) {
                            //If already has a color, show the menu to change the color to another one
                            removeStyle(ForegroundColorSpan(0))
                            canPaint = false
                        } else {
                            //If has no color, do not do anything
                            colorMenu.dismiss()
                            canPaint = false
                        }
                    }
                    R.id.redItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.red)
                    }
                    R.id.pinkItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.pink)
                    }
                    R.id.purpleItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.purple)
                    }
                    R.id.blueItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.blue)
                    }
                    R.id.greenItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.green)
                    }
                    R.id.yellowItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.yellow)
                    }
                    R.id.orangeItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.orange)
                    }

                }
                if (canPaint) {
                    if (selectionHasStyle(ForegroundColorSpan(0))) {
                        //If already has a color, show the menu to change the color to another one
                        removeStyle(ForegroundColorSpan(0))
                        insertStyle(ForegroundColorSpan(foreColor))
                    } else {
                        //If has no color, show menu
                        insertStyle(ForegroundColorSpan(foreColor))
                    }
                }
                edited = true
                autoSave()
                true
            }
            colorMenu.show()
        }
        backgroundButton.setOnClickListener {
            val colorMenu = PopupMenu(this, backgroundButton)
            colorMenu.menuInflater.inflate(R.menu.color_menu, colorMenu.menu)
            var foreColor = 0
            colorMenu.setOnMenuItemClickListener {
                var canPaint = true
                when (it.itemId) {
                    R.id.noneItem -> {
                        if (selectionHasStyle(BackgroundColorSpan(0))) {
                            //If already has a color, show the menu to change the color to another one
                            removeStyle(BackgroundColorSpan(0))
                            canPaint = false
                        } else {
                            //If has no color, do not do anything
                            colorMenu.dismiss()
                            canPaint = false
                        }
                    }
                    R.id.redItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_red)
                    }
                    R.id.pinkItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_pink)
                    }
                    R.id.purpleItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_purple)
                    }
                    R.id.blueItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_blue)
                    }
                    R.id.greenItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_green)
                    }
                    R.id.yellowItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_yellow)
                    }
                    R.id.orangeItem -> {
                        foreColor = ContextCompat.getColor(this, R.color.semi_orange)
                    }
                }
                if (canPaint) {
                    if (selectionHasStyle(BackgroundColorSpan(0))) {
                        //If already has a color, show the menu to change the color to another one
                        removeStyle(BackgroundColorSpan(0))
                        insertStyle(BackgroundColorSpan(foreColor))
                    } else {
                        //If has no color, show menu
                        insertStyle(BackgroundColorSpan(foreColor))
                    }
                }
                edited = true
                autoSave()
                true
            }
            colorMenu.show()
        }
        colorButton.setOnClickListener {
            val colorMenu = PopupMenu(this, colorButton)
            colorMenu.menuInflater.inflate(R.menu.color_menu, colorMenu.menu)
            colorMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.noneItem -> {
                        currentColor = null
                    }
                    R.id.redItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.red))
                    }
                    R.id.pinkItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.pink))
                    }
                    R.id.purpleItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.purple))
                    }
                    R.id.blueItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.blue))
                    }
                    R.id.greenItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.green))
                    }
                    R.id.yellowItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.yellow))
                    }
                    R.id.orangeItem -> {
                        currentColor =
                            String.format("#%06X", ContextCompat.getColor(this, R.color.orange))
                    }
                }
                refreshColors()
                edited = true
                autoSave()
                true
            }
            colorMenu.show()
        }

        remindButton.setOnSingleClickListener {
            if (currentReminder == -1L) {
                //It does not have a reminder, add one
                val calendar = GregorianCalendar()
                val dateListener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    if (TimeUtils.isFutureDate(calendar.timeInMillis) || DateUtils.isToday(calendar.timeInMillis)) {
                        val timeListener =
                            TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                if (TimeUtils.isFutureDate(calendar.timeInMillis)) {
                                    currentReminder = calendar.timeInMillis
                                    refreshReminder()
                                    autoSave()
                                } else {
                                    failDateTimePicker()
                                }
                            }
                        TimePickerDialog(
                            this,
                            timeListener,
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    } else {
                        //If user tries to troll the app and picks a date older than today
                        failDateTimePicker()
                    }
                }
                DatePickerDialog(
                    this,
                    dateListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            } else {
                //Has a reminder, ask for confirmation to remove it
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.remove_reminder))
                val formattedDate =
                    SimpleDateFormat(
                        getString(R.string.year_month_date_hour_minute),
                        Locale.getDefault()
                    ).format(currentReminder)
                builder.setMessage(
                    String.format(
                        getString(R.string.would_like_remove),
                        formattedDate
                    )
                )
                builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                    currentReminder = -1
                    refreshReminder()
                    edited = true
                    autoSave()
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                builder.show()
            }
        }

        tagButton.setOnSingleClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.add_tag))
            val view = LayoutInflater.from(this).inflate(R.layout.edit_tag, null)
            val editTag = view.findViewById<AutoCompleteTextView>(R.id.editTag)
            builder.setView(view)
            //Tag adapter for autocomplete
            val adapter = ArrayAdapter(this, android.R.layout.select_dialog_item, allTags)
            editTag.setAdapter(adapter)
            builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                if (editTag.text.isNotEmpty()) {
                    if (!tagsList.contains(editTag.text.toString())) {
                        tagsList.add(editTag.text.toString())
                        this.adapter.notifyDataSetChanged()
                        autoSave()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.cant_add_existing_tag),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    dialog.dismiss()
                    Toast.makeText(this, getString(R.string.cant_add_empty_tag), Toast.LENGTH_SHORT)
                        .show()
                }
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
                dialog.cancel()
            }
            builder.show()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            editTag.requestFocus()
        }

        lockButton.setOnSingleClickListener {
            if (currentPass == null) {
                //Does not have a password, set one
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.add_pass))
                builder.setMessage(getString(R.string.add_pass_desc))
                val view = LayoutInflater.from(this).inflate(R.layout.edit_password, null)
                val editPass = view.findViewById<TextInputEditText>(R.id.editPass)
                builder.setView(view)
                builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                    if (editPass.text!!.isNotEmpty()) {
                        currentPass = editPass.text.toString()
                        edited = true
                        autoSave()
                        dialog.dismiss()
                        refreshLock()
                    }
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                builder.show()
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                editPass.requestFocus()
            } else {
                //Has a password, ask for removal
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.remove_password))
                builder.setMessage(getString(R.string.would_like_remove_pass))
                builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                    currentPass = null
                    refreshLock()
                    edited = true
                    autoSave()
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                builder.show()
            }
        }

        clearButton.setOnClickListener {
            clearStyles()
            edited = true
            autoSave()
        }
    }

    fun cursorX() = editContent.layout.getPrimaryHorizontal(selectionStart())

    fun cursorY(): Int {
        val line = editContent.layout.getLineForOffset(selectionStart())
        val baseline = editContent.layout.getLineBaseline(line)
        val ascent = editContent.layout.getLineAscent(line)
        return baseline + ascent
    }

    val adapter = TagsAdapter(supportFragmentManager, true, this, context = this)

    fun initRecycler() {
        adapter.submitList(tagsList)
        editRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        editRecycler.adapter = adapter
    }

    fun failDateTimePicker() {
        Toast.makeText(this, R.string.choose_time_in_future, Toast.LENGTH_SHORT).show()
    }

    fun loadData() {
        //Load essential data
        val id = intent.getIntExtra("id", -1)
        //It is an existing note, get it from database and cache it
        val context = this
        runBlocking {
            note = NotesDB.getDB(context).noteDao().getNote(id)
        }

        //Password
        currentPass = note?.password
        if (note == null) {
            //It is not an existing note
            refreshColors()
        } else {

            currentColor = note!!.color
            currentReminder = note!!.reminder

            //Favorite
            editFav.isChecked = note!!.favorite

            //Date
            refreshDate()

            //Reminder
            refreshReminder()

            refreshLock()

            //Tag
            tagsList.clear()
            tagsList.addAll(note!!.tags)
            refreshTags()

            refreshColors()

            //Content
            editContent.setText(HtmlUtils.fromHtml(note!!.content))

            //Name
            editName.setText(note?.name)
        }
    }

    fun getCursorLine(): Int {
        if (selectionStart() == -1) {
            return -1
        }
        return editContent.layout.getLineForOffset(selectionStart())
    }

    fun refreshColors() {
        window.statusBarColor = statusColor()
        editName.setTextColor(contrastColor())
        noteConstraint.setBackgroundColor(statusColor())
        refreshDate()
        refreshReminder()
        refreshTags()
        adapter.forceRefresh()
        //Tinting buttons
        saveFAB.backgroundTintList = ColorStateList.valueOf(noteAccentColor())
        tintButton(backButton, contrastColor())
        for (button in editorButtons.children) {
            tintButton(button, noteAccentColor())
        }
    }

    fun refreshTags() {
        if (currentColor != null) {
            adapter.color = Color.parseColor(currentColor)
        } else {
            adapter.color = null
        }
        adapter.notifyDataSetChanged()
    }

    fun noteAccentColor(): Int {
        if (currentColor == null) {
            val value = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, value, true)
            return ContextCompat.getColor(this, value.resourceId)
        } else {
            return parsedColor()
        }
    }

    fun statusColor(): Int {
        if (currentColor != null) {
            return Color.parseColor(currentColor)
        } else {
            return getColor(R.color.background)
        }
    }

    fun noteAccentColor(note: Note): String {
        if (note.color == null) {
            val value = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, value, true)
            return String.format("#%06X", ContextCompat.getColor(this, value.resourceId))

        } else {
            return note.color
        }
    }

    private fun parsedColor(): Int {
        if (currentColor != null) {
            return Color.parseColor(currentColor)
        } else {
            return Color.WHITE
        }
    }

    fun contrastColor(): Int {
        val finalColor: Int
        if (currentColor != null) {
            if (isDarkColor(Color.parseColor(currentColor))) {
                finalColor = Color.WHITE
            } else {
                finalColor = Color.BLACK
            }
        } else {
            if (isDarkColor(getColor(R.color.cardBackground))) {
                finalColor = Color.WHITE
            } else {
                finalColor = Color.BLACK
            }
        }
        return finalColor
    }

    fun refreshDate() {
        val contrastColor = contrastColor()
        tintImg(editDateIcon, contrastColor)
        tintText(editDate, contrastColor)

        var date: Long = System.currentTimeMillis()

        if (note != null) {
            date = note!!.lastDate
        }
        editDate.text = TimeUtils.getSimpleDate(date, this)
    }

    fun refreshReminder() {
        val textColor = contrastColor()
        if (currentReminder == -1L) {
            //Has no reminder
            editRemindLayout.visibility = View.GONE
            remindButton.setImageResource(R.drawable.ic_round_notification_add_32)
        } else {
            //Has reminder
            editRemind.text = TimeUtils.getSimpleDate(currentReminder, this)
            if (currentReminder < System.currentTimeMillis()) {
                editRemindIcon.setImageResource(R.drawable.ic_baseline_alarm_on_24)
            } else {
                editRemindIcon.setImageResource(R.drawable.ic_round_notifications_active_24)
            }
            editRemindLayout.visibility = View.VISIBLE
            tintText(editRemind, textColor)
            tintImg(editRemindIcon, textColor)
            remindButton.setImageResource(R.drawable.ic_round_notifications_off_32)
        }
    }

    fun refreshLock() {
        if (currentPass == null) {
            lockButton.setImageResource(R.drawable.ic_round_lock_32)
        } else {
            lockButton.setImageResource(R.drawable.ic_round_lock_open_32)
        }
    }

    fun tintButton(view: View, color: Int?) {
        if (view is ImageButton) {
            tintImgButton(view, color)
        } else if (view is CheckBox) {
            tintBox(view, color)
        }
    }

    fun toggleSpan(style: Any, start: Int = selectionStart(), end: Int = selectionEnd()) {
        if (selectionHasStyle(style)) {
            //Selection already has this style, so remove it
            removeStyle(style, start, end)
        } else {
            //Selection does not have this style, add it
            insertStyle(style, start, end)
        }
    }

    fun clearStyles() {
        for (char in editContent.text.getSpans(
            selectionStart(),
            selectionEnd(),
            CharacterStyle::class.java
        )) {
            editContent.text.removeSpan(char)
        }
    }

    fun selectionHasStyle(style: Any): Boolean {
        for (char in editContent.text.getSpans(
            selectionStart(),
            selectionEnd(),
            style::class.java
        )) {
            return if (style is StyleSpan) {
                (char as StyleSpan).style == style.style
            } else {
                char::class == style::class
            }
        }
        return false
    }

    fun getSelectedSpan(
        style: Any,
        start: Int = selectionStart(),
        end: Int = selectionEnd()
    ): Any? {
        for (char in editContent.text.getSpans(
            start,
            end,
            style::class.java
        )) {
            if (style is StyleSpan) {
                if ((char as StyleSpan).style == style.style) {
                    return char
                }
            } else {
                if (char::class == style::class) {
                    return char
                }
            }
        }
        return null
    }

    fun insertStyle(style: Any, start: Int = selectionStart(), end: Int = selectionEnd()) {
        try {
            editContent.text.setSpan(
                style,
                start,
                end,
                SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
            )
        } catch (ex: IndexOutOfBoundsException) {

        }
    }

    fun removeStyle(style: Any, start: Int = selectionStart(), end: Int = selectionEnd()) {
        editContent.text.removeSpan(getSelectedSpan(style, start, end))
    }

    fun selectionStart(): Int {
        return editContent.selectionStart
    }

    fun selectionEnd(): Int {
        return editContent.selectionEnd
    }

    fun saveData(showToast: Boolean = true) {
        if (!editName.text.isNullOrBlank()) {
            editContent.clearComposingText()
            if (note == null) {
                //It is not an existing note, create a new one
                note = Note(
                    name = getName(),
                    favorite = editFav.isChecked,
                    color = currentColor,
                    creationDate = System.currentTimeMillis(),
                    lastDate = System.currentTimeMillis(),
                    tags = tagsList,
                    reminder = currentReminder,
                    content = HtmlUtils.toHtml(editContent.text),
                    password = currentPass
                )
                val context = this
                runBlocking {
                    val id = NotesDB.getDB(context).noteDao().insert(note!!)
                    note?.id = id.toInt()
                }
                if (showToast) {
                    Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                }
                refreshDate()
            } else {
                //It is an existing note, update it
                note = Note(
                    id = note!!.id,
                    name = getName(),
                    favorite = editFav.isChecked,
                    color = currentColor,
                    creationDate = note!!.creationDate,
                    lastDate = System.currentTimeMillis(),
                    tags = tagsList,
                    reminder = currentReminder,
                    content = HtmlUtils.toHtml(editContent.text),
                    password = currentPass
                )
                viewModel.update(note!!)
                if (showToast) {
                    Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                }
                refreshDate()
            }
        }
        WidgetUtils.refreshWidgets(this)
        BackupManager.dataChanged(packageName)
    }

    fun autoSave() {
        if (hasAutoSave) {
            saveData(false)
        }
    }

    fun getName(): String {
        if (editName.text.toString().isNotEmpty()) {
            return editName.text.toString()
        } else {
            return getString(R.string.default_note_name)
        }
    }

    override fun onCloseTag(string: String) {
        tagsList.remove(string)
        adapter.notifyDataSetChanged()
        edited = true
        autoSave()
    }
}
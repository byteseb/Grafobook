package com.byteseb.grafobook.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.text.util.Linkify
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.byteseb.grafobook.AlarmReceiver
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.TagsAdapter
import com.byteseb.grafobook.interfaces.TagInterface
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.models.Filter
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.utils.HtmlUtils
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.activity_note.editFav
import kotlinx.android.synthetic.main.note_card.*
import java.lang.IndexOutOfBoundsException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NoteActivity : AppCompatActivity(), TagInterface {

    private lateinit var viewModel: NoteViewModel

    var note: Note? = null

    var currentReminder: Long = -1
    var currentColor: String? = null
    var tagsList = ArrayList<String>()

    val allTags = ArrayList<String>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("currentReminder", currentReminder)
        outState.putString("currentColor", currentColor)
        outState.putStringArrayList("tagsList", tagsList)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentReminder = savedInstanceState.getLong("currentReminder")
        currentColor = savedInstanceState.getString("currentColor")
        tagsList = savedInstanceState.getStringArrayList("tagsList")!!

        refreshDate()
        refreshReminder()
        refreshColors()
        adapter.submitList(ArrayList(tagsList))
    }

    enum class Colors {
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

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
                setTheme(R.style.Theme_Grafobook)
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

    var statusDefaultColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentTheme()
        setContentView(R.layout.activity_note)
        statusDefaultColor = window.statusBarColor
        viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        createNotChannel()

        viewModel.allNotes.observe(this) {
            clearAlarms()
            remindersList.clear()

            for (note in it) {
                if (note.reminder != -1L) {
                    remindersList.add(setAlarm(note))
                }
            }
        }

        viewModel.allNotes.observe(this) {
            allTags.clear()

            for (note in it) {
                for (tag in note.tags) {
                    if (!allTags.contains(tag)) {
                        allTags.add(tag)
                    }
                }
            }
        }

        initRecycler()

        loadData()
        editContent.linksClickable = true
        editContent.autoLinkMask = Linkify.WEB_URLS
        editContent.movementMethod = LinkMovementMethod.getInstance()

        backButton.setOnClickListener {
            super.onBackPressed()
        }

        var oldScroll = 0
        noteScroll.viewTreeObserver.addOnScrollChangedListener {
            if(noteScroll.scrollY > oldScroll){
                saveFAB.hide()
            }
            else if(noteScroll.scrollY < oldScroll){
                saveFAB.show()
            }
            oldScroll = noteScroll.scrollY
        }

        //Editor
        saveFAB.setOnClickListener {
            editContent.isCursorVisible = false
            saveData()
        }

        boldButton.setOnClickListener {
            toggleSpan(StyleSpan(Typeface.BOLD))
        }
        italButton.setOnClickListener {
            toggleSpan(StyleSpan(Typeface.ITALIC))
        }
        underButton.setOnClickListener {
            toggleSpan(UnderlineSpan())
        }
        strikeButton.setOnClickListener {
            toggleSpan(StrikethroughSpan())
        }
        headerButton.setOnClickListener {
            toggleSpan(RelativeSizeSpan(1.25f))
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
                true
            }
            colorMenu.show()
        }

        remindButton.setOnSingleClickListener {
            if (currentReminder == -1L) {
                //It does not have a reminder, add one
                val calendar = GregorianCalendar.getInstance()

                val dateListener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    if (day < GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                        //If user tries to troll the app and picks a date older than today
                        failDateTimePicker()
                    } else {
                        val timeListener =
                            TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                                if (hour >= GregorianCalendar.getInstance()
                                        .get(Calendar.HOUR_OF_DAY)
                                ) {
                                    //Hour is either current or future hour
                                    if (minute >= GregorianCalendar.getInstance()
                                            .get(Calendar.MINUTE)
                                    ) {
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        currentReminder = calendar.timeInMillis
                                        refreshReminder()
                                    } else {
                                        failDateTimePicker()
                                    }
                                } else {
                                    //If user tries to troll the app and picks a date older than today
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
                        this.adapter.submitList(ArrayList(tagsList))
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
            editTag.requestFocus()
        }
        clearButton.setOnClickListener {
            clearStyles()
        }
    }

    val adapter = TagsAdapter(supportFragmentManager, true, this)

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
        val id = intent.getIntExtra("id", -1)
        if (id == -1) {
            //It is not an existing note

            refreshDate()
        } else {
            //It is an existing note, get it from database and cache it
            note = Note(
                id,
                intent.getStringExtra("name")!!,
                intent.getBooleanExtra("favorite", false),
                intent.getStringExtra("color"),
                intent.getLongExtra("creationDate", -1),
                intent.getLongExtra("lastDate", System.currentTimeMillis()),
                intent.getStringArrayListExtra("tags")!!.toList(),
                intent.getLongExtra("reminder", -1),
                intent.getStringExtra("content")!!
            )

            currentColor = note!!.color
            currentReminder = note!!.reminder

            //Name
            editName.setText(note?.name)

            //Favorite
            editFav.isChecked = note!!.favorite

            //Date
            refreshDate()

            //Reminder
            refreshReminder()

            //Tag
            tagsList.clear()
            tagsList.addAll(note!!.tags)
            adapter.submitList(ArrayList(tagsList))

            refreshColors()

            //Content
            editContent.setText(HtmlUtils.fromHtml(note!!.content))
        }
    }

    lateinit var alarmManager: AlarmManager
    lateinit var notManager: NotificationManager
    lateinit var notChannel: NotificationChannel

    val CHANNEL_ID = "grafobook.reminderchannel"

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

    val remindersList = ArrayList<PendingIntent>()

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

    fun refreshColors() {
        window.statusBarColor = statusColor()
        editName.setTextColor(accentColor())
        tintBox(editFav, accentColor())
        //Tinting buttons
        saveFAB.backgroundTintList = ColorStateList.valueOf(accentColor())
        tintButton(backButton, accentColor())
        for (button in editorButtons.children) {
            tintButton(button as ImageButton, accentColor())
        }
    }

    fun accentColor(): Int {
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
            return statusDefaultColor
        }
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

    private fun parsedColor(): Int {
        if (currentColor != null) {
            return Color.parseColor(currentColor)
        } else {
            return Color.WHITE
        }
    }

    private fun textColor(): Int {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorSecondary, value, true)
        return ContextCompat.getColor(this, value.resourceId)
    }

    fun refreshDate() {
        val textColor = textColor()
        tintImg(editDateIcon, textColor)
        tintText(editDate, textColor)

        var date: Long = System.currentTimeMillis()

        if (note != null) {
            date = note!!.lastDate
        }

        val formattedDate =
            SimpleDateFormat(
                getString(R.string.year_month_date_hour_minute),
                Locale.getDefault()
            ).format(date)
        editDate.setText(formattedDate)
    }

    fun refreshReminder() {
        val textColor = textColor()
        if (currentReminder == -1L) {
            //Has no reminder
            editRemindLayout.visibility = View.GONE
            remindButton.setImageResource(R.drawable.ic_round_notification_add_32)
        } else {
            //Has reminder
            editRemind.setText(
                SimpleDateFormat(
                    getString(R.string.year_month_date_hour_minute),
                    Locale.getDefault()
                ).format(
                    currentReminder
                )
            )
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

    fun tintImg(view: ImageView, color: Int?) {

        if (color == null) {
            return
        }

        view.setColorFilter(color)
    }

    fun tintText(view: TextView, color: Int?) {

        if (color == null) {
            return
        }

        view.setTextColor(color)
    }

    fun tintButton(view: ImageButton, color: Int?) {

        if (color == null) {
            return
        }

        view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    fun tintBox(view: CheckBox, color: Int?) {

        if (color == null) {
            return
        }

        view.buttonTintList = ColorStateList.valueOf(color)
    }

    fun toggleSpan(style: Any) {
        if (selectionHasStyle(style)) {
            //Selection already has this style, so remove it
            removeStyle(style)
        } else {
            //Selection does not have this style, add it
            insertStyle(style)
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
            if (style is StyleSpan) {
                if ((char as StyleSpan).style == style.style) {
                    return true
                }
            } else {
                if (char::class == style::class) {
                    return true
                }
            }
        }
        return false
    }

    fun getSelectedSpan(style: Any): Any? {
        for (char in editContent.text.getSpans(
            selectionStart(),
            selectionEnd(),
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

    fun insertStyle(style: Any) {
        try{
            editContent.text.setSpan(
                style,
                selectionStart(),
                selectionEnd(),
                SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        catch(ex: IndexOutOfBoundsException){

        }
    }

    fun removeStyle(style: Any) {
        editContent.text.removeSpan(getSelectedSpan(style))
    }

    fun selectionStart(): Int {
        return editContent.selectionStart
    }

    fun selectionEnd(): Int {
        return editContent.selectionEnd
    }

    fun saveData() {
        if (canSave()) {
            editContent.clearComposingText()
            if (note == null) {
                //It is not an existing note, create a new one
                note = Note(
                    name = editName.text.toString(),
                    favorite = editFav.isChecked,
                    color = currentColor,
                    creationDate = System.currentTimeMillis(),
                    lastDate = System.currentTimeMillis(),
                    tags = tagsList,
                    reminder = currentReminder,
                    content = HtmlUtils.toHtml(editContent.text)
                )
                viewModel.insert(note!!)
                Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                refreshDate()
            } else {
                //It is an existing note, update it
                note = Note(
                    id = note!!.id,
                    name = editName.text.toString(),
                    favorite = editFav.isChecked,
                    color = currentColor,
                    creationDate = note!!.creationDate,
                    lastDate = System.currentTimeMillis(),
                    tags = tagsList,
                    reminder = currentReminder,
                    content = HtmlUtils.toHtml(editContent.text)
                )
                viewModel.update(note!!)
                Toast.makeText(this, getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                refreshDate()
            }
        } else {
            Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
        }
    }

    fun canSave(): Boolean {
        return editName.text.toString().trim().isNotEmpty()
    }

    override fun onCloseTag(string: String) {
        val index = tagsList.indexOf(string)
        tagsList.remove(string)
        adapter.submitList(ArrayList(tagsList))
    }
}
package com.byteseb.grafobook.utils

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.byteseb.grafobook.R
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.receivers.AlarmReceiver
import com.byteseb.grafobook.room.NotesDB
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException

class ReminderUtils {
    companion object {

        lateinit var alarmManager: AlarmManager
        val remindersList = ArrayList<PendingIntent>()

        fun refreshReminders(context: Context) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            cancelAlarms()
            remindersList.clear()
            runBlocking {
                for (note in NotesDB.getDB(context).noteDao().getAllNotes()) {
                    if (note.reminder != -1L) {
                        //If note has reminder
                        remindersList.add(setAlarm(note, context))
                    }
                }
            }
        }

        fun setAlarm(note: Note, context: Context): PendingIntent {
            val time: Long = note.reminder
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtras(getBundle(note, context))
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    note.id,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE //Immutable flag for Android 12
                )
            if (time != -1L && time > System.currentTimeMillis()) {
                //If reminder exists and has not passed
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }
                catch(exception: IllegalStateException){

                }
            }

            return pendingIntent
        }

        fun cancelAlarms() {
            for (alarm in remindersList) {
                alarmManager.cancel(alarm)
            }
        }

        fun getBundle(note: Note, context: Context): Bundle {
            val bundle = Bundle()
            bundle.putInt("id", note.id)
            bundle.putString("name", note.name)
            bundle.putString("content", HtmlUtils.fromHtml(note.content).toString())
            bundle.putString("color", accentColorString(context, note))
            bundle.putBoolean("favorite", note.favorite)
            bundle.putLong("creationDate", note.creationDate)
            bundle.putLong("lastDate", note.lastDate)
            bundle.putLong("reminder", note.reminder)
            val tags = ArrayList<String>()
            tags.addAll(note.tags)
            bundle.putStringArrayList("tags", tags)
            return bundle
        }

        fun accentColorString(context: Context, note: Note): String {
            if (note.color == null) {
                val value = TypedValue()
                context.theme.resolveAttribute(android.R.attr.colorAccent, value, true)
                return String.format("#%06X", ContextCompat.getColor(context, value.resourceId))

            } else {
                return note.color
            }
        }

        private fun accentColor(context: Context): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorAccent, value, true)
            return ContextCompat.getColor(context, value.resourceId)
        }
    }
}
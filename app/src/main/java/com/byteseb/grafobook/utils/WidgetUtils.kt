package com.byteseb.grafobook.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.NoteActivity
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NotesDB
import com.byteseb.grafobook.widgets.NoteWidget
import com.byteseb.grafobook.widgets.UpcomingWidget
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class WidgetUtils {
    companion object {

        fun refreshWidgets(context: Context) {
            //Note widget
            val manager = AppWidgetManager.getInstance(context)
            val noteIntent = Intent(context, NoteWidget::class.java)
            noteIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val noteIds = manager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))
            noteIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, noteIds)
            context.sendBroadcast(noteIntent)

            //Upcoming widget
            val upcomingIntent = Intent(context, UpcomingWidget::class.java)
            upcomingIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val upcomingIds =
                manager.getAppWidgetIds(ComponentName(context, UpcomingWidget::class.java))
            upcomingIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, upcomingIds)
            context.sendBroadcast(upcomingIntent)
        }

        fun getNoteWidget(
            context: Context,
            appWidgetId: Int,
            appWidgetManager: AppWidgetManager,
        ): RemoteViews {
            val showLockPrev: Boolean = PrefUtils.getPref("showLockPrev", false, context) as Boolean
            val noteId = PrefUtils.getPref(appWidgetId.toString(), -1, context) as Int

            val views = RemoteViews(context.packageName, R.layout.note_widget)
            //Has a valid ID
            var note: Note?
            runBlocking {
                note = NotesDB.getDB(context).noteDao().getNote(noteId)
            }

            if (note != null) {
                views.setViewVisibility(R.id.nwInfo, View.VISIBLE)
                views.setViewVisibility(R.id.nwError, View.GONE)
                //Color & Background
                val intent = Intent(context, NoteActivity::class.java)
                intent.putExtra("id", note?.id)
                //Add values to intent
                val pending =
                    PendingIntent.getActivity(
                        context,
                        noteId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                views.setOnClickPendingIntent(R.id.nwBack, pending)
                val contrastColor: Int
                val backColor: Int
                if (note?.color != null) {
                    //Tint the widget's background color to the note's color
                    backColor = Color.parseColor(note?.color)
                    if (ColorUtils.isDarkColor(Color.parseColor(note?.color!!))) {
                        contrastColor = Color.WHITE
                    } else {
                        contrastColor = Color.BLACK
                    }
                } else {
                    //Note does not have any color. Apply default one
                    val darkTheme = context.resources.getBoolean(R.bool.dark_theme)
                    //Tint the widget's background color to the theme's card background color
                    backColor = context.getColor(R.color.cardBackground)
                    if (!darkTheme) {
                        //Has light theme
                        contrastColor = Color.BLACK
                    } else {
                        //Has dark theme
                        contrastColor = Color.WHITE
                    }
                }

                views.setInt(
                    R.id.nwBack,
                    "setColorFilter",
                    backColor
                )

                //Name
                views.setTextViewText(R.id.nwName, note!!.name)
                views.setTextColor(R.id.nwName, contrastColor)

                //Favorite
                if (note?.favorite!!) {
                    views.setImageViewResource(R.id.nwFav, R.drawable.ic_round_star_24)
                } else {
                    views.setImageViewResource(R.id.nwFav, R.drawable.ic_round_star_border_24)
                }

                views.setInt(R.id.nwFav, "setColorFilter", contrastColor)

                //Reminder
                if (note?.reminder == -1L) {
                    views.setViewVisibility(R.id.nwReminder, View.GONE)
                } else {
                    views.setInt(R.id.nwReminder, "setColorFilter", contrastColor)
                    if (note?.reminder!! < System.currentTimeMillis()) {
                        views.setImageViewResource(
                            R.id.nwReminder,
                            R.drawable.ic_baseline_alarm_on_24
                        )
                    } else {
                        views.setImageViewResource(
                            R.id.nwReminder,
                            R.drawable.ic_round_notifications_active_24
                        )
                    }
                    views.setViewVisibility(R.id.nwReminder, View.VISIBLE)
                }

                //Date
                views.setTextViewText(
                    R.id.nwDate,
                    TimeUtils.getSimpleDate(note?.lastDate!!, context)
                )
                views.setTextColor(R.id.nwDate, contrastColor)

                //Content
                if (note?.content!!.isNotEmpty()) {
                    views.setTextViewText(R.id.nwContent, HtmlUtils.fromHtml(note?.content!!))
                    views.setViewVisibility(R.id.nwContent, View.VISIBLE)
                    views.setTextColor(R.id.nwContent, contrastColor)
                } else {
                    views.setTextViewText(R.id.nwContent, "")
                    views.setViewVisibility(R.id.nwContent, View.GONE)
                }

                //Password
                if (note?.password == null) {
                    views.setViewVisibility(R.id.nwLock, View.GONE)
                } else {
                    views.setInt(R.id.nwLock, "setColorFilter", contrastColor)
                    views.setViewVisibility(R.id.nwLock, View.VISIBLE)
                    if (showLockPrev) {
                        views.setViewVisibility(R.id.nwContent, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.nwContent, View.GONE)
                    }
                }

                //Tags
                val tagTextColor: Int
                val tagBack: Int
                if (note?.color != null) {
                    tagBack = R.drawable.colored_tag
                    if (ColorUtils.isDarkColor(ColorUtils.darkenColor(Color.parseColor(note?.color)))) {
                        tagTextColor = Color.WHITE
                    } else {
                        tagTextColor = Color.BLACK
                    }
                } else {
                    tagBack = R.drawable.default_tag
                    if (ColorUtils.isDarkColor(backColor)) {
                        tagTextColor = Color.WHITE
                    } else {
                        tagTextColor = Color.BLACK
                    }
                }
                views.removeAllViews(R.id.nwTags)
                for (tag in note?.tags!!) {
                    val tagView = getTagView(context, tag, tagTextColor, tagBack)
                    views.addView(R.id.nwTags, tagView)
                }
            } else {
                views.setViewVisibility(R.id.nwInfo, View.GONE)
                val darkTheme = context.resources.getBoolean(R.bool.dark_theme)
                if (!darkTheme) {
                    //Has light theme
                    views.setTextColor(R.id.nwError, Color.BLACK)
                } else {
                    //Has dark theme
                    views.setTextColor(R.id.nwError, Color.WHITE)
                }

                views.setViewVisibility(R.id.nwError, View.VISIBLE)
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return views
        }

        private fun getTagView(
            context: Context,
            tag: String,
            textColor: Int,
            backColor: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.tag_widget)
            views.setViewVisibility(R.id.nwTag, View.VISIBLE)
            views.setTextViewText(R.id.nwTag, tag)
            views.setTextColor(R.id.nwTag, textColor)
            views.setInt(R.id.nwTag, "setBackgroundResource", backColor)

            return views
        }

        fun getUpcomingWidget(
            context: Context,
            appWidgetId: Int,
            appWidgetManager: AppWidgetManager
        ) {
            val views = RemoteViews(context.packageName, R.layout.upcoming_widget)
            views.setViewVisibility(R.id.uwError, View.GONE)
            //Color & Background
            val contrastColor: Int

            //Note does not have any color. Apply default one
            val darkTheme = context.resources.getBoolean(R.bool.dark_theme)
            //Tint the widget's background color to the theme's card background color
            val backColor: Int = context.getColor(R.color.background)
            if (!darkTheme) {
                //Has light theme
                contrastColor = Color.BLACK
            } else {
                //Has dark theme
                contrastColor = Color.WHITE
            }

            views.setInt(
                R.id.uwBack,
                "setColorFilter",
                backColor
            )

            //Title
            views.setTextColor(R.id.uwTitle, contrastColor)

            //Items (Reminders)
            val items = ArrayList<Note>()
            runBlocking {
                items.clear()
                items.addAll(NotesDB.getDB(context).noteDao().getNotesWithReminders(System.currentTimeMillis()))
            }

            views.removeAllViews(R.id.uwNotes)

            if(items.isNullOrEmpty()){
                views.setViewVisibility(R.id.uwError, View.VISIBLE)
                views.setTextColor(R.id.uwError, contrastColor)
            }
            else{
                views.setViewVisibility(R.id.uwError, View.GONE)
                for(note in items){
                    views.addView(R.id.uwNotes, getUpcomingItem(context, note))
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getUpcomingItem(context: Context, note: Note?): RemoteViews {
            val showLockPrev: Boolean = PrefUtils.getPref("showLockPrev", false, context) as Boolean
            val views = RemoteViews(context.packageName, R.layout.upcoming_item)
            if (note != null) {
                views.setViewVisibility(R.id.upiInfo, View.VISIBLE)
                views.setViewVisibility(R.id.upiError, View.GONE)
                //Color & Background
                val intent = Intent(context, NoteActivity::class.java)
                intent.putExtra("id", note.id)
                //Add values to intent
                val pending =
                    PendingIntent.getActivity(
                        context,
                        note.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                views.setOnClickPendingIntent(R.id.upiBack, pending)
                val contrastColor: Int
                val backColor: Int
                if (note.color != null) {
                    //Tint the widget's background color to the note's color
                    backColor = Color.parseColor(note.color)
                    if (ColorUtils.isDarkColor(Color.parseColor(note.color))) {
                        contrastColor = Color.WHITE
                    } else {
                        contrastColor = Color.BLACK
                    }
                } else {
                    //Note does not have any color. Apply default one
                    val darkTheme = context.resources.getBoolean(R.bool.dark_theme)
                    //Tint the widget's background color to the theme's card background color
                    backColor = context.getColor(R.color.cardBackground)
                    if (!darkTheme) {
                        //Has light theme
                        contrastColor = Color.BLACK
                    } else {
                        //Has dark theme
                        contrastColor = Color.WHITE
                    }
                }
                views.setInt(
                    R.id.upiBack,
                    "setColorFilter",
                    backColor
                )

                //Name
                views.setTextViewText(R.id.upiName, note.name)
                views.setTextColor(R.id.upiName, contrastColor)

                //Reminder
                views.setTextViewText(R.id.upiDate, TimeUtils.getSimpleDate(note.reminder, context))
                views.setTextColor(R.id.upiDate, contrastColor)

                //Content
                if (note.content.isNotEmpty()) {
                    views.setTextViewText(R.id.upiContent, HtmlUtils.fromHtml(note.content))
                    views.setViewVisibility(R.id.upiContent, View.VISIBLE)
                    views.setTextColor(R.id.upiContent, contrastColor)
                } else {
                    views.setTextViewText(R.id.upiContent, "")
                    views.setViewVisibility(R.id.upiContent, View.GONE)
                }

                //Password
                if (note.password != null) {
                    if (showLockPrev) {
                        views.setViewVisibility(R.id.nwContent, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.nwContent, View.GONE)
                    }
                }
            } else {
                views.setViewVisibility(R.id.nwInfo, View.GONE)
                val darkTheme = context.resources.getBoolean(R.bool.dark_theme)
                if (!darkTheme) {
                    //Has light theme
                    views.setTextColor(R.id.nwError, Color.BLACK)
                } else {
                    //Has dark theme
                    views.setTextColor(R.id.nwError, Color.WHITE)
                }
                views.setViewVisibility(R.id.nwError, View.VISIBLE)
            }
            return views
        }
    }
}
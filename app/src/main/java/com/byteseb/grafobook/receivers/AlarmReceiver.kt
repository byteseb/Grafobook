package com.byteseb.grafobook.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.NoteActivity
import com.byteseb.grafobook.room.NotesDB
import com.byteseb.grafobook.utils.HtmlUtils
import com.byteseb.grafobook.utils.NotificationUtils
import com.byteseb.grafobook.utils.WidgetUtils
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle = intent?.extras

        //Note data
        val name = bundle?.getString("name")
        val id = bundle?.getInt("id", -1)
        val color = bundle?.getString("color")
        val content = bundle?.getString("content")

        val noteIntent = Intent(context, NoteActivity::class.java)
        noteIntent.putExtras(bundle!!)
        noteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending =
            PendingIntent.getActivity(context, id!!, noteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        NotificationUtils.showNotification(
            context = context,
            channelId = NotificationUtils.REMINDER_CHANNEL,
            notificationId = id,
            title = name,
            content = content,
            color = Color.parseColor(color),
            iconResource = R.drawable.ic_round_insert_drive_file_24,
            pendingIntent = pending
        )
        if (context != null) {
            WidgetUtils.refreshWidgets(context)
        }
    }
}
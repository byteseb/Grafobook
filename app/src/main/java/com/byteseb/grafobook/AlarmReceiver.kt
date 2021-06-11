package com.byteseb.grafobook

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.byteseb.grafobook.activities.NoteActivity

class AlarmReceiver : BroadcastReceiver() {

    val CHANNEL_ID = "grafobook.reminderchannel"

    override fun onReceive(context: Context?, intent: Intent?) {

        if(context == null){
            return
        }

        val bundle = intent?.extras

        val name = bundle?.getString("name")
        val id = bundle?.getInt("id", -1)
        val color = bundle?.getString("color")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_round_insert_drive_file_24)
        builder.setContentTitle(name)
        if(color != null){
            builder.color = Color.parseColor(color)
        }
        builder.priority = NotificationCompat.PRIORITY_DEFAULT
        builder.setAutoCancel(true)

        val noteIntent = Intent(context, NoteActivity::class.java)
        noteIntent.putExtras(bundle!!)
        noteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(context, 0, noteIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        builder.setContentIntent(pending)

        val notManager = NotificationManagerCompat.from(context)
        notManager.notify(id!!, builder.build())
    }
}
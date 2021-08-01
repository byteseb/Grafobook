package com.byteseb.grafobook.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.NoteActivity

class NotificationUtils {

    //Channels
    companion object{

        val REMINDER_CHANNEL = "grafobook.reminderchannel"
        val BACKUP_CHANNEL = "grafobook.backupchannel"
        val OTHER_CHANNEL = "grafobook.otherchannel"

        lateinit var notManager: NotificationManager
        lateinit var notChannel: NotificationChannel

        fun createNotChannel(context: Context, channelId: String, channelName: String, importance: Int = NotificationManager.IMPORTANCE_DEFAULT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notChannel = NotificationChannel(
                    channelId,
                    channelName,
                    importance
                )
                notChannel.enableLights(true)
                notChannel.lightColor = ColorUtils.accentColor(context)
                notChannel.enableVibration(true)

                notManager = context.getSystemService(NotificationManager::class.java)
                notManager.createNotificationChannel(notChannel)
            }
        }

        fun createChannels(context: Context){
            createNotChannel(context, REMINDER_CHANNEL, context.getString(R.string.reminder_channel))
            createNotChannel(context, BACKUP_CHANNEL, context.getString(R.string.backup_channel))
            createNotChannel(context, OTHER_CHANNEL, context.getString(R.string.other_channel))
        }

        fun showNotification(
            context: Context?,
            channelId: String,
            notificationId : Int = 0,
            title : String? = null,
            content: String? = null,
            color: Int? = null,
            iconResource : Int = R.drawable.ic_round_insert_drive_file_24,
            pendingIntent: PendingIntent? = null,
            autoCancel : Boolean = true
        ) {

            if (context == null) {
                return
            }

            val builder = NotificationCompat.Builder(context, channelId)

            //Icon
            builder.setSmallIcon(iconResource)
            //Title
            if(title != null){
                builder.setContentTitle(title)
            }
            if (!content.isNullOrBlank()){
                builder.setContentText(content)
            }
            if(color != null){
                builder.color = color
            }
            builder.priority = NotificationCompat.PRIORITY_DEFAULT
            builder.setAutoCancel(autoCancel)

            //PendingIntent
            if(pendingIntent != null){
                builder.setContentIntent(pendingIntent)
            }

            val notManager = NotificationManagerCompat.from(context)
            notManager.notify(notificationId, builder.build())
        }
    }
}
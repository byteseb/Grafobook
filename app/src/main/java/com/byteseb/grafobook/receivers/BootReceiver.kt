package com.byteseb.grafobook.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.byteseb.grafobook.utils.ReminderUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            ReminderUtils.refreshReminders(context)
        }
    }
}
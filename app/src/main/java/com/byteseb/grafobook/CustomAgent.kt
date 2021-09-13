package com.byteseb.grafobook

import android.app.PendingIntent
import android.app.backup.*
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.byteseb.grafobook.activities.SettingsActivity
import com.byteseb.grafobook.utils.ColorUtils
import com.byteseb.grafobook.utils.NotificationUtils
import com.byteseb.grafobook.utils.PrefUtils
import java.io.File

class CustomAgent : BackupAgentHelper() {

    val DB_NAME = "notesDB"

    val DB_BACKUP_KEY = "dbBackup"
    val SHARED_PREFS_KEY = "prefsBackup"

    val BACKUP_NOTIFICATION_ID = 0
    val RESTORE_NOTIFICATION_ID = 1

    override fun onCreate() {
        //Backup note's database file
        val dbHelper = FileBackupHelper(this, DB_NAME)
        addHelper(DB_BACKUP_KEY, dbHelper)

        //Backup SharedPreferences
        val prefHelper = SharedPreferencesBackupHelper(this)
        addHelper(SHARED_PREFS_KEY, prefHelper)
        super.onCreate()
    }

//    override fun onFullBackup(data: FullBackupDataOutput?) {
//        super.onFullBackup(data)
//        PrefUtils.setPref("lastCloudBackup", System.currentTimeMillis(), this)
//        showNotification(
//            channelId = NotificationUtils.BACKUP_CHANNEL,
//            notId = BACKUP_NOTIFICATION_ID,
//            title = getString(R.string.not_backup_title),
//            content = null,
//            color = ColorUtils.accentColor(this),
//            iconResource = R.drawable.ic_round_settings_backup_restore_24
//        )
//    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        super.onBackup(oldState, data, newState)
        PrefUtils.setPref("lastCloudBackup", System.currentTimeMillis(), this)
        showNotification(
            channelId = NotificationUtils.BACKUP_CHANNEL,
            notId = BACKUP_NOTIFICATION_ID,
            title = getString(R.string.not_backup_title),
            content = null,
            color = ColorUtils.accentColor(this),
            iconResource = R.drawable.ic_round_settings_backup_restore_24
        )
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        showNotification(
            channelId = NotificationUtils.BACKUP_CHANNEL,
            notId = RESTORE_NOTIFICATION_ID,
            title = getString(R.string.not_restore_title),
            content = null,
            color = ColorUtils.accentColor(this),
            iconResource = R.drawable.ic_round_settings_backup_restore_24
        )
    }

    override fun onQuotaExceeded(backupDataBytes: Long, quotaBytes: Long) {
        super.onQuotaExceeded(backupDataBytes, quotaBytes)
        showNotification(
            channelId = NotificationUtils.BACKUP_CHANNEL,
            notId = BACKUP_NOTIFICATION_ID,
            title = getString(R.string.not_exceeded_title),
            content = getString(R.string.not_exceeded_desc),
            color = ColorUtils.accentColor(this),
            iconResource = R.drawable.ic_round_error_24
        )
    }

    override fun getFilesDir(): File = getDatabasePath(DB_NAME).parentFile

    private fun showNotification(
        channelId: String,
        notId: Int,
        title: String,
        content: String? = null,
        color: Int,
        iconResource: Int) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending =
                PendingIntent.getActivity(
                    this,
                    notId,
                    settingsIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            NotificationUtils.showNotification(
                context = this,
                channelId = channelId,
                notificationId = notId,
                title = title,
                content = content,
                color = color,
                iconResource = iconResource,
                pendingIntent = pending
            )
        }
        handler.post(runnable)
    }
}
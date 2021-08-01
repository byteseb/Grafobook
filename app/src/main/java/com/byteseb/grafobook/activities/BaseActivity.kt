package com.byteseb.grafobook.activities

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.byteseb.grafobook.receivers.AlarmReceiver
import com.byteseb.grafobook.R
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.utils.PrefUtils

open class BaseActivity : AppCompatActivity() {

    enum class Colors {
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    fun setCurrentTheme() {
        val defColor = PrefUtils.getPref("defaultColor", Colors.ORANGE.ordinal, this)
        val defBack = PrefUtils.getPref("nightTheme", false, this) as Boolean

        when (defColor) {
            Colors.RED.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Red)
            }
            Colors.BLUE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Blue)
            }
            Colors.GREEN.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Green)
            }
            Colors.ORANGE.ordinal -> {
                setTheme(R.style.Theme_Grafobook)
            }
            Colors.YELLOW.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Yellow)
            }
            Colors.PINK.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Pink)
            }
            Colors.PURPLE.ordinal -> {
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
}
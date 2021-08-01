package com.byteseb.grafobook.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PrefUtils {
    companion object {
        fun getPref(
            key: String,
            defaultValue: Any,
            context: Context,
            preferences: String? = null
        ): Any? {

            val prefs: SharedPreferences
            if (preferences == null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(context)
            } else {
                prefs = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
            }
            //If does not specify any preference file, use the default preferences
            if (defaultValue is String) {
                return prefs.getString(key, defaultValue)
            } else if (defaultValue is Int) {
                return prefs.getInt(key, defaultValue)
            } else if (defaultValue is Boolean) {
                return prefs.getBoolean(key, defaultValue)
            } else if (defaultValue is Float) {
                return prefs.getFloat(key, defaultValue)
            } else if (defaultValue is Long) {
                return prefs.getLong(key, defaultValue)
            } else if (defaultValue is MutableSet<*>) {
                return prefs.getStringSet(key, defaultValue as MutableSet<String>)
            }
            return null
        }

        fun setPref(key: String, value: Any, context: Context, preferences: String? = null) {

            val prefs: SharedPreferences
            if (preferences == null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(context)
            } else {
                prefs = context.getSharedPreferences(preferences, Context.MODE_PRIVATE)
            }
            //If does not specify any preference file, use the default preferences
            if (value is String) {
                prefs.edit().putString(key, value).apply()
            } else if (value is Int) {
                prefs.edit().putInt(key, value).apply()
            } else if (value is Boolean) {
                prefs.edit().putBoolean(key, value).apply()
            } else if (value is Float) {
                prefs.edit().putFloat(key, value).apply()
            } else if (value is Long) {
                prefs.edit().putLong(key, value).apply()
            } else if (value is MutableSet<*>) {
                prefs.edit().putStringSet(key, value as MutableSet<String>).apply()
            }
        }
    }
}
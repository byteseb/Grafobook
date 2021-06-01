package com.byteseb.grafobook.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.byteseb.grafobook.BuildConfig
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.MainActivity

class SettingsFragment : PreferenceFragmentCompat() {


    enum class Colors{
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val prefs = preferenceManager.sharedPreferences

        //Load data
        val color = prefs.getInt("defaultColor", Colors.ORANGE.ordinal)

        val colorPref = findPreference<ListPreference>("accentColor")
        val backPref = findPreference<SwitchPreference>("backgroundColor")
        val versionPref = findPreference<Preference>("version")

        //Makes switch disabled if version if higher than or equal to Q

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            backPref?.isEnabled = true
            backPref?.summary = ""
        }
        else{
            backPref?.isEnabled = false
            backPref?.summary = getString(R.string.dark_theme_disabled)
        }
        backPref?.isChecked = resources.getBoolean(R.bool.dark_theme)

        colorPref?.setValueIndex(color)

        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        colorPref?.setOnPreferenceChangeListener { preference, newValue ->
            if(newValue is String && newValue != colorPref.value){
                prefs.edit().putInt("defaultColor", newValue.toInt()).apply()
                startActivity(intent)
            }
            true
        }
        
        backPref?.setOnPreferenceChangeListener { preference, newValue ->
            if(newValue is Boolean && newValue != backPref.isChecked){
                prefs.edit().putBoolean("nightTheme", newValue).apply()
                startActivity(intent)
            }
            true
        }

        versionPref?.summary = BuildConfig.VERSION_NAME
    }
}
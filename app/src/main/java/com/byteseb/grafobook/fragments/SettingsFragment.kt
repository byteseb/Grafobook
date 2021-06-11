package com.byteseb.grafobook.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.byteseb.grafobook.BuildConfig
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.MainActivity
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_import.*
import java.io.File
import java.io.FileWriter
import java.lang.Exception

class SettingsFragment : PreferenceFragmentCompat() {

    var allNotes: ArrayList<Note>? = null

    fun setNotes(notes: ArrayList<Note>){
        allNotes = notes
    }

    enum class Colors {
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val gson = Gson()

        //On directory picked
        val previewRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    val uri = it.data?.data
                    try {
                        val output = requireContext().contentResolver.openOutputStream(uri!!)
                        val data = gson.toJson(allNotes)
                        output?.write(data.toByteArray())
                        output?.flush()
                        output?.close()
                    } catch (ex: Exception) {
                        Toast.makeText(requireContext(), getString(R.string.failed_create), Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val prefs = preferenceManager.sharedPreferences

        //Load data
        val color = prefs.getInt("defaultColor", Colors.ORANGE.ordinal)
        val autoSave = prefs.getBoolean("autoSave", true)

        //Theming
        val colorPref = findPreference<ListPreference>("accentColor")
        val backPref = findPreference<SwitchPreference>("backgroundColor")
        //Backup
        val backupPref = findPreference<Preference>("getBackup")
        //Preferences
        val savePref = findPreference<SwitchPreference>("autoSave")
        //About
        val versionPref = findPreference<Preference>("version")

        //Makes switch disabled if version if higher than or equal to Q

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            backPref?.isEnabled = true
            backPref?.summary = ""
        } else {
            backPref?.isEnabled = false
            backPref?.summary = getString(R.string.dark_theme_disabled)
        }
        backPref?.isChecked = resources.getBoolean(R.bool.dark_theme)
        savePref?.isChecked = autoSave
        colorPref?.setValueIndex(color)

        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        colorPref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is String && newValue != colorPref.value) {
                prefs.edit().putInt("defaultColor", newValue.toInt()).apply()
                startActivity(intent)
            }
            true
        }

        backPref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != backPref.isChecked) {
                prefs.edit().putBoolean("nightTheme", newValue).apply()
                startActivity(intent)
            }
            true
        }

        savePref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != savePref.isChecked) {
                prefs.edit().putBoolean("autoSave", newValue).apply()
            }
            true
        }

        backupPref?.setOnPreferenceClickListener {
            val pickIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/grafobook"
                putExtra(Intent.EXTRA_TITLE, getString(R.string.backup_file_name))
                putExtra(Intent.EXTRA_MIME_TYPES, "application/grafobook")
            }
            previewRequest.launch(pickIntent)
            //Opening picker

            true
        }

        versionPref?.summary = BuildConfig.VERSION_NAME
    }
}
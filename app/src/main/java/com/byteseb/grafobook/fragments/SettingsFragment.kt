package com.byteseb.grafobook.fragments

import android.app.backup.BackupManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.byteseb.grafobook.BuildConfig
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.MainActivity
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.utils.PrefUtils
import com.byteseb.grafobook.utils.TimeUtils
import com.google.gson.Gson


class SettingsFragment : PreferenceFragmentCompat() {

    var allNotes: ArrayList<Note>? = null

    fun setNotes(notes: ArrayList<Note>) {
        allNotes = notes
    }

    enum class Colors {
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val prefs = preferenceManager.sharedPreferences

        //MainActivity restart intent
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        //THEMING

        //Color
        val color = prefs.getInt("defaultColor", Colors.ORANGE.ordinal)
        val colorPref = findPreference<ListPreference>("accentColor")
        colorPref?.setValueIndex(color)
        colorPref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is String && newValue != colorPref.value) {
                prefs.edit().putInt("defaultColor", newValue.toInt()).apply()
                BackupManager.dataChanged(requireContext().packageName)
                startActivity(intent)
            }
            true
        }

        //Theme (Dark theme)
        val themePref = findPreference<SwitchPreference>("backgroundColor")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //Makes switch disabled if version if higher than or equal to Q
            themePref?.isEnabled = true
            themePref?.summary = ""
        } else {
            themePref?.isEnabled = false
            themePref?.summary = getString(R.string.dark_theme_disabled)
        }
        themePref?.isChecked = resources.getBoolean(R.bool.dark_theme)
        themePref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != themePref.isChecked) {
                prefs.edit().putBoolean("nightTheme", newValue).apply()
                BackupManager.dataChanged(requireContext().packageName)
                startActivity(intent)
            }
            true
        }


        //EDITOR

        //Auto save notes
        val autoSave = prefs.getBoolean("autoSave", true)
        val savePref = findPreference<SwitchPreference>("autoSave")
        savePref?.isChecked = autoSave
        savePref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != savePref.isChecked) {
                prefs.edit().putBoolean("autoSave", newValue).apply()
                BackupManager.dataChanged(requireContext().packageName)
            }
            true
        }

        //Show back button
        val showBackPref = findPreference<SwitchPreference>("showBack")
        showBackPref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != showBackPref.isChecked) {
                prefs.edit().putBoolean("showBack", newValue).apply()
                BackupManager.dataChanged(requireContext().packageName)
            }
            true
        }


        //PRIVACY

        //Show locked notes preview
        val showLockPrev = prefs.getBoolean("showLockPrev", false)
        val lockPrevPref = findPreference<SwitchPreference>("showLockPrev")
        lockPrevPref?.isChecked = showLockPrev
        lockPrevPref?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue != lockPrevPref.isChecked) {
                prefs.edit().putBoolean("showLockPrev", newValue).apply()
                BackupManager.dataChanged(requireContext().packageName)
                startActivity(intent)
            }
            true
        }


        //BACKUP

        //Auto cloud backup
        val lastCloudBackup = PrefUtils.getPref("lastCloudBackup", -1L, requireContext())
        val autoCloudPref = findPreference<Preference>("manageAutoCloudBackups")

        val autoCloudSummary: String
        if (lastCloudBackup != -1L) {
            //If has a previous backup
            autoCloudSummary =
                getString(R.string.auto_cloud_backup_settings_desc) + "\n\n" + getString(
                    R.string.last_cloud_backup, TimeUtils.getSimpleDate(
                        lastCloudBackup as Long, requireContext()
                    )
                )
        } else {
            //If does not have a backup
            autoCloudSummary = getString(R.string.auto_cloud_backup_settings_desc)
        }
        autoCloudPref?.summary = autoCloudSummary

        autoCloudPref?.setOnPreferenceClickListener {
            try {
                val backupIntent = Intent()
                backupIntent.component = ComponentName(
                    "com.google.android.gms",
                    "com.google.android.gms.backup.component.BackupSettingsActivity"
                )
                startActivity(backupIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }

        //Local backup
        val lastLocalBackup = PrefUtils.getPref("lastLocalBackup", -1L, requireContext())
        val backupPref = findPreference<Preference>("getBackup")
        val gson = Gson()
        //On directory picked (To save local backup)
        val localBackupSummary: String
        if (lastLocalBackup != -1L) {
            //If has a previous backup
            localBackupSummary = getString(R.string.generate_backup_desc) + "\n\n" + getString(
                R.string.last_local_backup, TimeUtils.getSimpleDate(
                    lastLocalBackup as Long, requireContext()
                )
            )
        } else {
            //If does not have a backup
            localBackupSummary = getString(R.string.generate_backup_desc)
        }
        backupPref?.summary = localBackupSummary

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
                        PrefUtils.setPref(
                            "lastLocalBackup",
                            System.currentTimeMillis(),
                            requireContext()
                        )
                    } catch (ex: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.failed_create),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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

        //ABOUT
        val versionPref = findPreference<Preference>("version")
        versionPref?.summary = BuildConfig.VERSION_NAME
    }
}
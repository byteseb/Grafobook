package com.byteseb.grafobook.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.fragments.SettingsFragment
import com.byteseb.grafobook.room.NoteViewModel
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCurrentTheme()
        setContentView(R.layout.activity_settings)

        val viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)
        val fragment = SettingsFragment()
        supportFragmentManager.beginTransaction().replace(R.id.settingsHolder, fragment).commit()
        viewModel.allNotes.observe(this){
            fragment.setNotes(ArrayList(it))
        }
    }
}
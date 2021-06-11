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

class SettingsActivity : AppCompatActivity() {

    enum class Colors{
        RED, PINK, PURPLE, BLUE, GREEN, YELLOW, ORANGE
    }

    fun setCurrentTheme(){

        val defColor = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getInt("defaultColor", MainActivity.Colors.ORANGE.ordinal)!!
        val defBack = PreferenceManager.getDefaultSharedPreferences(this)
            ?.getBoolean("nightTheme", false)!!

        when(defColor){
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
            Colors.PINK.ordinal-> {
                setTheme(R.style.Theme_Grafobook_Pink)
            }
            Colors.PURPLE.ordinal -> {
                setTheme(R.style.Theme_Grafobook_Purple)
            }
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            //If it is a version lower than Q, set the theme manually
            if(!defBack){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

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
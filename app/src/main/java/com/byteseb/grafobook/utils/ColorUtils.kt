package com.byteseb.grafobook.utils

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

class ColorUtils {

    companion object {
        val darkTolerance = 0.25f

        fun isDarkColor(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < darkTolerance
        }

        fun tintImgButton(view: ImageButton, color: Int?) {

            if (color == null) {
                return
            }

            view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }

        fun tintBox(view: CheckBox, color: Int?) {

            if (color == null) {
                return
            }

            view.buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(-R.attr.state_enabled),  // Disabled
                    intArrayOf(R.attr.state_enabled)    // Enabled
                ),
                intArrayOf(
                    color,     // The color for the Disabled state
                    color        // The color for the Enabled state
                )
            )
        }

        fun tintImg(view: ImageView, color: Int?) {

            if (color == null) {
                return
            }

            view.setColorFilter(color)
        }

        fun tintText(view: TextView, color: Int?) {

            if (color == null) {
                return
            }

            view.setTextColor(color)
        }

        fun darkenColor(color: Int, factor: Float = 0.6f): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(
                a,
                r.coerceAtMost(255),
                g.coerceAtMost(255),
                b.coerceAtMost(255)
            )
        }

        fun accentColor(context: Context): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorAccent, value, true)
            return ContextCompat.getColor(context, value.resourceId)
        }
    }
}
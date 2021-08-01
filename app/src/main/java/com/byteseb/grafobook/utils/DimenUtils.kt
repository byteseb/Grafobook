package com.byteseb.grafobook.utils

import android.content.res.Resources
import android.util.TypedValue

class DimenUtils {
    fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
    fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}
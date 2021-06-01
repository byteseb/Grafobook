package com.byteseb.grafobook.listeners

import android.os.SystemClock
import android.view.View

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClick = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClick < 1000) {
            return
        }
        lastClick = SystemClock.elapsedRealtime()
        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}
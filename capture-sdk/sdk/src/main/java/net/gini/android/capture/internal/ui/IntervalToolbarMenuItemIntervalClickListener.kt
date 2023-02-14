package net.gini.android.capture.internal.ui

import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class IntervalToolbarMenuItemIntervalClickListener(private val itemListener: Toolbar.OnMenuItemClickListener?): Toolbar.OnMenuItemClickListener,
    IntervalClickListenerHelper() {

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (isEnabled) {
            isEnabled = false
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(enabled, interval)
            itemListener?.onMenuItemClick(item)
        }
        return true
    }

    override var enabled: Runnable = Runnable {
        isEnabled = true
    }
}
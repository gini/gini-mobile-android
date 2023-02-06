package net.gini.android.capture.internal.ui

import android.view.View


class IntervalClickListener(
    private val click: View.OnClickListener
) : View.OnClickListener {

    private val interval = 500L

    override fun onClick(view: View) {
        if (isEnabled) {
            isEnabled = false
            view.postDelayed(enable, interval)
            click.onClick(view)
        }
    }

    var isEnabled = true
    private val enable =
        Runnable { isEnabled = true }
}
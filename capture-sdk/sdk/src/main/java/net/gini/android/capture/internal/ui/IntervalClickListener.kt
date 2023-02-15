package net.gini.android.capture.internal.ui

import android.view.View


class IntervalClickListener(
    private val click: View.OnClickListener?
) : View.OnClickListener, IntervalClickListenerHelper() {

    override var enabled: Runnable =
        Runnable { isEnabled = true }

    override fun onClick(view: View) {
        if (isEnabled) {
            isEnabled = false
            view.postDelayed(enabled, interval)
            click?.onClick(view)
        }
    }
}
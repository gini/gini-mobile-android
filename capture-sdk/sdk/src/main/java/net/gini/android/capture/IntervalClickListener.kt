package net.gini.android.capture

import android.view.View


class IntervalClickListener(
    private val click: ((View) -> Unit)
) : View.OnClickListener {

    private val interval = 500L

    override fun onClick(view: View) {
        if (isEnabled) {
            isEnabled = false
            view.postDelayed(ENABLE, interval)
            click(view)
        }
    }

    companion object {
        @JvmStatic
        var isEnabled = true
        private val ENABLE =
            Runnable { isEnabled = true }
    }
}
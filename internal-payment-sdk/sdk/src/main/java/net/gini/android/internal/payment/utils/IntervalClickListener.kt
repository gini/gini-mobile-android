package net.gini.android.internal.payment.utils

import android.view.View


/**
 * Internal use only.
 *
 * Creates a enough delay between clicks to prevent double taps
 *
 * @param click
 * @suppress
 */
class IntervalClickListener(
    private val click: View.OnClickListener?
) : View.OnClickListener, IntervalClickListenerHelper() {

    override var enabled: Runnable =
        Runnable { isEnabled = true }

    override fun onClick(view: View?) {
        if (view == null) return;

        if (isEnabled) {
            isEnabled = false
            view.postDelayed(enabled, interval)
            click?.onClick(view)
        }
    }
}
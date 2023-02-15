package net.gini.android.capture.internal.ui

import android.view.View
import net.gini.android.capture.internal.ui.IntervalClickListener

/**
 * Internal use only.
 *
 * @suppress
 */
fun View.setIntervalClickListener(click: View.OnClickListener?) {
    setOnClickListener(IntervalClickListener(click))
}
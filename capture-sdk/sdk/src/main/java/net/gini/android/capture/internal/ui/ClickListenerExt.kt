package net.gini.android.capture.internal.ui

import android.view.View

fun View.setIntervalClickListener(click: View.OnClickListener?) {
    setOnClickListener(IntervalClickListener(click))
}
package net.gini.android.capture

import android.view.View

fun View.onClick(click: (View) -> Unit) {
    setOnClickListener(IntervalClickListener(click))
}
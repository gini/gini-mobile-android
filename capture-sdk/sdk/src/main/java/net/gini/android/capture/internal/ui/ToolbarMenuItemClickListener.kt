package net.gini.android.capture.internal.ui

import androidx.appcompat.widget.Toolbar

fun Toolbar.setOnMenuItemIntervalClickListener(toolbarMenuItemClickListener: Toolbar.OnMenuItemClickListener) {
    setOnMenuItemClickListener(IntervalToolbarMenuItemIntervalClickListener(toolbarMenuItemClickListener))
}
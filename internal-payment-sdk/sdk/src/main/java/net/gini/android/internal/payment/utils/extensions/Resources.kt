package net.gini.android.internal.payment.utils.extensions

import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.Resources.getSystem

fun Resources.isLandscapeOrientation(): Boolean = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun Resources.getWidthPixels(): Int = getSystem().displayMetrics.widthPixels

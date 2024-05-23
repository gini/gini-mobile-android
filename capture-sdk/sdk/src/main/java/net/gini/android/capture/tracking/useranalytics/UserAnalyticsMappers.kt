package net.gini.android.capture.tracking.useranalytics

fun Boolean.mapToAnalyticsValue() = if (this) "yes" else "no"
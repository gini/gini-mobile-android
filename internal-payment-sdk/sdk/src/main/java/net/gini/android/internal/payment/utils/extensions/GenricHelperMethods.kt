package net.gini.android.internal.payment.utils.extensions

fun BooleanArray.atLeastOneIsTrue(): Boolean = any { it }
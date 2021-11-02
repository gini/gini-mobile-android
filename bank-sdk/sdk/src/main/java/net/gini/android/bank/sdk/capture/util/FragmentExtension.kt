package net.gini.android.bank.sdk.capture.util

import androidx.fragment.app.Fragment

internal fun Fragment.parentFragmentManagerOrNull() = if (isAdded) { parentFragmentManager } else { null }
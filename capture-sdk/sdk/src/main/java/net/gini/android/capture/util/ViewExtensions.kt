package net.gini.android.capture.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * [protectViewFromInsets] adds safe padding to the view so it doesn't clash with the phone’s
 * status bar or navigation bar. This helps make sure content isn’t hidden under system UI
 * like the top or bottom bars. It also adds a small extra space (3dp) at the
 * top for better visual breathing room.
 */

fun View.protectViewFromInsets() {
    val safeTopPaddingDp = 3
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val safeThreshold = (safeTopPaddingDp * view.resources.displayMetrics.density).toInt()
        view.setPadding(
            view.paddingLeft,
            systemBarsInsets.top + safeThreshold,
            view.paddingRight,
            systemBarsInsets.bottom
        )

        WindowInsetsCompat.CONSUMED
    }

    ViewCompat.requestApplyInsets(this)
}

package net.gini.android.capture.internal.util

import android.view.Window
import android.view.WindowManager

/**
 * Disables screenshots for the given [Window] by setting the [android.view.WindowManager.LayoutParams.FLAG_SECURE]
 * flag.
 */
internal fun Window.disallowScreenshots() {
    setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
}

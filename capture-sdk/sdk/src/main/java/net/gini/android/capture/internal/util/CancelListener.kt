package net.gini.android.capture.internal.util

/**
 * Internal use only.
 */
fun interface CancelListener {

    /**
     * Used by screens to signal the cancellation of the [Capture] flow
     *
     */
    fun onCancelFlow()
}
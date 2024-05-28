package net.gini.android.capture.util

/**
 * Internal use only.
 */
interface CancelListener {

    /**
     * Used by screens to signal the cancellation of the [Capture] flow
     *
     */
    fun onCancelFlow()
}
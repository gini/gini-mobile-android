package net.gini.android.capture.error

import net.gini.android.capture.GiniCaptureError

/**
 * Internal use only.
 *
 * Interface used by the {@link ErrorFragmentCompat} to dispatch events to the hosting activity.
 */
interface ErrorFragmentListener {

    /**
     * Called when an error occurred.
     *
     * @param error details about what went wrong
     */
    fun onError(error: GiniCaptureError)
}

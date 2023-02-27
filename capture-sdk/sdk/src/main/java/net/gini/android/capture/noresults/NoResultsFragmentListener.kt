package net.gini.android.capture.noresults

import net.gini.android.capture.GiniCaptureError

/**
 * Internal use only.
 *
 * Interface used by the {@link NoResultsFragmentCompat} to dispatch events to the hosting activity.
 */
interface NoResultsFragmentListener {

    /**
     * Called when an error occurred.
     *
     * @param error details about what went wrong
     */
    fun onError(error: GiniCaptureError)
}

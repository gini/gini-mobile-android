package net.gini.android.capture.logging

/**
 * Implement this interface if you would like to log Gini Capture SDK errors.
 */
interface ErrorLoggerListener {
    /**
     * Called when an error occurred inside the Gini Capture SDK.
     *
     * @param errorLog error details and metadata for logging
     */
    fun handleErrorLog(errorLog: ErrorLog) { }
}
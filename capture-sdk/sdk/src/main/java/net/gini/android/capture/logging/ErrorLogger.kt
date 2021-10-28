package net.gini.android.capture.logging

import net.gini.android.capture.GiniCapture

/**
 * Internal use only.
 *
 * @suppress
 */
internal class ErrorLogger(
    private val isGiniLoggingOn: Boolean,
    private val giniErrorLogger: ErrorLoggerListener?,
    private val customErrorLogger: ErrorLoggerListener?
) : ErrorLoggerListener {

    override fun handleErrorLog(errorLog: ErrorLog) {
        if (isGiniLoggingOn) {
            giniErrorLogger?.handleErrorLog(errorLog)
        }
        customErrorLogger?.handleErrorLog(errorLog)
    }

    companion object {
        @JvmStatic
        fun log(errorLog: ErrorLog) {
            if (GiniCapture.hasInstance()) {
                GiniCapture.getInstance().internal().errorLogger.handleErrorLog(errorLog);
            }
        }
    }
}
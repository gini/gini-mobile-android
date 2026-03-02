package net.gini.android.health.sdk.exampleapp.util

import android.content.Context
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.internal.payment.GiniHealthException

/**
 * Utility class for handling GiniHealthException errors with user-friendly messages.
 *
 * This handler maps error codes from Gini Health API v5.0 to localized error messages.
 * It supports all documented error codes from the API specification.
 */
object GiniHealthErrorHandler {

    /**
     * Converts a GiniHealthException to a user-friendly error message.
     *
     * @param exception The GiniHealthException to handle
     * @param context Context for accessing string resources
     * @return A user-friendly error message
     */
    fun getUserFriendlyMessage(exception: GiniHealthException, context: Context): String {
        val errorCode = exception.errorItems?.firstOrNull()?.code ?: "N/A"
        val errorMessage = exception.message ?: context.getString(R.string.error_unknown)
        val statusCode = exception.statusCode?.toString() ?: "N/A"
        return context.getString(R.string.error_full_message, errorCode, errorMessage, statusCode)
    }

    /**
     * Determines if an error is retryable based on the error code or status.
     *
     * @param exception The GiniHealthException to check
     * @return true if the error is retryable, false otherwise
     */
    fun isRetryable(exception: GiniHealthException): Boolean {
        // Check error codes that are retryable (first item)
        exception.errorItems?.firstOrNull()?.code?.let { code ->
            return when (code) {
                "2400", // Too many requests
                "2600", // Third party service exception
                "2800", // Service unavailable
                "3300"  // Request timed out
                -> true
                else -> false
            }
        }

        // Check status codes that are retryable
        exception.statusCode?.let { status ->
            return when (status) {
                408, // Request Timeout
                429, // Too Many Requests
                500, // Internal Server Error
                503  // Service Unavailable
                -> true
                else -> false
            }
        }

        return false
    }
}


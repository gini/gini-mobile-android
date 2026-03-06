package net.gini.android.health.sdk.exampleapp.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.internal.payment.GiniHealthException

/**
 * Extension function to show an error dialog for any Throwable.
 * If it's a GiniHealthException, shows detailed error information including error codes,
 * request IDs, and structured error messages.
 *
 * Distinguishes between:
 * - API errors (cause is ApiException) → Uses structured errorResponse
 * - Non-API errors (cause is NOT ApiException) → Uses exception message directly
 *
 * @param exception The Throwable to display
 * @param onDismiss Optional callback when dialog is dismissed
 */
fun Context.showGiniHealthErrorDialog(
    exception: Throwable,
    onDismiss: (() -> Unit)? = null
) {
    // Determine error message based on exception type
    val errorMessage = when (exception) {
        is GiniHealthException -> {
            // Smart cast: exception is automatically GiniHealthException here
            val message = GiniHealthErrorHandler.getUserFriendlyMessage(exception, this)

            // Add request ID if available
            if (exception.requestId != null) {
                "$message\n\n${getString(R.string.error_request_id, exception.requestId)}"
            } else {
                message
            }
        }
        else -> {
            // Handle non-GiniHealthException throwables
            exception.message ?: getString(R.string.error_unknown)
        }
    }

    // Show dialog with single OK/Dismiss button
    AlertDialog.Builder(this)
        .setTitle(R.string.error_dialog_title)
        .setMessage(errorMessage)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onDismiss?.invoke()
        }
        .setOnDismissListener {
            onDismiss?.invoke()
        }
        .show()
}


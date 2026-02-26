package net.gini.android.health.sdk.exampleapp.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import net.gini.android.health.sdk.exampleapp.R
import net.gini.android.internal.payment.GiniHealthException

/**
 * Extension function to show an error dialog for any Throwable.
 * If it's a GiniHealthException, shows detailed error information.
 *
 * @param exception The Throwable to display
 * @param onRetry Optional callback for retry action (only shown if error is retryable)
 * @param onDismiss Optional callback when dialog is dismissed
 */
fun Context.showGiniHealthErrorDialog(
    exception: Throwable,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    // Check if it's a GiniHealthException to show detailed error info
    val giniException = exception as? GiniHealthException

    // Get user-friendly error message
    val errorMessage = if (giniException != null) {
        GiniHealthErrorHandler.getUserFriendlyMessage(giniException, this)
    } else {
        exception.message ?: getString(R.string.error_unknown)
    }

    // Build detailed message with request ID if available
    val detailedMessage = if (giniException?.requestId != null) {
        "$errorMessage\n\n${getString(R.string.error_request_id, giniException.requestId)}"
    } else {
        errorMessage
    }

    val dialogBuilder = AlertDialog.Builder(this)
        .setTitle(R.string.error_dialog_title)
        .setMessage(detailedMessage)

    // Show retry button if error is retryable and callback is provided
    val isRetryable = giniException?.let { GiniHealthErrorHandler.isRetryable(it) } ?: false
    if (isRetryable && onRetry != null) {
        dialogBuilder
            .setPositiveButton(R.string.error_dialog_retry) { _, _ ->
                onRetry()
            }
            .setNegativeButton(R.string.error_dialog_cancel) { _, _ ->
                onDismiss?.invoke()
            }
    } else {
        dialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
            onDismiss?.invoke()
        }
    }

    dialogBuilder.setOnDismissListener {
        onDismiss?.invoke()
    }

    dialogBuilder.show()
}

/**
 * Simplified error dialog without retry option.
 */
fun Context.showGiniHealthErrorDialog(
    exception: Throwable,
    onDismiss: (() -> Unit)? = null
) {
    showGiniHealthErrorDialog(exception, onRetry = null, onDismiss = onDismiss)
}


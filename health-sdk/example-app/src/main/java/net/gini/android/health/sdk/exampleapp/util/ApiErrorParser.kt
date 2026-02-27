package net.gini.android.health.sdk.exampleapp.util


import net.gini.android.health.sdk.exampleapp.invoices.data.ErrorDetail

/**
 * Utility for parsing API error responses and extracting error information.
 */
object ApiErrorParser {
    /**
     * Creates a detailed error message from ErrorDetail.
     * Includes error code, status code, and request ID if available.
     *
     * @param errorDetail The error detail to format
     * @return Formatted error message
     */
    fun formatErrorMessage(errorDetail: ErrorDetail): String {

        return buildString {
            append(errorDetail.message)

            errorDetail.let { response ->
                response.errorResponse?.items?.firstOrNull()?.let { item ->
                    append("\nError Code: ${item.code}")
                    item.message?.let { msg ->
                        if (msg.isNotBlank() && msg != errorDetail.message) {
                            append("\nDetails: $msg")
                        }
                    }
                }
                append("\nRequest ID: ${response.requestId}")
            }
        }
    }

}


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
    // in getUserFriendlyMessage function I want to show error code and error messsage and status code all of them without any change fix it
    fun getUserFriendlyMessage(exception: GiniHealthException, context: Context): String {
        val errorCode = exception.errorItems?.firstOrNull()?.code ?: "N/A"
        val errorMessage = exception.message ?: context.getString(R.string.error_unknown)
        val statusCode = exception.statusCode?.toString() ?: "N/A"
        return context.getString(R.string.error_full_message, errorCode, errorMessage, statusCode)
    }

    /**
     * Gets a user-friendly message for a specific error code.
     *
     * @param errorCode The error code from the API
     * @param context Context for accessing string resources
     * @return A user-friendly error message, or null if no specific message exists
     */
    fun getMessageForErrorCode(errorCode: String, context: Context): String? {
        return when (errorCode) {
            // Generic errors (1xxx)
            "1000" -> context.getString(R.string.error_1000_unknown)
            "1100" -> context.getString(R.string.error_1100_processing_failed)

            // Field validation errors (2xxx)
            "2000" -> context.getString(R.string.error_2000_validation)
            "2001" -> context.getString(R.string.error_2001_purpose_missing)
            "2002" -> context.getString(R.string.error_2002_purpose_length)
            "2003" -> context.getString(R.string.error_2003_recipient_missing)
            "2004" -> context.getString(R.string.error_2004_recipient_length)
            "2005" -> context.getString(R.string.error_2005_iban_missing)
            "2006" -> context.getString(R.string.error_2006_iban_length)
            "2007" -> context.getString(R.string.error_2007_iban_invalid)
            "2008" -> context.getString(R.string.error_2008_amount_missing)
            "2009" -> context.getString(R.string.error_2009_amount_length)
            "2010" -> context.getString(R.string.error_2010_amount_format)
            "2011" -> context.getString(R.string.error_2011_bic_length)
            "2012" -> context.getString(R.string.error_2012_bic_invalid)
            "2013" -> context.getString(R.string.error_2013_unauthorized_documents)
            "2014" -> context.getString(R.string.error_2014_documents_not_found)
            "2015" -> context.getString(R.string.error_2015_composite_docs_missing)
            "2016" -> context.getString(R.string.error_2016_unauthorized_payment_requests)
            "2017" -> context.getString(R.string.error_2017_payment_requests_not_found)
            "2100" -> context.getString(R.string.error_2100_entity_validation_failed)

            // Document and resource errors (22xx-25xx)
            "2201" -> context.getString(R.string.error_2201_layout_not_found)
            "2202" -> context.getString(R.string.error_2202_page_not_found)
            "2203" -> context.getString(R.string.error_2203_resource_load_failed)
            "2204" -> context.getString(R.string.error_2204_feedback_failed)
            "2205" -> context.getString(R.string.error_2205_feedback_invalid)
            "2206" -> context.getString(R.string.error_2206_feedback_unsupported_media)
            "2300" -> context.getString(R.string.error_2300_media_type_exception)
            "2301" -> context.getString(R.string.error_2301_media_type_not_supported)
            "2302" -> context.getString(R.string.error_2302_media_type_not_acceptable)
            "2400" -> context.getString(R.string.error_2400_too_many_requests)
            "2500" -> context.getString(R.string.error_2500_resource_not_found)
            "2501" -> context.getString(R.string.error_2501_document_not_found)
            "2502" -> context.getString(R.string.error_2502_payment_provider_icon_not_found)
            "2503" -> context.getString(R.string.error_2503_payment_request_not_found)
            "2504" -> context.getString(R.string.error_2504_payment_not_found)
            "2505" -> context.getString(R.string.error_2505_source_document_not_found)
            "2506" -> context.getString(R.string.error_2506_cover_page_not_found)
            "2507" -> context.getString(R.string.error_2507_extractions_not_found)
            "2508" -> context.getString(R.string.error_2508_payment_provider_not_found)

            // Service errors (26xx-29xx)
            "2600" -> context.getString(R.string.error_2600_third_party_service)
            "2700" -> context.getString(R.string.error_2700_type_mismatch)
            "2800" -> context.getString(R.string.error_2800_service_unavailable)
            "2900" -> context.getString(R.string.error_2900_unauthorized_operation)
            "2901" -> context.getString(R.string.error_2901_unauthorized_deletion)

            // Request errors (3xxx-4xxx)
            "3000" -> context.getString(R.string.error_3000_entity_too_large)
            "3100" -> context.getString(R.string.error_3100_parse_failed)
            "3200" -> context.getString(R.string.error_3200_connection_aborted)
            "3300" -> context.getString(R.string.error_3300_timeout)
            "3400" -> context.getString(R.string.error_3400_method_not_supported)
            "3500" -> context.getString(R.string.error_3500_missing_path_variable)
            "3600" -> context.getString(R.string.error_3600_missing_request_parameter)
            "3700" -> context.getString(R.string.error_3700_missing_request_part)
            "3800" -> context.getString(R.string.error_3800_request_binding)
            "3900" -> context.getString(R.string.error_3900_validation)
            "4000" -> context.getString(R.string.error_4000_no_handler_found)
            "4100" -> context.getString(R.string.error_4100_response_exception)
            "4200" -> context.getString(R.string.error_4200_message_not_writable)

            else -> null
        }
    }

    /**
     * Gets a user-friendly message for an HTTP status code.
     *
     * @param statusCode The HTTP status code
     * @param context Context for accessing string resources
     * @return A user-friendly error message, or null if no specific message exists
     */
    private fun getMessageForStatusCode(statusCode: Int, context: Context): String? {
        return when (statusCode) {
            400 -> context.getString(R.string.error_status_400_bad_request)
            401 -> context.getString(R.string.error_status_401_unauthorized)
            403 -> context.getString(R.string.error_status_403_forbidden)
            404 -> context.getString(R.string.error_status_404_not_found)
            408 -> context.getString(R.string.error_status_408_timeout)
            413 -> context.getString(R.string.error_status_413_too_large)
            415 -> context.getString(R.string.error_status_415_unsupported_media)
            429 -> context.getString(R.string.error_status_429_too_many_requests)
            500 -> context.getString(R.string.error_status_500_server_error)
            503 -> context.getString(R.string.error_status_503_unavailable)
            else -> null
        }
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

    /**
     * Checks if an error indicates the document/resource was not found.
     *
     * @param exception The GiniHealthException to check
     * @return true if the error is a not found error, false otherwise
     */
    fun isNotFoundError(exception: GiniHealthException): Boolean {
        val errorCode = exception.errorItems?.firstOrNull()?.code
        return errorCode in listOf(
            "2501", // Document not found
            "2503", // Payment request not found
            "2504", // Payment not found
            "2505", // Source document not found
            "2507"  // Extractions not found
        ) || exception.statusCode == 404
    }

    fun isValidationError(exception: GiniHealthException): Boolean {
        val errorCode = exception.errorItems?.firstOrNull()?.code
        return errorCode?.startsWith("20") == true || exception.statusCode == 400
    }
}


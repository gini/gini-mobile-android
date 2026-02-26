package net.gini.android.internal.payment

import net.gini.android.core.api.response.ErrorResponse


/**
 * Exception thrown by Gini SDK operations.
 *
 * @property message Raw error message (may be JSON string for backward compatibility)
 * @property cause The underlying cause of this exception (preserves original exception type for backward compatibility)
 * @property statusCode HTTP status code from the API response
 * @property errorResponse Parsed error response object with detailed error information
 */
open class GiniHealthException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null,
    val errorResponse: ErrorResponse? = null
) : Throwable(message, cause) {

    /**
     * Human-readable parsed error message extracted from the error response.
     *
     * This property provides a clean, user-friendly error message suitable for display.
     * Priority order:
     * 1. Top-level message field (e.g., "Validation of the request entity failed")
     * 2. First error item's message (e.g., "Service unavailable, please try again later")
     * 3. Raw message (JSON string fallback for backward compatibility)
     * 4. "Unknown error" (last resort)
     *
     * **Use this for displaying errors to users** instead of the raw `message` property.
     *
     * Example Error Response:
     * ```json
     * {
     *   "message": "Validation of the request entity failed",
     *   "requestId": "8896f9dc-260d-4133-9848-c54e5715270f",
     *   "items": [{"code": "2002", "message": "Value too long"}]
     * }
     * ```
     *
     * Result:
     * - `message`: Raw JSON string (for backward compatibility)
     * - `parsedMessage`: "Validation of the request entity failed"
     */
    val parsedMessage: String
        get() = errorResponse?.message                          // 1. Top-level message (PRIORITY)
            ?: message                                           // 2. Raw JSON fallback
            ?: "Unknown error"                                   // 3. Last resort

    /**
     * Request ID for support tracking.
     * Provide this when contacting support for help with errors.
     */
    val requestId: String? get() = errorResponse?.requestId

    /**
     * All error items with codes, messages, and affected document/request IDs.
     */
    val errorItems get() = errorResponse?.items
}


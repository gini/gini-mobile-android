package net.gini.android.health.sdk.review.model

import net.gini.android.core.api.Resource
import net.gini.android.internal.payment.GiniHealthException

/**
 * Represents the result of processing a document to get its extractions.
 * Wraps the result of the extraction request with enhanced error information from API v5.0.
 */
sealed class ResultWrapper<out T> {

    /**
     * Request completed successfully and data was returned.
     */
    class Success<T>(val value: T) : ResultWrapper<T>()

    /**
     * Request was unable to complete - returns the cause of the error
     */
    class Error<T>(val error: Throwable) : ResultWrapper<T>()

    /**
     * Request did not complete yet.
     */
    class Loading<T> : ResultWrapper<T>()
}

suspend inline fun <T> wrapToResult(crossinline block: suspend () -> Resource<T>): ResultWrapper<T> {
    return try {
        when(val resource = block()) {
            is Resource.Cancelled -> ResultWrapper.Error(
                error = Exception("Request was cancelled")
            )
            is Resource.Error -> {
                ResultWrapper.Error(
                    error = GiniHealthException(
                        message = resource.exception?.message ?: resource.message ?: "Request failed",
                        cause = resource.exception,  //  Preserve original exception (SocketTimeoutException, etc.)
                        statusCode = resource.responseStatusCode,
                        errorResponse = resource.errorResponse
                    )
                )
            }
            is Resource.Success -> ResultWrapper.Success(resource.data)
        }
    } catch (throwable: Throwable) {
        ResultWrapper.Error(throwable)
    }
}
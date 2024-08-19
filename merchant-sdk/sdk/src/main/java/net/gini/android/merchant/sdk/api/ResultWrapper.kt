package net.gini.android.merchant.sdk.api

import net.gini.android.core.api.Resource

/**
 * Represents the result of processing a document to get its extractions.
 * Wraps the result of the extraction request.
 */
internal sealed class ResultWrapper<out T> {

    /**
     * Request completed successfully and extractions were returned.
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

internal suspend inline fun <T> wrapToResult(crossinline block: suspend () -> Resource<T>): ResultWrapper<T> {
    return try {
        when(val resource = block()) {
            is Resource.Cancelled -> ResultWrapper.Error(Exception("Cancelled"))
            is Resource.Error -> ResultWrapper.Error(resource.exception ?: Exception(resource.message))
            is Resource.Success -> ResultWrapper.Success(resource.data)
        }
    } catch (throwable: Throwable) {
        ResultWrapper.Error(throwable)
    }
}
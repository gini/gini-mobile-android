package net.gini.android.health.sdk.review.model

import net.gini.android.core.api.Resource

sealed class ResultWrapper<out T> {
    class Success<T>(val value: T) : ResultWrapper<T>()
    class Error<T>(val error: Throwable) : ResultWrapper<T>()
    class Loading<T> : ResultWrapper<T>()

    inline fun <U> mapSuccess(crossinline block: (T) -> U): ResultWrapper<U> {
        return when(this) {
            is Error -> Error(error)
            is Loading -> Loading()
            is Success -> Success(block(value))
        }
    }
}

suspend inline fun <T> wrapToResult(crossinline block: suspend () -> Resource<T>): ResultWrapper<T> {
    return when(val resource = block()) {
        is Resource.Cancelled -> ResultWrapper.Error(Exception("Cancelled"))
        is Resource.Error -> ResultWrapper.Error(resource.exception ?: Exception(resource.message))
        is Resource.Success -> ResultWrapper.Success(resource.data)
    }
}
package net.gini.android.bank.sdk.exampleapp.core

sealed class ResultWrapper<out T> {
    class Success<T>(val value: T) : ResultWrapper<T>()
    class Error<T>(val error: Throwable) : ResultWrapper<T>()
    class Loading<T> : ResultWrapper<T>()
}

suspend inline fun <T> wrapToResult(crossinline block: suspend () -> T): ResultWrapper<T> = try {
    ResultWrapper.Success(block())
} catch (throwable: Throwable) {
    ResultWrapper.Error(throwable)
}
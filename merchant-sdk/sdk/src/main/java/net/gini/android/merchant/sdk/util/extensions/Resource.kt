package net.gini.android.merchant.sdk.util.extensions

import net.gini.android.core.api.Resource.Error

fun <T> Error<T>.toException(): Exception {
    return if (exception != null && message != null) {
        Exception(message, exception)
    } else if (message != null) {
        Exception(message)
    } else if (exception != null) {
        Exception(exception)
    } else {
        Exception("Unknown error")
    }
}
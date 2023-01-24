package net.gini.android.capture.internal.network

import jersey.repackaged.jsr166e.CompletionException
import net.gini.android.capture.error.ErrorType

/**
 * Internal use only.
 *
 * @suppress
 */
class FailureException(val errorType: ErrorType) : RuntimeException() {

    companion object {
        /**
         * Tries to cast the [Throwable] received from a [jersey.repackaged.jsr166e.CompletableFuture]
         * to a [FailureException].
         *
         * If the throwable is [CompletionException], then this method tries to cast the throwable's cause to [FailureException].
         *
         * [jersey.repackaged.jsr166e.CompletableFuture]s can wrap the actual throwable in a [CompletionException].
         * For example when using [jersey.repackaged.jsr166e.CompletableFuture.allOf].
         *
         * @param throwable received from a [jersey.repackaged.jsr166e.CompletableFuture]
         * @return [FailureException] or `null`, if casting failed
         */
        @JvmStatic
        fun tryCastFromCompletableFutureThrowable(throwable: Throwable): FailureException? {
            return when {
                throwable is FailureException -> throwable
                throwable is CompletionException && throwable.cause is FailureException -> throwable.cause as FailureException
                else -> null
            }
        }
    }
}
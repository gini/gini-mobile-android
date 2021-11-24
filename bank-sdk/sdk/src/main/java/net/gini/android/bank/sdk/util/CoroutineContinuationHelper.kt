package net.gini.android.bank.sdk.util

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Created by Alpár Szotyori on 22.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * Helper functions for Kotlin coroutine [Continuation]s.
 */
class CoroutineContinuationHelper {

    companion object {

        /**
         * Helper function to get the result of a Kotlin coroutine continuation in a callback.
         *
         * You should use this function if you can't use Kotlin coroutines when invoking suspending functions
         * in the Bank SDK's public API (e.g., [GiniBank.getPaymentRequest()] or [GiniBank.resolvePaymentRequest()]).
         *
         * @param callback to return the continuation result
         * @param looper to use for invoking the callback on a specific thread (using a [Handler])
         */
        @JvmOverloads
        @JvmStatic
        fun <R> callbackContinuation(
            callback: ContinuationCallback<R>,
            looper: Looper = Looper.getMainLooper()
        ): Continuation<R> {
            return object : Continuation<R> {
                override val context: CoroutineContext
                    get() = Dispatchers.Unconfined

                val handler = Handler(looper)

                override fun resumeWith(result: Result<R>) {
                    handler.post(Runnable {
                        result.fold(
                            onSuccess = { callback.onFinished(it) },
                            onFailure = { error ->
                                if (error is CancellationException) {
                                    callback.onCancelled()
                                } else {
                                    callback.onFailed(error)
                                }
                            }
                        )
                    })
                }
            }
        }
    }

    interface ContinuationCallback<R> {
        fun onFinished(result: R)
        fun onFailed(error: Throwable)
        fun onCancelled()
    }
}
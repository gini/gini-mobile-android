package net.gini.android.bank.sdk.invoice.network

import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.capture.error.ErrorType.Companion.typeFromError
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InvoicePreviewFileNetworkService(
    private val giniCaptureNetworkService: GiniCaptureNetworkService,
) {

    suspend fun getFile(url: String): ByteArray {
        return suspendCancellableCoroutine { continuation ->
            giniCaptureNetworkService.getFile(url, object :
                GiniCaptureNetworkCallback<Array<Byte>, Error> {

                override fun failure(error: Error) {
                    val errorType = typeFromError(error)
                    continuation.resumeWithException(FailureException(errorType))
                }

                override fun success(result: Array<Byte>) {
                    continuation.resume(result.toByteArray())
                }

                override fun cancelled() {
                    continuation.cancel()
                }
            })
        }
    }
}

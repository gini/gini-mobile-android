package net.gini.android.bank.sdk.capture.skonto.invoice.network

import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class SkontoFileNetworkService(
    private val giniCaptureNetworkService: GiniCaptureNetworkService,
) {

    suspend fun getFile(url: String): ByteArray {
        return suspendCancellableCoroutine { continuation ->
            giniCaptureNetworkService.getFile(url, object :
                GiniCaptureNetworkCallback<Array<Byte>, Error> {

                override fun failure(error: Error) {
                    continuation.resumeWithException(IllegalStateException(error.message))
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

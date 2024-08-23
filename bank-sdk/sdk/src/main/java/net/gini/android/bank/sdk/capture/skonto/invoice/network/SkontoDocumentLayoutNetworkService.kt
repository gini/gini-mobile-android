package net.gini.android.bank.sdk.capture.skonto.invoice.network

import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.capture.internal.network.model.DocumentLayout
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class SkontoDocumentLayoutNetworkService(
    private val giniCaptureNetworkService: GiniCaptureNetworkService,
) {

    suspend fun getLayout(documentId: String): DocumentLayout {
        return suspendCancellableCoroutine { continuation ->
            giniCaptureNetworkService.getDocumentLayout(documentId, object :
                GiniCaptureNetworkCallback<DocumentLayout, Error> {

                override fun failure(error: Error) {
                    continuation.resumeWithException(IllegalStateException(error.message))
                }

                override fun success(result: DocumentLayout) {
                    continuation.resume(result)
                }

                override fun cancelled() {
                    continuation.cancel()
                }
            })
        }
    }
}

package net.gini.android.bank.sdk.invoice.network

import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.capture.error.ErrorType.Companion.typeFromError
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.network.model.DocumentPage
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InvoicePreviewDocumentPagesNetworkService(
    private val giniCaptureNetworkService: GiniCaptureNetworkService,
) {

    suspend fun getDocumentPages(documentId: String): List<DocumentPage> {
        return suspendCancellableCoroutine { continuation ->
            giniCaptureNetworkService.getDocumentPages(documentId, object :
                GiniCaptureNetworkCallback<List<DocumentPage>, Error> {

                override fun failure(error: Error) {
                    val errorType = typeFromError(error)
                    continuation.resumeWithException(FailureException(errorType))
                }

                override fun success(result: List<DocumentPage>) {
                    continuation.resume(result)
                }

                override fun cancelled() {
                    continuation.cancel()
                }
            })
        }
    }
}

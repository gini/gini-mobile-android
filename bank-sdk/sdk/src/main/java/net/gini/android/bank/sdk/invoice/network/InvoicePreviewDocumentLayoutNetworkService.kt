package net.gini.android.bank.sdk.invoice.network

import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.capture.error.ErrorType.Companion.typeFromError
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.internal.network.model.DocumentLayout
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InvoicePreviewDocumentLayoutNetworkService(
    private val giniCaptureNetworkService: GiniCaptureNetworkService,
) {

    suspend fun getLayout(documentId: String): DocumentLayout? {
        return suspendCancellableCoroutine { continuation ->
            giniCaptureNetworkService.getDocumentLayout(documentId, object :
                GiniCaptureNetworkCallback<DocumentLayout, Error> {

                override fun failure(error: Error) {
                    val errorType = typeFromError(error)
                    continuation.resumeWithException(FailureException(errorType))
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

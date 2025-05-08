package net.gini.android.bank.sdk.invoice.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.gini.android.bank.sdk.invoice.image.InvoicePreviewPageImageProcessor
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentLayoutNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentPagesNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewFileNetworkService
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.network.model.GiniCaptureBox

internal class LoadInvoiceBitmapsUseCase(
    private val invoicePreviewDocumentLayoutNetworkService: InvoicePreviewDocumentLayoutNetworkService,
    private val invoicePreviewDocumentPagesNetworkService: InvoicePreviewDocumentPagesNetworkService,
    private val invoicePreviewFileNetworkService: InvoicePreviewFileNetworkService,
    private val invoicePreviewPageImageProcessor: InvoicePreviewPageImageProcessor
) {
    suspend operator fun invoke(
        documentId: String,
        highlightBoxes: List<GiniCaptureBox>
    ): List<Bitmap> {

        val layout = fetchWithExceptionHandling(
            { invoicePreviewDocumentLayoutNetworkService.getLayout(documentId) },
            "Failed to fetch document layout for document ID: $documentId"
        )

        val pages = fetchWithExceptionHandling(
            { invoicePreviewDocumentPagesNetworkService.getDocumentPages(documentId) },
            "Failed to fetch document pages for document ID: $documentId"
        )

        val bitmaps = pages.map { documentPage ->
            val bitmapBytes = fetchWithExceptionHandling(
                { invoicePreviewFileNetworkService.getFile(documentPage.getSmallestImage()!!) },
                "Failed to fetch file for page: ${documentPage.pageNumber}"
            )

            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            val pageHighlights = highlightBoxes.filter { it.pageNumber == documentPage.pageNumber }

            val skontoPageLayout = layout?.pages?.find { documentPage.pageNumber == it.number }
                ?: throw IllegalStateException("Layout for page #${documentPage.pageNumber} not found")

            invoicePreviewPageImageProcessor.processImage(
                image = bitmap,
                highlightBoxes = pageHighlights,
                skontoPageLayout = skontoPageLayout
            )
        }

        return bitmaps
    }

    private suspend fun <T> fetchWithExceptionHandling(
        fetcher: suspend () -> T,
        errorMessage: String
    ): T {
        return try {
            fetcher()
        } catch (failureException: FailureException) {
            throw failureException
        } catch (e: Exception) {
            throw IllegalStateException(errorMessage, e)
        }
    }
}

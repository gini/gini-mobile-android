package net.gini.android.bank.sdk.invoice.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.gini.android.bank.sdk.invoice.image.InvoicePreviewPageImageProcessor
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentLayoutNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentPagesNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewFileNetworkService
import net.gini.android.capture.network.model.GiniCaptureBox

internal class LoadInvoiceBitmapsUseCase(
    private val invoicePreviewDocumentLayoutNetworkService: InvoicePreviewDocumentLayoutNetworkService,
    private val invoicePreviewDocumentPagesNetworkService: InvoicePreviewDocumentPagesNetworkService,
    private val invoicePreviewFileNetworkService: InvoicePreviewFileNetworkService,
    private val invoicePreviewPageImageProcessor: InvoicePreviewPageImageProcessor
) {

    suspend operator fun invoke(documentId: String, highlightBoxes: List<GiniCaptureBox>) : List<Bitmap>? {
        val layout = runCatching {
            invoicePreviewDocumentLayoutNetworkService.getLayout(documentId)
        }.getOrNull()
        val pages = kotlin.runCatching {
            invoicePreviewDocumentPagesNetworkService.getDocumentPages(documentId)
        }.getOrNull()

        val bitmaps = pages?.map { documentPage ->
            val bitmapBytes =
                invoicePreviewFileNetworkService.getFile(documentPage.getSmallestImage()!!)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            val pageHighlights = highlightBoxes.filter { it.pageNumber == documentPage.pageNumber }

            val skontoPageLayout = layout?.pages?.find { documentPage.pageNumber == it.number }


            pageHighlights.let {
                invoicePreviewPageImageProcessor.processImage(
                    image = bitmap,
                    highlightBoxes = pageHighlights,
                    skontoPageLayout = skontoPageLayout
                        ?: error("Layout for page #$${documentPage.pageNumber} not found")
                )
            }
        }

        return bitmaps
    }
}

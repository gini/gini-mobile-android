package net.gini.android.bank.sdk.invoice

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.invoice.image.InvoicePreviewPageImageProcessor
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentLayoutNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentPagesNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewFileNetworkService
import net.gini.android.capture.network.model.GiniCaptureBox

internal class InvoicePreviewViewModel(
    private val screenTitle: String,
    private val documentId: String,
    private val highlightBoxes: List<GiniCaptureBox>,
    private val infoTextLines: List<String>,
    private val invoicePreviewDocumentLayoutNetworkService: InvoicePreviewDocumentLayoutNetworkService,
    private val invoicePreviewDocumentPagesNetworkService: InvoicePreviewDocumentPagesNetworkService,
    private val invoicePreviewFileNetworkService: InvoicePreviewFileNetworkService,
    private val invoicePreviewPageImageProcessor: InvoicePreviewPageImageProcessor,
) : ViewModel() {

    val stateFlow: MutableStateFlow<InvoicePreviewFragmentState> =
        MutableStateFlow(createInitalState())

    private fun createInitalState() =
        InvoicePreviewFragmentState(
            screenTitle = screenTitle,
            isLoading = true,
            images = emptyList(),
            infoTextLines = infoTextLines,
        )

    init {
        init()
    }

    private fun init() = viewModelScope.launch {

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

        val currentState = stateFlow.value
        if (bitmaps != null) {
            stateFlow.emit(
                currentState.copy(
                    isLoading = false,
                    images = bitmaps
                )
            )
        }
    }
}

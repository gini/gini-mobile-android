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
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes

internal class InvoicePreviewViewModel(
    private val documentId: String?,
    private val skontoInvoiceHighlights: List<SkontoInvoiceHighlightBoxes>,
    private val skontoData: SkontoData?,
    private val invoicePreviewDocumentLayoutNetworkService: InvoicePreviewDocumentLayoutNetworkService,
    private val invoicePreviewDocumentPagesNetworkService: InvoicePreviewDocumentPagesNetworkService,
    private val invoicePreviewFileNetworkService: InvoicePreviewFileNetworkService,
    private val invoicePreviewPageImageProcessor: InvoicePreviewPageImageProcessor,
) : ViewModel() {

    val stateFlow: MutableStateFlow<InvoicePreviewFragmentState> =
        MutableStateFlow(createInitalState())

    private fun createInitalState() =
        InvoicePreviewFragmentState(
            isLoading = true,
            images = emptyList(),
            skontoData = skontoData,
        )

    init {
        init()
    }

    private fun init() = viewModelScope.launch {
        requireNotNull(documentId)

        val layout = invoicePreviewDocumentLayoutNetworkService.getLayout(documentId)
        val pages = invoicePreviewDocumentPagesNetworkService.getDocumentPages(documentId)

        val bitmaps = pages.map { documentPage ->
            val bitmapBytes = invoicePreviewFileNetworkService.getFile(documentPage.getSmallestImage()!!)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            val pageHighlights = skontoInvoiceHighlights.find {
                it.getExistBoxes().all { it.pageNumber == documentPage.pageNumber }
            }

            val skontoPageLayout = layout.pages.find { documentPage.pageNumber == it.number }


            pageHighlights?.let {
                invoicePreviewPageImageProcessor.processImage(
                    image = bitmap,
                    skontoInvoiceHighlightBoxes = pageHighlights,
                    skontoPageLayout = skontoPageLayout
                        ?: error("Layout for page #$${documentPage.pageNumber} not found")
                )
            } ?: bitmap
        }

        val currentState = stateFlow.value
        stateFlow.emit(
            currentState.copy(
                isLoading = false,
                images = bitmaps
            )
        )
    }
}

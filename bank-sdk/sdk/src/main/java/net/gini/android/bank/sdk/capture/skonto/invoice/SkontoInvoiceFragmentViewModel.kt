package net.gini.android.bank.sdk.capture.skonto.invoice

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.skonto.invoice.image.SkontoPageImageProcessor
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoDocumentLayoutNetworkService
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoDocumentPagesNetworkService
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoFileNetworkService
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.internal.network.model.DocumentLayout
import net.gini.android.capture.internal.network.model.DocumentPage

internal class SkontoInvoiceFragmentViewModel(
    private val documentId: String?,
    private val skontoInvoiceHighlights: List<SkontoInvoiceHighlightBoxes>,
    private val skontoData: SkontoData?,
    private val skontoDocumentLayoutNetworkService: SkontoDocumentLayoutNetworkService,
    private val skontoDocumentPagesNetworkService: SkontoDocumentPagesNetworkService,
    private val skontoFileNetworkService: SkontoFileNetworkService,
    private val skontoPageImageProcessor: SkontoPageImageProcessor,
) : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoInvoiceFragmentState> =
        MutableStateFlow(createInitalState())

    private fun createInitalState() =
        SkontoInvoiceFragmentState(
            isLoading = true,
            images = emptyList(),
            skontoData = skontoData,
        )

    init {
        init()
    }

    private fun init() = viewModelScope.launch {
        requireNotNull(documentId)

        val layout = runCatching {
            skontoDocumentLayoutNetworkService.getLayout(documentId)
        }.getOrNull()

        val pages = runCatching {
            skontoDocumentPagesNetworkService.getDocumentPages(documentId)
        }.getOrElse { emptyList() }


        val bitmaps = pages?.map { documentPage ->
            val bitmapBytes = skontoFileNetworkService.getFile(documentPage.getSmallestImage()!!)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            val pageHighlights = skontoInvoiceHighlights.find {
                it.getExistBoxes().all { it.pageNumber == documentPage.pageNumber }
            }

            val skontoPageLayout = layout?.pages?.find { documentPage.pageNumber == it.number }

            pageHighlights?.let {
                skontoPageImageProcessor.processImage(
                    image = bitmap,
                    skontoInvoiceHighlightBoxes = pageHighlights,
                    skontoPageLayout = skontoPageLayout
                        ?: error("Layout for page #$${documentPage.pageNumber} not found")
                )
            } ?: bitmap
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

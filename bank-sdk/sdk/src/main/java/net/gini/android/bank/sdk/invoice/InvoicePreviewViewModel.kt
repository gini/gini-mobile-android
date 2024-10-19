package net.gini.android.bank.sdk.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.invoice.usecase.LoadInvoiceBitmapsUseCase
import net.gini.android.capture.network.model.GiniCaptureBox

internal class InvoicePreviewViewModel(
    private val screenTitle: String,
    private val documentId: String,
    private val highlightBoxes: List<GiniCaptureBox>,
    private val infoTextLines: List<String>,
    private val loadInvoiceBitmapsUseCase: LoadInvoiceBitmapsUseCase,
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
        val bitmaps = loadInvoiceBitmapsUseCase.invoke(documentId, highlightBoxes)

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

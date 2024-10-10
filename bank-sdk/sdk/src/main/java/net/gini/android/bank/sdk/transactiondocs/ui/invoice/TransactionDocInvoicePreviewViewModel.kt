package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.invoice.usecase.LoadInvoiceBitmapsUseCase
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.network.model.GiniCaptureBox

internal class TransactionDocInvoicePreviewViewModel(
    private val screenTitle: String,
    private val documentId: String,
    private val highlightBoxes: List<GiniCaptureBox>,
    private val infoTextLines: List<String>,
    private val loadInvoiceBitmapsUseCase: LoadInvoiceBitmapsUseCase,
    private val attachedToTransactionDocumentProvider: AttachedToTransactionDocumentProvider,
) : ViewModel() {

    val stateFlow: MutableStateFlow<TransactionDocInvoicePreviewFragmentState> =
        MutableStateFlow(createInitalState())

    private fun createInitalState() =
        TransactionDocInvoicePreviewFragmentState(
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

    fun onDeleteClicked() {
        attachedToTransactionDocumentProvider.clear()
    }
}

package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import androidx.lifecycle.ViewModel
import net.gini.android.bank.sdk.invoice.usecase.LoadInvoiceBitmapsUseCase
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.network.FailureException
import net.gini.android.capture.network.model.GiniCaptureBox
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

internal class TransactionDocInvoicePreviewViewModel(
    private val screenTitle: String,
    private val documentId: String,
    private val highlightBoxes: List<GiniCaptureBox>,
    private val infoTextLines: List<String>,
    private val loadInvoiceBitmapsUseCase: LoadInvoiceBitmapsUseCase,
    private val attachedToTransactionDocumentProvider: AttachedToTransactionDocumentProvider,
) : ViewModel(), ContainerHost<TransactionDocInvoicePreviewFragmentState, Unit> {

    override val container: Container<TransactionDocInvoicePreviewFragmentState, Unit> = container(
        initialState = createInitalState()
    )

    private fun createInitalState() =
        TransactionDocInvoicePreviewFragmentState.Ready(
            screenTitle = screenTitle,
            isLoading = true,
            images = emptyList(),
            infoTextLines = infoTextLines,
        )

    init {
        init()
    }

    internal fun init() = intent {
        runCatching {
            reduce { createInitalState() }
            val bitmaps = loadInvoiceBitmapsUseCase.invoke(documentId, highlightBoxes)
            reduce {
                TransactionDocInvoicePreviewFragmentState.Ready(
                    isLoading = false,
                    images = bitmaps,
                    screenTitle = screenTitle,
                    infoTextLines = infoTextLines,
                )
            }
        }.onFailure {
            reduce {
                if (it is FailureException) {
                    TransactionDocInvoicePreviewFragmentState.Error(
                        errorType = it.errorType
                    )
                } else {
                    TransactionDocInvoicePreviewFragmentState.Error(
                        errorType = ErrorType.GENERAL
                    )
                }
            }
        }

    }

    fun onDeleteClicked() {
        attachedToTransactionDocumentProvider.clear()
    }
}

package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import net.gini.android.capture.network.model.GiniCaptureBox
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val transactionDocInvoicePreviewScreenModule = module {


    // the most part of dependencies already defined at [InvoicePreviewScreenModule]

    viewModel { (screenTitle: String,
                    documentId: String,
                    infoTextLines: Array<String>,
                    highlights: Array<GiniCaptureBox>) ->
        TransactionDocInvoicePreviewViewModel(
            screenTitle = screenTitle,
            documentId = documentId,
            infoTextLines = infoTextLines.toList(),
            highlightBoxes = highlights.toList(),
            loadInvoiceBitmapsUseCase = get(),
            attachedToTransactionDocumentProvider = get(),
        )
    }
}

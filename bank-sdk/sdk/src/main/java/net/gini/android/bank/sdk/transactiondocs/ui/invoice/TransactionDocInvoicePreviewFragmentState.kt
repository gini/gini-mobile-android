package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import android.graphics.Bitmap
import net.gini.android.capture.error.ErrorType

sealed interface TransactionDocInvoicePreviewFragmentState {

    data class Ready(
        val screenTitle: String,
        val isLoading: Boolean,
        val images: List<Bitmap>,
        val infoTextLines: List<String>,
    ) : TransactionDocInvoicePreviewFragmentState

    data class Error(val errorType: ErrorType) : TransactionDocInvoicePreviewFragmentState
}


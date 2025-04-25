package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import android.graphics.Bitmap

sealed interface TransactionDocInvoicePreviewFragmentState {

    data class Ready(
        val screenTitle: String,
        val isLoading: Boolean,
        val images: List<Bitmap>,
        val infoTextLines: List<String>,
    ) : TransactionDocInvoicePreviewFragmentState

    data object Error : TransactionDocInvoicePreviewFragmentState
}


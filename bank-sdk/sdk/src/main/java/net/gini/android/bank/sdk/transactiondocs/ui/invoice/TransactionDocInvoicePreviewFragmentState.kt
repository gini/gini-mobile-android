package net.gini.android.bank.sdk.transactiondocs.ui.invoice

import android.graphics.Bitmap

data class TransactionDocInvoicePreviewFragmentState(
    val screenTitle: String,
    val isLoading: Boolean,
    val images: List<Bitmap>,
    val infoTextLines: List<String>,
)

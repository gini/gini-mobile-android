package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap

sealed interface InvoicePreviewFragmentState {

    data class Ready(
        val screenTitle: String,
        val isLoading: Boolean,
        val images: List<Bitmap>,
        val infoTextLines: List<String>,
    ) : InvoicePreviewFragmentState

    data object Error : InvoicePreviewFragmentState
}


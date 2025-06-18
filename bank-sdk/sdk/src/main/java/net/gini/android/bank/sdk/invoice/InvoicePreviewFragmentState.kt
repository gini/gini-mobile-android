package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap
import net.gini.android.capture.error.ErrorType

sealed interface InvoicePreviewFragmentState {

    data class Ready(
        val screenTitle: String,
        val isLoading: Boolean,
        val images: List<Bitmap>,
        val infoTextLines: List<String>,
    ) : InvoicePreviewFragmentState

    data class Error(val errorType: ErrorType) : InvoicePreviewFragmentState
}


package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap

data class InvoicePreviewFragmentState(
    val screenTitle: String,
    val isLoading: Boolean,
    val images: List<Bitmap>,
    val infoTextLines: List<String>,
)

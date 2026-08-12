package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap

/**
 * Groups content state data for InvoiceScreenReadyContent to satisfy
 * kotlin:S107 (too many parameters).
 */
internal data class InvoiceScreenContentState(
    val isLoading: Boolean,
    val screenTitle: String,
    val infoTextLines: List<String>,
    val images: List<Bitmap>,
)


package net.gini.android.bank.sdk.invoice

import android.graphics.Bitmap
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData

data class InvoicePreviewFragmentState(
    val isLoading: Boolean,
    val images: List<Bitmap>,
    val infoTextLines: List<String>,
)

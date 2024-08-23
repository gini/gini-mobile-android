package net.gini.android.bank.sdk.capture.skonto.invoice

import android.graphics.Bitmap

data class SkontoInvoiceFragmentState(
    val isLoading: Boolean,
    val images: List<Bitmap>,

)

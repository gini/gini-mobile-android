package net.gini.android.bank.sdk.capture.skonto.invoice

import android.graphics.Bitmap
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData

data class SkontoInvoiceFragmentState(
    val isLoading: Boolean,
    val images: List<Bitmap>,
    val skontoData: SkontoData?,
)

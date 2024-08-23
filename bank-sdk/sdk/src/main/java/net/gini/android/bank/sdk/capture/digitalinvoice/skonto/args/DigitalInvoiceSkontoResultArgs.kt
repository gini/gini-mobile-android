package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData

@Parcelize
internal data class DigitalInvoiceSkontoResultArgs(
    val isSkontoEnabled: Boolean,
    val skontoData: SkontoData,
) : Parcelable
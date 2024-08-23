package net.gini.android.bank.sdk.capture.digitalinvoice.args

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

@JvmInline
@Parcelize
value class DigitalInvoiceSpecificExtractionsArg(
    val value: Map<String, GiniCaptureSpecificExtraction>
) : Parcelable
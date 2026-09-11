package net.gini.android.bank.sdk.capture.digitalinvoice.args

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

@Parcelize
class ExtractionsResultData(
    val specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
    val compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
) : Parcelable

package net.gini.android.bank.sdk.capture.skonto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.network.model.GiniCaptureBox

@Parcelize
data class SkontoInvoiceHighlightBoxes(
    val skontoPercentageDiscounted: GiniCaptureBox?,
    val skontoPaymentMethod: GiniCaptureBox?,
    val skontoAmountToPay: GiniCaptureBox?,
    val skontoAmountDiscounted: GiniCaptureBox?,
    val skontoRemainingDays: GiniCaptureBox?,
    val skontoDueDate: GiniCaptureBox?,
) : Parcelable {

    fun getExistBoxes() = listOfNotNull(
        skontoPercentageDiscounted,
        skontoPaymentMethod,
        skontoAmountToPay,
        skontoAmountDiscounted,
        skontoRemainingDays,
        skontoDueDate,
    )
}

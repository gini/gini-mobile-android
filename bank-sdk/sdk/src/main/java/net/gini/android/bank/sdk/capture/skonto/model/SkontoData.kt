package net.gini.android.bank.sdk.capture.skonto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

@Parcelize
data class SkontoData(
    val skontoPercentageDiscounted: BigDecimal,
    val skontoPaymentMethod: SkontoPaymentMethod?,
    val skontoAmountToPay: Amount,
    val fullAmountToPay: Amount,
    val skontoRemainingDays: Int,
    val skontoDueDate: LocalDate,
) : Parcelable {

    @Parcelize
    enum class SkontoPaymentMethod : Parcelable {
        Unspecified, Cash, PayPal
    }
}

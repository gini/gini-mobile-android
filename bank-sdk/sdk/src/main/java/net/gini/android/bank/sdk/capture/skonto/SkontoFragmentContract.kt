package net.gini.android.bank.sdk.capture.skonto

import java.math.BigDecimal
import java.time.LocalDate

object SkontoFragmentContract {

    sealed class State {
        object Idle : State()

        data class Ready(
            val isSkontoSectionActive: Boolean,
            val paymentInDays: Int,
            val discountValue: BigDecimal,
            val skontoAmount: BigDecimal,
            val discountDueDate: LocalDate,
            val fullAmount: BigDecimal,
            val totalAmount: BigDecimal,
            val currency: String,
        ) : State()
    }

}
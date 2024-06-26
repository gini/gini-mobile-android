package net.gini.android.bank.sdk.capture.skonto

import java.time.LocalDate

object SkontoFragmentContract {

    sealed class State {
        object Idle : State()

        data class Ready(
            val isDiscountSectionActive: Boolean,
            val paymentInDays: Int,
            val discountValue: Float,
            val amountWithDiscount: Float,
            val discountDueDate: LocalDate,
            val withoutDiscountAmount: Float,
            val totalAmount: Float,
            val totalDiscount: Float,
        ) : State()
    }

}
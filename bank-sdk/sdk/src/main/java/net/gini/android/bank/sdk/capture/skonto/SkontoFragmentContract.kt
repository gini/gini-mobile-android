package net.gini.android.bank.sdk.capture.skonto

import java.time.LocalDate

object SkontoFragmentContract {

    sealed class State {
        object Idle : State()

        data class Ready(
            val isSkontoSectionActive: Boolean,
            val paymentInDays: Int,
            val discountValue: Float,
            val skontoAmount: Float,
            val discountDueDate: LocalDate,
            val fullAmount: String,
            val totalAmount: Float,
            val currency: String,
            val totalDiscount: Float,
        ) : State()
    }

}
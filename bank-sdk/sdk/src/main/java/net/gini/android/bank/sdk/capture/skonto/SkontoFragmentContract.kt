package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import java.math.BigDecimal
import java.time.LocalDate

internal object SkontoFragmentContract {

    sealed class State {
        data class Ready(
            val isSkontoSectionActive: Boolean,
            val paymentInDays: Int,
            val skontoPercentage: BigDecimal,
            val skontoAmount: SkontoData.Amount,
            val discountDueDate: LocalDate,
            val fullAmount: SkontoData.Amount,
            val totalAmount: SkontoData.Amount,
            val savedAmount: SkontoData.Amount,
            val paymentMethod: SkontoData.SkontoPaymentMethod,
            val skontoEdgeCase: SkontoEdgeCase?,
            val edgeCaseInfoDialogVisible: Boolean,
        ) : State()
    }

    sealed interface SideEffect {
        object OpenInvoiceScreen : SideEffect
    }

    sealed class SkontoEdgeCase {
        object SkontoLastDay : SkontoEdgeCase()
        object PayByCashOnly : SkontoEdgeCase()
        object SkontoExpired : SkontoEdgeCase()
    }
}
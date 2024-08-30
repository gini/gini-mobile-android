package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

internal object SkontoFragmentContract {

    sealed class State {
        data class Ready(
            val isSkontoSectionActive: Boolean,
            val paymentInDays: Int,
            val skontoPercentage: BigDecimal,
            val skontoAmount: Amount,
            val discountDueDate: LocalDate,
            val fullAmount: Amount,
            val totalAmount: Amount,
            val savedAmount: Amount,
            val paymentMethod: SkontoData.SkontoPaymentMethod,
            val skontoEdgeCase: SkontoEdgeCase?,
            val edgeCaseInfoDialogVisible: Boolean,
        ) : State()
    }

    sealed interface SideEffect {
        data class OpenInvoiceScreen(
            val skontoData: SkontoData
        ) : SideEffect
    }
}
package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import java.time.LocalDate

/**
 * Detects the edge case of Skonto.
 */
internal class GetSkontoEdgeCaseUseCase {

    /**
     * Detects the edge case of Skonto.
     *
     * @param dueDate The due date of the Skonto.
     * @param paymentMethod The payment method of the Skonto.
     *
     * @return The edge case of the Skonto, or null if there is no edge case.
     */
    fun execute(
        dueDate: LocalDate, paymentMethod: SkontoData.SkontoPaymentMethod?
    ): SkontoEdgeCase? {
        val today = LocalDate.now()
        return when {
            dueDate.isBefore(today) -> SkontoEdgeCase.SkontoExpired
            paymentMethod == SkontoData.SkontoPaymentMethod.Cash -> SkontoEdgeCase.PayByCashOnly
            dueDate == today -> SkontoEdgeCase.SkontoLastDay
            else -> null
        }
    }
}

package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.Amount
import java.math.BigDecimal
import java.time.LocalDate

internal sealed interface SkontoScreenState {

    data class Ready(
        val isSkontoSectionActive: Boolean,
        val paymentInDays: Int,
        val skontoPercentage: BigDecimal,
        val skontoAmount: Amount,
        val skontoAmountValidationError: SkontoAmountValidationError?,
        val discountDueDate: LocalDate,
        val fullAmount: Amount,
        val fullAmountValidationError: FullAmountValidationError?,
        val totalAmount: Amount,
        val savedAmount: Amount,
        val paymentMethod: SkontoData.SkontoPaymentMethod,
        val skontoEdgeCase: SkontoEdgeCase?,
        val edgeCaseInfoDialogVisible: Boolean,
        val transactionDialogVisible: Boolean,
    ) : SkontoScreenState {

        sealed interface SkontoAmountValidationError {
            object SkontoAmountMoreThanFullAmount : SkontoAmountValidationError
            object SkontoAmountLimitExceeded : SkontoAmountValidationError
        }

        sealed interface FullAmountValidationError {
            object FullAmountLimitExceeded : FullAmountValidationError
        }
    }
}
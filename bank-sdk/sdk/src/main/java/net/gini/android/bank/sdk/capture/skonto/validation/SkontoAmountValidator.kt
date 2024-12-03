package net.gini.android.bank.sdk.capture.skonto.validation

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import java.math.BigDecimal

internal class SkontoAmountValidator {

    fun execute(
        newSkontoAmount: BigDecimal,
        fullAmount: BigDecimal
    ): SkontoScreenState.Ready.SkontoAmountValidationError? = when {
        newSkontoAmount > fullAmount ->
            SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount

        newSkontoAmount > BigDecimal(SKONTO_AMOUNT_LIMIT) ->
            SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountLimitExceeded

        else -> null
    }

    companion object {
        internal const val SKONTO_AMOUNT_LIMIT = "99999.99"
    }
}

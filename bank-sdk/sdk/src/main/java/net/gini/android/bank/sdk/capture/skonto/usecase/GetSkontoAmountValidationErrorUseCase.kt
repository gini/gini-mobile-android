package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import java.math.BigDecimal

internal class GetSkontoAmountValidationErrorUseCase {

    fun execute(
        newSkontoAmount: BigDecimal,
        fullAmount: BigDecimal
    ): SkontoScreenState.Ready.SkontoAmountValidationError? = when {
        newSkontoAmount > fullAmount ->
            SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount

        newSkontoAmount > BigDecimal.valueOf(SKONTO_AMOUNT_LIMIT) ->
            SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountLimitExceeded

        else -> null
    }

    companion object {
        internal const val SKONTO_AMOUNT_LIMIT = 99_999L
    }
}

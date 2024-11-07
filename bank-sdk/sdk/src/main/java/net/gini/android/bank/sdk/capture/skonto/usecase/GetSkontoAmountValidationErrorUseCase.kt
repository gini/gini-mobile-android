package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import java.math.BigDecimal

/**
 * Use case for validating the Skonto amount.
 */
internal class GetSkontoAmountValidationErrorUseCase {

    /**
     * Validates the Skonto amount.
     *
     * @return [SkontoScreenState.Ready.SkontoAmountValidationError] if the validation fails, null otherwise.
     */
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

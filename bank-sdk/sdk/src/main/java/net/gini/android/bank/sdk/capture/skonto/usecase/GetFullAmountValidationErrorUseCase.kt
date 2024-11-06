package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import java.math.BigDecimal

/**
 * Use case for validating the full amount to ensure it doesn't exceed the limit.
 */
internal class GetFullAmountValidationErrorUseCase {

    /**
     * Validates the full amount to ensure it doesn't exceed the limit.
     */

    fun execute(
        fullAmount: BigDecimal
    ): SkontoScreenState.Ready.FullAmountValidationError? = when {

        fullAmount > BigDecimal.valueOf(SKONTO_AMOUNT_LIMIT) ->
            SkontoScreenState.Ready.FullAmountValidationError.FullAmountLimitExceeded

        else -> null
    }

    companion object {
        private const val SKONTO_AMOUNT_LIMIT = 99_999L
    }
}

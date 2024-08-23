package net.gini.android.bank.sdk.capture.skonto.usecase

import java.math.BigDecimal

/**
 * Use case for calculating the saved amount based on the skonto amount and the full amount.
 */
internal class GetSkontoSavedAmountUseCase {

    /**
     * Calculates the saved amount based on the skonto amount and the full amount.
     *
     * @param skontoAmount The amount of the Skonto.
     * @param fullAmount The full amount.
     *
     * @return The saved amount as a [BigDecimal]. Minimum possible value is 0
     */
    fun execute(skontoAmount: BigDecimal, fullAmount: BigDecimal): BigDecimal =
        fullAmount
            .minus(skontoAmount)
            .coerceAtLeast(BigDecimal.ZERO)
}
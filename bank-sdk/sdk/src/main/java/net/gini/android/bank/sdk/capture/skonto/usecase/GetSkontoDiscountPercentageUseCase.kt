package net.gini.android.bank.sdk.capture.skonto.usecase

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Use case for calculating the discount percentage based on the skonto amount and the full amount.
 */
internal class GetSkontoDiscountPercentageUseCase {

    /**
     * Calculates the discount percentage based on the skonto amount and the full amount.
     *
     * @param skontoAmount The amount of the Skonto.
     * @param fullAmount The full amount.
     *
     * @return The discount percentage as a [BigDecimal].
     */
    fun execute(skontoAmount: BigDecimal, fullAmount: BigDecimal): BigDecimal {
        if (fullAmount == BigDecimal.ZERO) return BigDecimal.ONE.movePointRight(CALCULATIONS_SCALE)
        return BigDecimal.ONE
            .minus(
                skontoAmount.divide(
                    fullAmount,
                    PERCENTAGE_CALCULATIONS_SCALE,
                    RoundingMode.HALF_UP
                )
            )
            .multiply(BigDecimal.ONE.movePointRight(CALCULATIONS_SCALE))
            .coerceAtLeast(BigDecimal.ZERO)
    }

    companion object {
        private const val PERCENTAGE_CALCULATIONS_SCALE = 4
        private const val CALCULATIONS_SCALE = 2
    }
}

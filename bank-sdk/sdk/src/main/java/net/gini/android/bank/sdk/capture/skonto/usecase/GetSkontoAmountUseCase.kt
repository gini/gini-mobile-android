package net.gini.android.bank.sdk.capture.skonto.usecase

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Use case for calculating the Skonto amount based on the full amount and the discount percentage.
 */
internal class GetSkontoAmountUseCase {

    /**
     * Calculates the Skonto amount based on the full amount and the discount percentage.
     *
     * The math: `FullAmount - (FullAmount * (DiscountPercentage / 100))`
     *
     * @param fullAmount The full amount.
     * @param discount The Skonto discount (percentage).
     *
     * @return The calculated Skonto amount.
     */
    fun execute(fullAmount: BigDecimal, discount: BigDecimal): BigDecimal = fullAmount.minus(
        fullAmount.multiply(
            discount.divide(
                BigDecimal.ONE.movePointRight(CALCULATIONS_SCALE),
                CALCULATIONS_SCALE,
                RoundingMode.HALF_UP
            )
        ).setScale(CALCULATIONS_SCALE, RoundingMode.HALF_UP)
    )

    companion object {
        private const val CALCULATIONS_SCALE = 2
    }
}

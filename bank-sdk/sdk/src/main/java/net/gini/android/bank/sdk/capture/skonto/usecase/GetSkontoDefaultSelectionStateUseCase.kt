package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Determines whether the Skonto default selection state should be enabled.
 */
internal class GetSkontoDefaultSelectionStateUseCase {

    /**
     * Determines whether the Skonto default selection state should be enabled.
     *
     * @param skontoEdgeCase The edge case of the Skonto.
     *
     * @return True if the Skonto default selection state should be enabled, false otherwise.
     */
    fun execute(skontoEdgeCase: SkontoEdgeCase?): Boolean =
        skontoEdgeCase != SkontoEdgeCase.PayByCashOnly
                && skontoEdgeCase != SkontoEdgeCase.SkontoExpired

}
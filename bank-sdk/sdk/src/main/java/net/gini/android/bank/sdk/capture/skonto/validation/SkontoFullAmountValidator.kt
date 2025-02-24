package net.gini.android.bank.sdk.capture.skonto.validation

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import java.math.BigDecimal

internal class SkontoFullAmountValidator {

    fun execute(
        fullAmount: BigDecimal
    ): SkontoScreenState.Ready.FullAmountValidationError? = when {

        fullAmount > BigDecimal(SKONTO_AMOUNT_LIMIT) ->
            SkontoScreenState.Ready.FullAmountValidationError.FullAmountLimitExceeded

        else -> null
    }

    companion object {
        private const val SKONTO_AMOUNT_LIMIT = "99999.99"
    }
}

package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.validation

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import java.math.BigDecimal

internal class DigitalInvoiceSkontoAmountValidator {

    operator fun invoke(newSkontoAmount: BigDecimal, fullAmount: BigDecimal)
            : SkontoScreenState.Ready.SkontoAmountValidationError? = when {
        newSkontoAmount > fullAmount ->
            SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount

        else -> null
    }
}

package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.validation

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.DigitalInvoiceSkontoScreenState
import java.math.BigDecimal

internal class DigitalInvoiceSkontoAmountValidator {

    operator fun invoke(newSkontoAmount: BigDecimal, fullAmount: BigDecimal)
            : DigitalInvoiceSkontoScreenState.Ready.SkontoAmountValidationError? = when {
        newSkontoAmount > fullAmount ->
            DigitalInvoiceSkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount

        else -> null
    }
}

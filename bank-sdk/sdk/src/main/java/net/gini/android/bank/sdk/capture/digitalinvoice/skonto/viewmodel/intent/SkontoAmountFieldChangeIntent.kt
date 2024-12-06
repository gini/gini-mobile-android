package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.validation.DigitalInvoiceSkontoAmountValidator
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import java.math.BigDecimal

internal class SkontoAmountFieldChangeIntent(
    private val digitalInvoiceSkontoAmountValidator: DigitalInvoiceSkontoAmountValidator,
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
) {

    fun SkontoContainerHost.run(newValue: BigDecimal) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        if (newValue == state.skontoAmount.value) return@intent

        val skontoAmountValidationError = digitalInvoiceSkontoAmountValidator(
            newValue,
            state.fullAmount.value
        )

        if (skontoAmountValidationError != null) {
            reduce {
                state.copy(
                    skontoAmount = state.skontoAmount,
                    skontoAmountValidationError = SkontoScreenState
                        .Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount
                )
            }
            return@intent
        }

        val discount = getSkontoDiscountPercentageUseCase.execute(
            newValue,
            state.fullAmount.value
        )

        val newSkontoAmount = state.skontoAmount.copy(value = newValue)

        reduce {
            state.copy(
                skontoAmount = newSkontoAmount,
                skontoPercentage = discount,
                skontoAmountValidationError = null,
            )
        }
    }
}

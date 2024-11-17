package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoAmountValidator
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.capture.Amount
import java.math.BigDecimal

internal class SkontoAmountFieldChangeIntent(
    private val skontoAmountValidator: SkontoAmountValidator,
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
) {

    fun SkontoScreenContainerHost.run(newValue: BigDecimal) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        if (newValue == state.skontoAmount.value) return@intent

        val skontoAmountValidationError = skontoAmountValidator.execute(
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

        val totalAmount = if (state.isSkontoSectionActive)
            newValue
        else state.fullAmount.value

        val newSkontoAmount = state.skontoAmount.copy(value = newValue)
        val newTotalAmount = state.totalAmount.copy(value = totalAmount)

        val savedAmountValue = getSkontoSavedAmountUseCase.execute(
            newSkontoAmount.value,
            state.fullAmount.value
        )

        val savedAmount = Amount(savedAmountValue, state.fullAmount.currency)

        reduce {
            state.copy(
                skontoAmountValidationError = skontoAmountValidationError,
                skontoAmount = newSkontoAmount,
                skontoPercentage = discount,
                totalAmount = newTotalAmount,
                savedAmount = savedAmount,
            )
        }
    }
}

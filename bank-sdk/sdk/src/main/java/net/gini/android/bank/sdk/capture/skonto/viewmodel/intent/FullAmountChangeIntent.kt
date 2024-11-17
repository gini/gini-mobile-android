package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoFullAmountValidator
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoFragmentViewModel
import net.gini.android.capture.Amount
import java.math.BigDecimal

internal class FullAmountChangeIntent(
    private val skontoFullAmountValidator: SkontoFullAmountValidator,
    private val getSkontoAmountUseCase: GetSkontoAmountUseCase,
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
) {

    fun SkontoFragmentViewModel.run(newValue: BigDecimal) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        if (newValue == state.fullAmount.value) return@intent

        val validationError = skontoFullAmountValidator.execute(newValue)

        if (validationError != null) {
            reduce {
                state.copy(
                    fullAmountValidationError = validationError
                )
            }
            return@intent
        }

        val totalAmount =
            if (state.isSkontoSectionActive) state.skontoAmount.value else newValue

        val discount = state.skontoPercentage

        val skontoAmount = getSkontoAmountUseCase.execute(newValue, discount)

        val savedAmountValue = getSkontoSavedAmountUseCase.execute(
            skontoAmount,
            newValue
        )

        val savedAmount = Amount(savedAmountValue, state.fullAmount.currency)

        reduce {
            state.copy(
                fullAmountValidationError = validationError,
                skontoAmount = state.skontoAmount.copy(value = skontoAmount),
                fullAmount = state.fullAmount.copy(value = newValue),
                totalAmount = state.totalAmount.copy(value = totalAmount),
                savedAmount = savedAmount,
            )
        }
    }
}

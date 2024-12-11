package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost

internal class SkontoActiveChangeIntent(
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
) {

    fun SkontoScreenContainerHost.run(newValue: Boolean) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        val totalAmount = if (newValue) state.skontoAmount else state.fullAmount
        val discount = getSkontoDiscountPercentageUseCase.execute(
            state.skontoAmount.value,
            state.fullAmount.value
        )

        reduce {
            state.copy(
                isSkontoSectionActive = newValue,
                totalAmount = totalAmount,
                skontoPercentage = discount
            )
        }
    }
}

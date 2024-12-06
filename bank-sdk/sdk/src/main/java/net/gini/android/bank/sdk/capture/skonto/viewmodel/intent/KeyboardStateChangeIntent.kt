package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost

internal class KeyboardStateChangeIntent {

    fun SkontoScreenContainerHost.run(visible: Boolean) = intent {
        if (visible) return@intent
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                fullAmountValidationError = null,
                skontoAmountValidationError = null
            )
        }
    }
}

package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost

internal class KeyboardStateChangeIntent {

    fun SkontoContainerHost.run(visible: Boolean) = intent {
        if (visible) return@intent
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                skontoAmountValidationError = null
            )
        }
    }
}

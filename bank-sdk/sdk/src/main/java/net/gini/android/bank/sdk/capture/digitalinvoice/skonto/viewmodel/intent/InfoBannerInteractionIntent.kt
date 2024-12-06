package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost

internal class InfoBannerInteractionIntent {

    fun SkontoContainerHost.runClick() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                edgeCaseInfoDialogVisible = true,
            )
        }
    }

    fun SkontoContainerHost.runDismiss() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                edgeCaseInfoDialogVisible = false,
            )
        }
    }
}

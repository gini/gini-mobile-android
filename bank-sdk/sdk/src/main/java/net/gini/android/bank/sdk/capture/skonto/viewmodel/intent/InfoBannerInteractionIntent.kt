package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost

internal class InfoBannerInteractionIntent {

    fun SkontoScreenContainerHost.runClick() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                edgeCaseInfoDialogVisible = true,
            )
        }
    }

    fun SkontoScreenContainerHost.runDismiss() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        reduce {
            state.copy(
                edgeCaseInfoDialogVisible = false,
            )
        }
    }
}

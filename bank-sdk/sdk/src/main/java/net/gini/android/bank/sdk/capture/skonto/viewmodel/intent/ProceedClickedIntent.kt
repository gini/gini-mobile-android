package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent.OpenExtractionsScreenSubIntent
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocsFeatureEnabledUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase

internal class ProceedClickedIntent(
    private val openExtractionsScreenSubIntent: OpenExtractionsScreenSubIntent,
    private val getTransactionDocShouldBeAutoAttachedUseCase: GetTransactionDocShouldBeAutoAttachedUseCase,
    private val getTransactionDocsFeatureEnabledUseCase: GetTransactionDocsFeatureEnabledUseCase,
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase,
) {

    fun SkontoScreenContainerHost.run(skontoFragmentListener: SkontoFragmentListener?) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        if (!getTransactionDocsFeatureEnabledUseCase()) {
            with(openExtractionsScreenSubIntent) {
                run(skontoFragmentListener)
            }
            return@intent
        }
        if (getTransactionDocShouldBeAutoAttachedUseCase()) {
            transactionDocDialogConfirmAttachUseCase(true)
            with(openExtractionsScreenSubIntent) {
                run(skontoFragmentListener)
            }
        } else {
            reduce { state.copy(transactionDialogVisible = true) }
        }
    }
}

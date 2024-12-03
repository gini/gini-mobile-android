package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent.OpenExtractionsScreenSubIntent
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogCancelAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase

internal class TransactionDocDialogDecisionIntent(
    private val openExtractionsScreenSubIntent: OpenExtractionsScreenSubIntent,
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase,
    private val transactionDocDialogCancelAttachUseCase: TransactionDocDialogCancelAttachUseCase,
) {

    fun SkontoScreenContainerHost.runConfirm(
        alwaysAttach: Boolean,
        listener: SkontoFragmentListener?
    ) = intent {
        transactionDocDialogConfirmAttachUseCase(alwaysAttach)
        with(openExtractionsScreenSubIntent) { run(listener) }
    }

    fun SkontoScreenContainerHost.runCancel(listener: SkontoFragmentListener?) = intent {
        transactionDocDialogCancelAttachUseCase()
        with(openExtractionsScreenSubIntent) { run(listener) }
    }
}

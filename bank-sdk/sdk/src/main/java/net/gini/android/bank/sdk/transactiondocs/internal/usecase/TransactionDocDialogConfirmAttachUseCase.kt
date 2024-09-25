package net.gini.android.bank.sdk.transactiondocs.internal.usecase

import net.gini.android.bank.sdk.transactiondocs.internal.GiniTransactionDocsSettings
import net.gini.android.bank.sdk.transactiondocs.internal.repository.GiniAttachTransactionDocDialogDecisionRepository

internal class TransactionDocDialogConfirmAttachUseCase(
    private val giniTransactionDocsSettings: GiniTransactionDocsSettings,
    private val attachTransactionDocDialogDecisionRepository: GiniAttachTransactionDocDialogDecisionRepository,
) {

    suspend operator fun invoke(
        alwaysAttach: Boolean
    ) {
        giniTransactionDocsSettings.setAlwaysAttachSetting(alwaysAttach)
        attachTransactionDocDialogDecisionRepository.setAttachDocToTransaction(true)
    }
}

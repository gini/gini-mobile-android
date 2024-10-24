package net.gini.android.bank.sdk.transactiondocs.internal.usecase

import net.gini.android.bank.sdk.transactiondocs.internal.repository.GiniAttachTransactionDocDialogDecisionRepository

internal class TransactionDocDialogCancelAttachUseCase(
    private val attachTransactionDocDialogDecisionRepository: GiniAttachTransactionDocDialogDecisionRepository,
) {

    suspend operator fun invoke() {
        attachTransactionDocDialogDecisionRepository.setAttachDocToTransaction(false)
    }

}

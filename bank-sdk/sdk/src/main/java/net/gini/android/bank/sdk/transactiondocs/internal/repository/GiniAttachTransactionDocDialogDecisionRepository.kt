package net.gini.android.bank.sdk.transactiondocs.internal.repository

internal class GiniAttachTransactionDocDialogDecisionRepository {

    var attachDocToTransaction: Boolean = false
        private set

    fun setAttachDocToTransaction(value: Boolean) {
        attachDocToTransaction = value
    }
}

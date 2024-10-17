package net.gini.android.bank.sdk.transactiondocs.internal.repository

internal class GiniAttachTransactionDocDialogDecisionRepository {

    private var attachDocToTransaction: Boolean = false

    fun setAttachDocToTransaction(value: Boolean) {
        attachDocToTransaction = value
    }

    fun getAttachDocToTransaction(): Boolean = attachDocToTransaction
}

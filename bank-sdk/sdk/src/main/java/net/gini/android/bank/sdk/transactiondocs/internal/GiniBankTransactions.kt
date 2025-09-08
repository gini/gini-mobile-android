package net.gini.android.bank.sdk.transactiondocs.internal

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.gini.android.bank.sdk.transactiondocs.GiniTransactions
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransaction
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionDoc
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionIdentifier

class GiniBankTransactions : GiniTransactions {

    private var transactions: List<GiniTransaction> = emptyList()

    override val giniSelectedTransactionDocsFlow = MutableSharedFlow<List<GiniTransactionDoc>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Sets the list of transactions. Must be called before calling [setSelectedTransaction].
     */
    override fun setTransactions(transactions: List<GiniTransaction>) {
        this.transactions = transactions
    }

    /**
     * Sets the selected transaction.
     *
     * @param identifier [GiniTransactionIdentifier] of the selected transaction.
     *
     * @throws IllegalStateException if transactions list is empty.
     * @throws IllegalArgumentException if transaction with given identifier is not found.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override fun setSelectedTransaction(identifier: GiniTransactionIdentifier) {
        check(transactions.isNotEmpty()) {
            "Transactions list is not empty. " +
                    "Make sure you call `setTransactions(...)` before calling `setSelectedTransaction(...)`"
        }
        val selectedTransaction = transactions.firstOrNull { it.identifier == identifier }
        require(selectedTransaction != null) {
            "Transaction with identifier $identifier not found."
        }
        giniSelectedTransactionDocsFlow.tryEmit(selectedTransaction.attachments)
    }
}

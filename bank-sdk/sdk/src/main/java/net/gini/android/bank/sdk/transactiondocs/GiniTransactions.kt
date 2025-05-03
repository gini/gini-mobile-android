package net.gini.android.bank.sdk.transactiondocs

import kotlinx.coroutines.flow.Flow
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransaction
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionDoc
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionIdentifier

/**
 * Represents a collection of transactions and their associated documents.
 */
interface GiniTransactions {

    /**
     * This flow emits a list of [GiniTransactionDoc] objects representing the documents
     * associated to selected transaction.
     */
    val giniSelectedTransactionDocsFlow: Flow<List<GiniTransactionDoc>>

    /**
     * Sets the transactions
     * This method allows to provide multiple transactions, each containing a list of documents.
     *
     */
    fun setTransactions(transactions: List<GiniTransaction>)

    /**
     * Sets the selected transaction identifier within the SDK.
     * This determines which transaction's documents will be emitted in [giniSelectedTransactionDocsFlow]
     *
     * @param identifier: A [GiniTransactionIdentifier]` representing the identifier of the
     * transaction to select.
     */
    fun setSelectedTransaction(identifier: GiniTransactionIdentifier)
}


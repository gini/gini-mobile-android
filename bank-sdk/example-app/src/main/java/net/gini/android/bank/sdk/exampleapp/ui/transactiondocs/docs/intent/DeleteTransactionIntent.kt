package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent

import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionDocsStorage
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.TransactionDocsContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import javax.inject.Inject

internal class DeleteTransactionIntent @Inject constructor(
    private val transactionDocsStorage: TransactionDocsStorage,
) {

    fun TransactionDocsContainerHost.run(
        transaction: Transaction,
    ) = intent {
        val list = transactionDocsStorage.get() ?: emptyList()
        val newList = list.filter { it != transaction }
        transactionDocsStorage.update(newList)

        reduce { state.copy(transactions = newList) }
    }
}

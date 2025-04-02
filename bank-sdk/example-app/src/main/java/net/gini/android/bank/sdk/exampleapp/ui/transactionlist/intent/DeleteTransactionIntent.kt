package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent

import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionListStorage
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.TransactionListContainerHost
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model.Transaction
import javax.inject.Inject

internal class DeleteTransactionIntent @Inject constructor(
    private val transactionListStorage: TransactionListStorage,
) {

    fun TransactionListContainerHost.run(
        transaction: Transaction,
    ) = intent {
        val list = transactionListStorage.get() ?: emptyList()
        val newList = list.filter { it != transaction }
        transactionListStorage.update(newList)

        reduce { state.copy(transactions = newList) }
    }
}

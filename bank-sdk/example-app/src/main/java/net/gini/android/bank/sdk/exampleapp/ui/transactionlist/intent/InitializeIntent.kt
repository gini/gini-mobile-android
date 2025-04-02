package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent

import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionListStorage
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.TransactionListContainerHost
import javax.inject.Inject

internal class InitializeIntent @Inject constructor(
    private val transactionListStorage: TransactionListStorage,
) {

    fun TransactionListContainerHost.run() = intent {
        val transaction = transactionListStorage.get() ?: emptyList()
        reduce {
            state.copy(transactions = transaction)
        }
    }
}

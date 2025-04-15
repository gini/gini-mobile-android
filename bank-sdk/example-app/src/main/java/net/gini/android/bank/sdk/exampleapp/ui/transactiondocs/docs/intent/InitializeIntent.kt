package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.intent

import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionDocsStorage
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.TransactionDocsContainerHost
import javax.inject.Inject

internal class InitializeIntent @Inject constructor(
    private val transactionDocsStorage: TransactionDocsStorage,
) {

    fun TransactionDocsContainerHost.run() = intent {
        val transaction = transactionDocsStorage.get() ?: emptyList()
        reduce {
            state.copy(transactions = transaction)
        }
    }
}

package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs

import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction

internal sealed interface TransactionDocsSideEffect {
    data class OpenTransactionDocInvoiceScreen(
        val documentId: String,
    ) : TransactionDocsSideEffect

    data class OpenTransactionDetails(
        val transaction: Transaction
    ) : TransactionDocsSideEffect
}

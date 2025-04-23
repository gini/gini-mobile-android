package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction

internal sealed interface TransactionDetailsSideEffect {
    data class OpenTransactionDocInvoiceScreen(
        val transaction: Transaction,
        val documentId: String,
    ) : TransactionDetailsSideEffect
}

package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details

internal sealed interface TransactionDetailsSideEffect {
    data class OpenTransactionDocInvoiceScreen(
        val documentId: String,
    ) : TransactionDetailsSideEffect
}

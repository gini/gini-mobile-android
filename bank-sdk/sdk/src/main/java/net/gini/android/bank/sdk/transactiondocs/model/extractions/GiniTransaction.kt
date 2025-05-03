package net.gini.android.bank.sdk.transactiondocs.model.extractions

data class GiniTransaction(
    val identifier: GiniTransactionIdentifier,
    val attachments: List<GiniTransactionDoc>
)

package net.gini.android.bank.sdk.exampleapp.ui.transactionlist.model

internal data class Transaction(
    val title: String,
    val description: String,
    val amount: String,
    val attachments: List<Attachment>
)
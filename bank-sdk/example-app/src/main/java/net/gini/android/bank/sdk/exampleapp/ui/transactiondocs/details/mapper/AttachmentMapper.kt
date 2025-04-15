package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.details.mapper

import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Attachment
import net.gini.android.bank.sdk.transactiondocs.model.extractions.TransactionDoc

fun Attachment.toTransactionDoc() = TransactionDoc(
    giniApiDocumentId = id,
    documentFileName = filename
)

package net.gini.android.bank.sdk.transactiondocs.model.extractions

data class TransactionDoc(
    /**
     * Unique ID of the document **provided by Gini API**.
     */
    val giniApiDocumentId: String,
    /**
     * Document file name
     */
    val documentFileName: String,
)
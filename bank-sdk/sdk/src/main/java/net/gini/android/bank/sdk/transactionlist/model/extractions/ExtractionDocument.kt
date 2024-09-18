package net.gini.android.bank.sdk.transactionlist.model.extractions

data class ExtractionDocument(
    /**
     * Unique ID of the document **provided by Gini API**.
     */
    val giniApiDocumentId: String,
    /**
     * Document file name
     */
    val documentFileName: String,
)
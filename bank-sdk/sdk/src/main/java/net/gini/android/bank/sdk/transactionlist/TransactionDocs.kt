package net.gini.android.bank.sdk.transactionlist

import kotlinx.coroutines.flow.Flow
import net.gini.android.bank.sdk.transactionlist.model.extractions.ExtractionDocument

interface TransactionDocs {

    val configuration: TransactionDocsConfiguration

    val transactionDocsSettings: TransactionDocsSettings

    val extractionDocumentsFlow: Flow<List<ExtractionDocument>>
}


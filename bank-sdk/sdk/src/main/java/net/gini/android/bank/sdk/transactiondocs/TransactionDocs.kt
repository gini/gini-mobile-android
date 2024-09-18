package net.gini.android.bank.sdk.transactiondocs

import kotlinx.coroutines.flow.Flow
import net.gini.android.bank.sdk.transactiondocs.model.extractions.TransactionDoc

interface TransactionDocs {

    val configuration: TransactionDocsConfiguration

    val transactionDocsSettings: TransactionDocsSettings

    val extractionDocumentsFlow: Flow<List<TransactionDoc>>
}


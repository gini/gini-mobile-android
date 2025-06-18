package net.gini.android.bank.sdk.transactiondocs

import kotlinx.coroutines.flow.Flow
import net.gini.android.bank.sdk.transactiondocs.model.extractions.GiniTransactionDoc

interface GiniTransactionDocs {

    val transactionDocsSettings: TransactionDocsSettings

    val giniTransactionDocsFlow: Flow<List<GiniTransactionDoc>>

}


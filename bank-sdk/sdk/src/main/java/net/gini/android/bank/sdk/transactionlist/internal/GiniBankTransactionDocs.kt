package net.gini.android.bank.sdk.transactionlist.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.transactionlist.TransactionDocs
import net.gini.android.bank.sdk.transactionlist.TransactionDocsConfiguration
import net.gini.android.bank.sdk.transactionlist.model.extractions.ExtractionDocument

internal class GiniBankTransactionDocs internal constructor(
    override val configuration: TransactionDocsConfiguration,
    override val transactionDocsSettings: GiniTransactionDocsSettings,
    backgroundDispatcher: CoroutineDispatcher,
) : TransactionDocs {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    override val extractionDocumentsFlow: MutableStateFlow<List<ExtractionDocument>> =
        MutableStateFlow(listOf(
            ExtractionDocument("id", "document1.jpg"),
            ExtractionDocument("id", "document2.jpg"),
            ExtractionDocument("id", "document3.pdf"),
            ExtractionDocument("id", "document4.pdf"),
        ))

    internal suspend fun updateExtractionDocumentsBlocking(documents: List<ExtractionDocument>) {
        extractionDocumentsFlow.emit(documents)
    }

    fun updateExtractionDocuments(documents: List<ExtractionDocument>) = coroutineScope.launch {
        extractionDocumentsFlow.emit(documents)
    }
}
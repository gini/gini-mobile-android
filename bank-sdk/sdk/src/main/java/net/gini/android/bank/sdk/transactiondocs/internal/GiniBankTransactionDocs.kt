package net.gini.android.bank.sdk.transactiondocs.internal

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import net.gini.android.bank.sdk.transactiondocs.TransactionDocs
import net.gini.android.bank.sdk.transactiondocs.TransactionDocsConfiguration
import net.gini.android.bank.sdk.transactiondocs.model.extractions.TransactionDoc
import net.gini.android.bank.sdk.transactionlist.internal.GiniTransactionDocsSettings
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin

internal class GiniBankTransactionDocs internal constructor(
    override val configuration: TransactionDocsConfiguration,
    override val transactionDocsSettings: GiniTransactionDocsSettings,
    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider = getGiniCaptureKoin().get(),
) : TransactionDocs {

    fun deleteDocument(document: TransactionDoc) {
        lastAnalyzedDocumentProvider.clear()
    }

    override val extractionDocumentsFlow = lastAnalyzedDocumentProvider
        .data
        .filterNotNull()
        .map {
            listOf(
                TransactionDoc(
                    giniApiDocumentId = it.first,
                    it.second
                )
            )
        }
}
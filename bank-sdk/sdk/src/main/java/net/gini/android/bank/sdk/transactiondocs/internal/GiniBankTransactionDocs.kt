package net.gini.android.bank.sdk.transactiondocs.internal

import kotlinx.coroutines.flow.map
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.TransactionDocs
import net.gini.android.bank.sdk.transactiondocs.model.extractions.TransactionDoc
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider

internal class GiniBankTransactionDocs internal constructor(
    override val transactionDocsSettings: GiniTransactionDocsSettings =
        getGiniBankKoin().get(),
    private val attachedToTransactionDocumentProvider: AttachedToTransactionDocumentProvider =
        getGiniBankKoin().get()
) : TransactionDocs {

    @Suppress("UnusedParameter")
    fun deleteDocument(document: TransactionDoc) {
        attachedToTransactionDocumentProvider.clear()
    }

    override val extractionDocumentsFlow = attachedToTransactionDocumentProvider
        .data
        .map {
            listOfNotNull(it?.let {
                TransactionDoc(
                    giniApiDocumentId = it.giniApiDocumentId,
                    documentFileName = it.filename
                )
            })
        }
}

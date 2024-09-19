package net.gini.android.capture.analysis.transactiondoc

import kotlinx.coroutines.flow.Flow
import net.gini.android.capture.analysis.RemoteAnalyzedDocument

interface AttachedToTransactionDocumentProvider {

    val data: Flow<RemoteAnalyzedDocument?>

    fun provide(): RemoteAnalyzedDocument?

    fun update(document: RemoteAnalyzedDocument)

    fun clear()
}

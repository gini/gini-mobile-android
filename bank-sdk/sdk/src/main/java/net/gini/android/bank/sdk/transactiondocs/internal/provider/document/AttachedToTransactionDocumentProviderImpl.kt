package net.gini.android.bank.sdk.transactiondocs.internal.provider.document

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.analysis.RemoteAnalyzedDocument
import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider

internal class AttachedToTransactionDocumentProviderImpl(
    backgroundDispatcher: CoroutineDispatcher
) : AttachedToTransactionDocumentProvider {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    override val data: MutableStateFlow<RemoteAnalyzedDocument?> = MutableStateFlow(null)

    override fun provide(): RemoteAnalyzedDocument? = data.value

    override fun update(document: RemoteAnalyzedDocument) {
        coroutineScope.launch {
            data.emit(document)
        }
    }

    override fun clear() {
        coroutineScope.launch {
            data.emit(null)
        }
    }
}

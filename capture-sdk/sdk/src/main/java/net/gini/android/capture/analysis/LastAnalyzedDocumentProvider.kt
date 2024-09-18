package net.gini.android.capture.analysis

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.document.GiniCaptureDocument

class LastAnalyzedDocumentProvider(
    backgroundDispatcher: CoroutineDispatcher
) {

    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    val data: MutableStateFlow<Pair<String, String>?> = MutableStateFlow(null)

    fun provide(): Pair<String, String>? = data.value

    fun update(giniDocumentApiId: String, filename: String) {
        coroutineScope.launch { data.emit(giniDocumentApiId to filename) }
    }

    fun clear() {
        coroutineScope.launch { data.emit(null) }
    }
}

package net.gini.android.capture.analysis

class LastAnalyzedDocumentIdProvider {

    private var lastAnalyzedDocumentId: String? = null

    fun provide(): String? = lastAnalyzedDocumentId

    fun update(documentId: String) {
        lastAnalyzedDocumentId = documentId
    }

    fun clear() {
        lastAnalyzedDocumentId = null
    }
}

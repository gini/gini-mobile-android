package net.gini.android.capture.tracking.useranalytics.properties

sealed class UserAnalyticsEventSuperProperty(key: String, value: String) :
    AnalyticsKeyPairProperty(key, value) {
    data class GiniClientId(val clientId: String) :
        UserAnalyticsEventSuperProperty("gini_client_id", clientId)

    data class DocumentId(val documentId: String) :
        UserAnalyticsEventSuperProperty("document_id", documentId)

    data class EntryPoint(val entryPointType: EntryPointType) :
        UserAnalyticsEventSuperProperty("entry_point", entryPointType.analyticsName) {
        enum class EntryPointType(val analyticsName: String) {
            OPEN_WITH("open_with"), BUTTON("button"), FIELD("field")
        }
    }

    data class AnalyzedDocumentId(val documentId: String) :
        UserAnalyticsEventSuperProperty("document_id", documentId)
}
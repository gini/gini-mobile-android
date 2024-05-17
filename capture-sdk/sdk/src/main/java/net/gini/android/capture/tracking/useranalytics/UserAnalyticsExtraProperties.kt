package net.gini.android.capture.tracking.useranalytics

enum class UserAnalyticsExtraProperties(val propertyName: String) {
    DOCUMENT_TYPE("document_type"),
    PARTIAL_DOCUMENT_ID("partial_document_id"),
    DOCUMENT_PAGE_NUMBER("document_page_number"),
    ERROR_CODE("error_code"),
    ERROR_TYPE("error_type"),
    ERROR_MESSAGE("error_message"),
    FLASH_ACTIVE("flash_active"),
}
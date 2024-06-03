package net.gini.android.capture.tracking.useranalytics

enum class UserAnalyticsExtraProperties(val propertyName: String) {
    DOCUMENT_TYPE("document_type"),
    PARTIAL_DOCUMENT_ID("partial_document_id"),
    DOCUMENT_PAGE_NUMBER("document_page_number"),
    ERROR_CODE("error_code"),
    ERROR_TYPE("error_type"),
    ERROR_MESSAGE("error_message"),
    FLASH_ACTIVE("flash_active"),
    SCREEN("screen"),
    // region Help Screen
    HAS_CUSTOM_ITEMS("has_custom_items"),
    HELP_ITEMS("help_items"),
    ITEM_TAPPED("item_tapped"),
    // endregion
    CUSTOM_ONBOARDING_TITLE("custom_onboarding_title"),
    ONBOARDING_HAS_CUSTOM_ITEMS("has_custom_items"),

    // region Return Assistant
    SWITCH_ACTIVE("switch_active"),
    ITEMS_CHANGED("items_changed"),
    // endregion
}
package net.gini.android.capture.tracking.useranalytics


enum class UserAnalyticsEvent(val eventName: String) {
    SCREEN_SHOWN("screen_shown"),
    NEXT_STEP_TAPPED("next_step_tapped"),
    CLOSE_TAPPED("close_tapped"),
    GET_STARTED_TAPPED("get_started_tapped"),
    HELP_ITEM_TAPPED("help_item_tapped"),
    PAGE_SWIPED("page_swiped"),
    SKIP_TAPPED("skip_tapped"),
    CAPTURE_TAPPED("capture_tapped"),
    IMPORT_FILES_TAPPED("import_files_tapped"),
    UPLOAD_PHOTOS_TAPPED("upload_photos_tapped"),
    UPLOAD_DOCUMENTS_TAPPED("upload_documents_tapped"),
    FLASH_TAPPED("flash_tapped"),
    HELP_TAPPED("help_tapped"),
    MULTIPLE_PAGES_CAPTURED_TAPPED("multiple_pages_captured_tapped"),
    ERROR_DIALOG_SHOWN("error_dialog_shown"),
    QR_CODE_SCANNED("qr_code_scanned"),
    PROCEED_TAPPED("proceed_tapped"),
    ADD_PAGES_TAPPED("add_pages_tapped"),
    DELETE_PAGES_TAPPED("delete_pages_tapped"),
    ENTER_MANUALLY_TAPPED("enter_manually_tapped"),
    RETAKE_IMAGES_TAPPED("retake_images_tapped"),
    BACK_TO_CAMERA_TAPPED("back_to_camera_tapped"),
    DRAG_TO_DISMISS("drag_to_dismiss"),
    SAVE_TAPPED("save_tapped"),
    FULL_SCREEN_PAGE_TAPPED("full_screen_page_tapped"),
    PREVIEW_ZOOMED("preview_zoomed"),
    // region Return Assistant
    ITEM_SWITCH_TAPPED("item_switch_tapped"),
    EDIT_TAPPED("edit_tapped"),
    // endregion
    // region Camera Permission
    GIVE_ACCESS_TAPPED("give_access_tapped"),
    // endregion
    SDK_OPENED("sdk_opened"),
    SDK_CLOSED("sdk_closed"),
}
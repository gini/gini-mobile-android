package net.gini.android.capture.network

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.gini.android.capture.R

enum class ErrorType(@DrawableRes val drawableResource: Int,
                     @StringRes val titleTextResource: Int,
                     @StringRes val descriptionTextResource: Int
) {
    GENERAL(R.drawable.gc_alert_triangle_icon, R.string.gc_error_unexpected_title, R.string.gc_error_unexpected_text),
    NO_CONNECTION(R.drawable.gc_error_connection_icon, R.string.gc_error_connection_title, R.string.gc_error_connection_text),
    AUTH(R.drawable.gc_error_auth_icon, R.string.gc_error_auth_title, R.string.gc_error_auth_text),
    UPLOAD(R.drawable.gc_error_upload_icon, R.string.gc_error_upload_title, R.string.gc_error_upload_text),
    SERVER(R.drawable.gc_error_server_icon, R.string.gc_error_server_title, R.string.gc_error_server_text),
    FILE_IMPORT_GENERIC(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_import_generic_title, R.string.gc_error_file_import_generic_text),
    FILE_IMPORT_SIZE(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_import_size_title, R.string.gc_error_file_import_size_text),
    FILE_IMPORT_PAGE_COUNT(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_import_page_count_text, R.string.gc_error_file_import_page_count_text),
    FILE_IMPORT_UNSUPPORTED(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_import_unsupported_title, R.string.gc_error_file_import_unsupported_text),
    FILE_IMPORT_PASSWORD(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_import_password_title, R.string.gc_error_file_import_password_text),
    CUSTOM_VALIDATION(R.drawable.gc_alert_triangle_icon, R.string.gc_error_file_custom_validation_title, R.string.gc_error_file_custom_validation_text);

    fun typeFromError(error: Error): ErrorType {
        // TODO: handle no internet case

        if (error.statusCode == null) {
            return when (error.fileImportErrors) {
                FileImportErrors.GENERIC -> FILE_IMPORT_GENERIC
                FileImportErrors.SIZE -> FILE_IMPORT_SIZE
                FileImportErrors.PAGE -> FILE_IMPORT_PAGE_COUNT
                FileImportErrors.UNSUPPORTED -> FILE_IMPORT_UNSUPPORTED
                FileImportErrors.PASSWORD -> FILE_IMPORT_PASSWORD
                FileImportErrors.CUSTOM -> CUSTOM_VALIDATION
            }
        }

        error.statusCode?.let {
            if (it > 500) {
                return SERVER
            }

            if (it == 401) {
                return AUTH
            }

            if (it == 400 || (it in 402..498)) {
                return UPLOAD
            }

            return GENERAL
        }

        return GENERAL
    }
}
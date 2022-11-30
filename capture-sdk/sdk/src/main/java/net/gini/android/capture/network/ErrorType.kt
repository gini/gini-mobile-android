package net.gini.android.capture.network

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.gini.android.capture.R
import net.gini.android.capture.network.FileImportErrors.*
import java.net.UnknownHostException


enum class FileImportErrors {
    GENERIC, PAGE, SIZE, UNSUPPORTED, PASSWORD, CUSTOM
}

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

        if (error.cause != null && (error.cause is UnknownHostException)) {
            return NO_CONNECTION
        }

        if (error.statusCode == null) {
            return when (error.fileImportErrors) {
                GENERIC -> FILE_IMPORT_GENERIC
                SIZE -> FILE_IMPORT_SIZE
                PAGE -> FILE_IMPORT_PAGE_COUNT
                UNSUPPORTED -> FILE_IMPORT_UNSUPPORTED
                PASSWORD -> FILE_IMPORT_PASSWORD
                CUSTOM -> CUSTOM_VALIDATION
                else -> {
                    GENERAL
                }
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
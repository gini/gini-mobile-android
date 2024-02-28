package net.gini.android.capture.error

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.gini.android.capture.R
import net.gini.android.capture.document.GiniCaptureDocumentError
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.network.Error
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset

/**
 * Custom enum class to gather all resources needed to show an error that occured while using features of the SDK.
 */
enum class ErrorType(
    @DrawableRes val drawableResource: Int,
    @StringRes val titleTextResource: Int,
    @StringRes val descriptionTextResource: Int
) {
    GENERAL(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_unexpected_title,
        R.string.gc_error_unexpected_text
    ),
    NO_CONNECTION(
        R.drawable.gc_error_connection_icon,
        R.string.gc_error_connection_title,
        R.string.gc_error_connection_text
    ),
    AUTH(R.drawable.gc_error_auth_icon, R.string.gc_error_auth_title, R.string.gc_error_auth_text),
    UPLOAD(
        R.drawable.gc_error_upload_icon,
        R.string.gc_error_upload_title,
        R.string.gc_error_upload_text
    ),
    SERVER(
        R.drawable.gc_error_server_icon,
        R.string.gc_error_server_title,
        R.string.gc_error_server_text
    ),
    OUTAGE(
        R.drawable.gc_error_server_icon,
        R.string.gc_error_outage_title,
        R.string.gc_error_outage_text
    ),
    MAINTENANCE(
        R.drawable.gc_error_miantenance,
        R.string.gc_error_maintenance_title,
        R.string.gc_error_maintenance_text
    ),
    FILE_IMPORT_GENERIC(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_file_import_generic_title,
        R.string.gc_error_file_import_generic_text
    ),
    FILE_IMPORT_SIZE(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_file_import_size_title,
        R.string.gc_error_file_import_size_text
    ),
    FILE_IMPORT_PAGE_COUNT(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_file_import_page_count_title,
        R.string.gc_error_file_import_page_count_text
    ),
    FILE_IMPORT_UNSUPPORTED(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_file_import_unsupported_title,
        R.string.gc_error_file_import_unsupported_text
    ),
    FILE_IMPORT_PASSWORD(
        R.drawable.gc_alert_triangle_icon,
        R.string.gc_error_file_import_password_title,
        R.string.gc_error_file_import_password_text
    );

    companion object {
        private fun isInvalidUserError(error: Error): Boolean {
            val errorMessage = error.cause?.message ?: return false
            val responseJson = JSONObject(
                errorMessage?.let {
                    String(
                        it.toByteArray(),
                        Charset.forName("utf-8"))
                }
            )
            return responseJson[ERROR_KEY] == GRANT_VALUE
        }


        const val ERROR_KEY = "error"
        const val GRANT_VALUE = "invalid_grant"

        @JvmStatic
        fun typeFromError(error: Error): ErrorType {

            if (error.cause != null && (error.cause is UnknownHostException)) {
                return NO_CONNECTION
            }

            error.statusCode?.let {
                if (it == 500) {
                    return OUTAGE
                }
                if (it == 503) {
                    return MAINTENANCE
                }
                if (it > 500) {
                    return SERVER
                }

                if (it == 401) {
                    return AUTH
                }

                if (it == 400 && isInvalidUserError(error)) {
                    return AUTH
                }

                if (it == 400 || (it in 402..498)) {
                    return UPLOAD
                }

                return GENERAL
            }

            if (error.statusCode == null) {
                (error.cause as? SocketTimeoutException)?.let {
                    return UPLOAD
                }

                return when (error.fileImportErrors) {
                    FileImportValidator.Error.SIZE_TOO_LARGE -> FILE_IMPORT_SIZE
                    FileImportValidator.Error.TYPE_NOT_SUPPORTED -> FILE_IMPORT_UNSUPPORTED
                    FileImportValidator.Error.PASSWORD_PROTECTED_PDF -> FILE_IMPORT_PASSWORD
                    FileImportValidator.Error.TOO_MANY_PDF_PAGES -> FILE_IMPORT_PAGE_COUNT
                    FileImportValidator.Error.TOO_MANY_DOCUMENT_PAGES -> FILE_IMPORT_PAGE_COUNT
                    else -> {
                        GENERAL
                    }
                }
            }

            return GENERAL
        }

        @JvmStatic
        fun typeFromDocumentErrorCode(errorCode: GiniCaptureDocumentError.ErrorCode): ErrorType =
            when (errorCode) {
                GiniCaptureDocumentError.ErrorCode.UPLOAD_FAILED -> UPLOAD
                GiniCaptureDocumentError.ErrorCode.FILE_VALIDATION_FAILED -> FILE_IMPORT_GENERIC
            }
    }


}
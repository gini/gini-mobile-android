package net.gini.android.capture.tracking.useranalytics

import net.gini.android.capture.Document
import net.gini.android.capture.error.ErrorType

fun Boolean.mapToAnalyticsValue() = if (this) "yes" else "no"

fun Document.mapToAnalyticsDocumentType(): String {
    return when (this.getType()) {
        Document.Type.IMAGE, Document.Type.IMAGE_MULTI_PAGE -> "image"
        Document.Type.PDF, Document.Type.PDF_MULTI_PAGE -> "pdf"
        Document.Type.QRCode, Document.Type.QR_CODE_MULTI_PAGE -> "qrcode"
        else -> "unknown"
    }
}

fun ErrorType.mapToAnalyticsErrorType(): String {
    return when (this) {
        ErrorType.GENERAL -> "unknown"
        ErrorType.NO_CONNECTION -> "no_internet"
        ErrorType.AUTH -> "unauthorized"
        ErrorType.UPLOAD -> "upload"
        ErrorType.SERVER -> "server"
        ErrorType.OUTAGE -> "outage"
        ErrorType.MAINTENANCE -> "maintenance"
        ErrorType.FILE_IMPORT_GENERIC -> "file_import_generic"
        ErrorType.FILE_IMPORT_SIZE -> "file_import_size"
        ErrorType.FILE_IMPORT_PAGE_COUNT -> "file_import_page_count"
        ErrorType.FILE_IMPORT_UNSUPPORTED -> "file_import_unsupported"
        ErrorType.FILE_IMPORT_PASSWORD -> "file_import_password"
    }
}
package net.gini.android.capture.tracking.useranalytics

import net.gini.android.capture.Document
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty.DocumentType.Type as AnalyticsDocumentType
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty.ErrorType.Type as AnalyticsErrorType

fun Boolean.mapToAnalyticsValue() = if (this) "yes" else "no"

fun Document?.mapToAnalyticsDocumentType(): AnalyticsDocumentType {
    return when (this?.getType()) {
        Document.Type.IMAGE, Document.Type.IMAGE_MULTI_PAGE -> AnalyticsDocumentType.Image
        Document.Type.PDF, Document.Type.PDF_MULTI_PAGE -> AnalyticsDocumentType.Pdf
        Document.Type.QRCode, Document.Type.QR_CODE_MULTI_PAGE -> AnalyticsDocumentType.QrCode
        null -> AnalyticsDocumentType.Unknown
    }
}

fun ErrorType?.mapToAnalyticsErrorType(): AnalyticsErrorType {
    return when (this) {
        ErrorType.GENERAL, null -> AnalyticsErrorType.Unknown
        ErrorType.NO_CONNECTION -> AnalyticsErrorType.NoInternet
        ErrorType.AUTH -> AnalyticsErrorType.Unauthorized
        ErrorType.UPLOAD -> AnalyticsErrorType.Upload
        ErrorType.SERVER -> AnalyticsErrorType.Server
        ErrorType.OUTAGE -> AnalyticsErrorType.Outage
        ErrorType.MAINTENANCE -> AnalyticsErrorType.Maintenance
        ErrorType.FILE_IMPORT_GENERIC -> AnalyticsErrorType.FileImportGeneric
        ErrorType.FILE_IMPORT_SIZE -> AnalyticsErrorType.FileImportSize
        ErrorType.FILE_IMPORT_PAGE_COUNT -> AnalyticsErrorType.FileImportPageCount
        ErrorType.FILE_IMPORT_UNSUPPORTED -> AnalyticsErrorType.FileImportUnsupported
        ErrorType.FILE_IMPORT_PASSWORD -> AnalyticsErrorType.FileImportPassword
    }
}
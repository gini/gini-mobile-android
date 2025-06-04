package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.internal.storage.QrCodeEducationStorage

internal class GetQrEducationTypeUseCase(
    private val qrCodeEducationStorage: QrCodeEducationStorage,
    private val isOnlyQrCodeScanningEnabledProvider: () -> Boolean?,
    private val documentImportEnabledFileTypesProvider: () -> DocumentImportEnabledFileTypes?,
) {

    suspend fun execute(): QrEducationType? {
        if (isOnlyQrCodeScanningEnabledProvider() == true ||
            documentImportEnabledFileTypesProvider() == DocumentImportEnabledFileTypes.NONE
        ) {
            return null
        }

        return when (qrCodeEducationStorage.getQrCodeRecognitionCount().first()) {
            PHOTO_DOC_TYPE_VALUE -> QrEducationType.PHOTO_DOC
            UPLOAD_PICTURE_TYPE_VALUE -> QrEducationType.UPLOAD_PICTURE
            else -> null
        }
    }

    companion object {
        private const val PHOTO_DOC_TYPE_VALUE = 0
        private const val UPLOAD_PICTURE_TYPE_VALUE = 1
    }

}

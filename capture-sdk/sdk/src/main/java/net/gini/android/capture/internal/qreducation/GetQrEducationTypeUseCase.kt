package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.internal.qreducation.model.FlowType
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.internal.qreducation.model.QrEducationType
import net.gini.android.capture.internal.storage.FlowTypeStorage
import net.gini.android.capture.internal.storage.QrCodeEducationStorage

@Suppress("ReturnCount") // TODO Remove after https://github.com/gini/gini-mobile-android/pull/751/files will be merged
internal class GetQrEducationTypeUseCase(
    private val qrCodeEducationStorage: QrCodeEducationStorage,
    private val flowTypeStorage: FlowTypeStorage,
    private val isOnlyQrCodeScanningEnabledProvider: () -> Boolean?,
    private val documentImportEnabledFileTypesProvider: () -> DocumentImportEnabledFileTypes?,
) {

    suspend fun execute(): QrEducationType? {
        val flowType = flowTypeStorage.get()
        val qrCodeScanningOnly = isOnlyQrCodeScanningEnabledProvider() == true
        val documentImportDisabled =
            documentImportEnabledFileTypesProvider() == DocumentImportEnabledFileTypes.NONE
        val wrongFlowType = flowType == null || !ALLOWED_FLOW_TYPES.contains(flowType)

        val skipEducationConditions = listOf(
            qrCodeScanningOnly, documentImportDisabled, wrongFlowType
        )

        if (skipEducationConditions.any { it }) {
            return null
        }

        return when (qrCodeEducationStorage.getQrCodeRecognitionCount().first()) {
            PHOTO_DOC_TYPE_VALUE -> QrEducationType.PHOTO_DOC
            UPLOAD_PICTURE_TYPE_VALUE -> QrEducationType.UPLOAD_PICTURE
            else -> null
        }
    }

    companion object {
        private val ALLOWED_FLOW_TYPES = listOf(FlowType.Photo, FlowType.QrCode)
        private const val PHOTO_DOC_TYPE_VALUE = 0
        private const val UPLOAD_PICTURE_TYPE_VALUE = 1
    }

}

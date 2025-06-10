package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.internal.qreducation.model.FlowType
import net.gini.android.capture.internal.qreducation.model.InvoiceEducationType
import net.gini.android.capture.internal.storage.FlowTypeStorage
import net.gini.android.capture.internal.storage.InvoiceEducationStorage

internal class GetInvoiceEducationTypeUseCase(
    private val invoiceEducationStorage: InvoiceEducationStorage,
    private val flowTypeStorage: FlowTypeStorage,
) {

    suspend fun execute(): InvoiceEducationType? {

        val flowType = flowTypeStorage.get()

        if (flowType == null || !ALLOWED_FLOW_TYPES.contains(flowType)) {
            return null
        }

        val count = invoiceEducationStorage.getInvoiceRecognitionCount().first()
        return when {
            UPLOAD_PICTURE_TYPE_VALUE.contains(count) -> InvoiceEducationType.UPLOAD_PICTURE

            else -> null
        }
    }

    companion object {
        private val ALLOWED_FLOW_TYPES = listOf(FlowType.Photo, FlowType.QrCode)
        private val UPLOAD_PICTURE_TYPE_VALUE = 0..1
    }
}

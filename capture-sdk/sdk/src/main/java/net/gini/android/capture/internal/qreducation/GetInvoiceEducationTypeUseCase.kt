package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.internal.qreducation.model.InvoiceEducationType
import net.gini.android.capture.internal.storage.InvoiceEducationStorage

internal class GetInvoiceEducationTypeUseCase(
    private val invoiceEducationStorage: InvoiceEducationStorage,
) {

    suspend fun execute(): InvoiceEducationType? {
        val count = invoiceEducationStorage.getInvoiceRecognitionCount().first()
        return when {
            UPLOAD_PICTURE_TYPE_VALUE.contains(count) -> InvoiceEducationType.UPLOAD_PICTURE
            else -> null
        }
    }

    companion object {
        private val UPLOAD_PICTURE_TYPE_VALUE = 0..1
    }
}

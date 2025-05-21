package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.internal.storage.InvoiceEducationStorage

internal class IncrementInvoiceRecognizedCounterUseCase(
    private val invoiceEducationStorage: InvoiceEducationStorage,
) {

    suspend fun execute() {
        val count = invoiceEducationStorage.getInvoiceRecognitionCount().first()
        invoiceEducationStorage.setInvoiceRecognitionCount(count + 1)
    }
}

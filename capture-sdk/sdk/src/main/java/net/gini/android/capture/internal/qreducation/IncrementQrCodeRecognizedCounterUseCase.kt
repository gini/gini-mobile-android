package net.gini.android.capture.internal.qreducation

import kotlinx.coroutines.flow.first
import net.gini.android.capture.internal.storage.QrCodeEducationStorage

internal class IncrementQrCodeRecognizedCounterUseCase(
    private val qrCodeEducationStorage: QrCodeEducationStorage,
) {

    suspend fun execute() {
        val count = qrCodeEducationStorage.getQrCodeRecognitionCount().first()
        qrCodeEducationStorage.setQrCodeRecognitionCount(count + 1)
    }
}

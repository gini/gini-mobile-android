package net.gini.android.capture.camera

import kotlinx.coroutines.runBlocking
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.camera.view.education.qrcode.QRCodeEducationPopup
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

internal abstract class CameraFragmentExtension {

    lateinit var qrCodeEducationPopup: QRCodeEducationPopup<PaymentQRCodeData>
    lateinit var fragmentListener: CameraFragmentListener

    private val getQrEducationTypeUseCase:
            GetQrEducationTypeUseCase by getGiniCaptureKoin().inject()
    private val incrementQrCodeRecognizedCounterUseCase:
            IncrementQrCodeRecognizedCounterUseCase by getGiniCaptureKoin().inject()

    private fun runQrCodeEducationFlow(onComplete: () -> Unit) {
        val type = runBlocking { getQrEducationTypeUseCase.execute() }
        if (type != null) {
            qrCodeEducationPopup.show(type) {
                runBlocking { incrementQrCodeRecognizedCounterUseCase.execute() }
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    fun onQrCodeRecognized(
        extractions: Map<String, GiniCaptureSpecificExtraction>
    ) {
        hideImageCorners()
        runQrCodeEducationFlow {
            fragmentListener.onExtractionsAvailable(extractions)
        }
    }

    abstract fun hideImageCorners()
}

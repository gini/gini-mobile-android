package net.gini.android.capture.camera

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import net.gini.android.capture.internal.camera.view.QRCodePopup
import net.gini.android.capture.internal.camera.view.education.qrcode.QRCodeEducationPopup
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import net.gini.android.capture.internal.qreducation.GetQrEducationTypeUseCase
import net.gini.android.capture.internal.qreducation.IncrementQrCodeRecognizedCounterUseCase
import net.gini.android.capture.internal.qreducation.UpdateFlowTypeUseCase
import net.gini.android.capture.internal.qreducation.model.FlowType
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.education.GetEducationFeatureEnabledUseCase
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin

internal abstract class CameraFragmentExtension {

    @VisibleForTesting
    lateinit var qrCodeEducationPopup: QRCodeEducationPopup<PaymentQRCodeData>
    lateinit var fragmentListener: CameraFragmentListener
    val updateFlowTypeUseCase : UpdateFlowTypeUseCase by getGiniCaptureKoin().inject()
    lateinit var mPaymentQRCodePopup: QRCodePopup<PaymentQRCodeData>

    private val getQrEducationTypeUseCase:
            GetQrEducationTypeUseCase by getGiniCaptureKoin().inject()
    private val incrementQrCodeRecognizedCounterUseCase:
            IncrementQrCodeRecognizedCounterUseCase by getGiniCaptureKoin().inject()
    val getEInvoiceFeatureEnabledUseCase:
            GetEInvoiceFeatureEnabledUseCase by getGiniCaptureKoin().inject()
    private val getEducationFeatureEnabledUseCase:
            GetEducationFeatureEnabledUseCase by getGiniCaptureKoin().inject()
    private val giniBankConfigurationProvider:
            GiniBankConfigurationProvider by getGiniCaptureKoin().inject()
    private val unsupportedQrWarningSessionPin:
            UnsupportedQrWarningSessionPin by getGiniCaptureKoin().inject()
    private val educationMutex = Mutex()

    /**
     * Decides which unsupported-QR-code warning to show and pins that decision for the rest of
     * the capture session, so the warning type cannot change mid-session. Usually the pin is
     * already set by GiniCaptureViewModel from the first persisted configuration emission; the
     * provider read is only the fallback when a QR code is scanned before that emission arrives.
     */
    fun isUnsupportedQRCodeWarningEnabled(): Boolean =
        unsupportedQrWarningSessionPin.pinIfAbsent {
            giniBankConfigurationProvider.provide().isUnsupportedQRCodeWarningEnabled
        }

    fun showQrCodePopup(data: PaymentQRCodeData, onEducationFlowTriggered: () -> Unit) =
        runBlocking {
            updateFlowTypeUseCase.execute(FlowType.QrCode)
            val type = getQrEducationTypeUseCase.execute()
            if (type != null && getEducationFeatureEnabledUseCase.invoke()) {
                qrCodeEducationPopup.show(type) {
                    runBlocking {
                        incrementQrCodeRecognizedCounterUseCase.execute()
                        educationMutex.unlock()
                    }
                }
                educationMutex.lock()
                onEducationFlowTriggered()
            } else {
                mPaymentQRCodePopup.show(data)
            }
        }

    fun onQrCodeRecognized(
        extractions: Map<String, GiniCaptureSpecificExtraction>
    ) {
        hideImageCorners()
        CoroutineScope(Dispatchers.IO).launch {
            educationMutex.withLock {
                fragmentListener.onExtractionsAvailable(extractions)
            }
        }
    }

    abstract fun hideImageCorners()
}

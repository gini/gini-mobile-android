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
     * the capture session, so the warning type cannot change mid-session. This is the only pin
     * site: the decision is taken lazily from the provider's latest configuration when the first
     * warning is shown — pinning any earlier (e.g. on the first persisted-configuration emission)
     * would latch the previous session's cached value before the fresh remote configuration
     * arrives. GiniCaptureViewModel releases the pin when the session ends.
     *
     * In QR-code-scanning-only mode the new dialog's "Take photo of document" action would be
     * invalid (document capture is disabled), so the old yellow warning is pinned instead. The
     * mode is part of the pinned decision: switching modes via the dialog's own buttons happens
     * only after the first warning was shown, so it cannot change the warning type mid-session.
     */
    fun isUnsupportedQRCodeWarningEnabled(): Boolean =
        unsupportedQrWarningSessionPin.pinIfAbsent {
            giniBankConfigurationProvider.provide().isUnsupportedQRCodeWarningEnabled &&
                    !isOnlyQRCodeScanningEnabled()
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

    protected abstract fun isOnlyQRCodeScanningEnabled(): Boolean
}

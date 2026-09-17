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
import net.gini.android.capture.ingredientbrand.GetIngredientBrandVisibleUseCase
import net.gini.android.capture.ingredientbrand.IngredientBrandScreen
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
    private val getIngredientBrandVisibleUseCase:
            GetIngredientBrandVisibleUseCase by getGiniCaptureKoin().inject()
    private val educationMutex = Mutex()

    /**
     * Which of the two halves of the QR-code analysis step is currently running: `true` while the
     * QR-code education half is on screen, `false` for the invoice-retrieval half (and whenever no
     * step is running at all). Written only by [showQrCodePopup], which is the single entry point
     * of the step and runs on the main thread.
     */
    private var qrEducationStepRunning = false

    /**
     * Whether the Gini ingredient brand element must be shown on the camera screen.
     *
     * The camera screen is governed by the same [IngredientBrandScreen.ANALYSIS] entry as the
     * Analysis screen: scanning for a QR code is part of analysing the document, and the
     * approved design shows the badge on both. There is no separate backend screen name for it.
     */
    fun isIngredientBrandVisible(): Boolean =
        getIngredientBrandVisibleUseCase(IngredientBrandScreen.ANALYSIS)

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

    /**
     * Entry point of the QR-code analysis step and therefore the single place that decides which
     * of its two mutually exclusive halves runs: QR-code education below, invoice retrieval in the
     * `else` branch. R12 requires the ingredient brand element for both halves.
     *
     * The badge is deliberately *not* switched on here, before the branch. It tracks the surface
     * that covers the live preview, and the two halves put that surface up at different moments:
     *
     * - the **education** half covers the preview with a full-screen ComposeView in the same frame
     *   as `qrCodeEducationPopup.show(...)`, so the badge is raised right before that call;
     * - the **retrieval** half does *not* cover anything yet when `mPaymentQRCodePopup.show(data)`
     *   runs — `QRCodePopup.showViews()` only makes the dim background visible, and the shutter
     *   stays enabled and tappable for the popup's full delay. Interaction is disabled about a
     *   second later, in `CameraFragmentImpl.showActivityIndicatorAndDisableInteraction()`, and
     *   that is where this half raises the badge — the same instant the popup's own
     *   `progressViews()` used to. Raising it here instead would put a `screenReaderFocusable`
     *   badge over a usable shutter, which is exactly what R13 forbids.
     *
     * The halves also *end* at different times, which is what [isQrEducationStepRunning] exists
     * for: the retrieval overlay is gone as soon as the backend answers, while the education
     * overlay outlives that network call — `QRCodeEducationPopup.hide()` is never called and its
     * ComposeView stays visible past the 4.5 s animation, until navigation tears the screen down.
     * So the badge comes down in `CameraFragmentImpl.hideActivityIndicatorAndEnableInteraction()`
     * — the single moment the dim is removed and the shutter becomes usable again, and therefore
     * the one place every end of the retrieval half passes through, cancelled requests included —
     * but only when the education half is not the one running. See
     * `CameraFragmentImpl.setPoweredByGiniVisible` for every end point.
     */
    fun showQrCodePopup(data: PaymentQRCodeData, onEducationFlowTriggered: () -> Unit) =
        runBlocking {
            updateFlowTypeUseCase.execute(FlowType.QrCode)
            qrEducationStepRunning = false
            val type = getQrEducationTypeUseCase.execute()
            if (type != null && getEducationFeatureEnabledUseCase.invoke()) {
                // Set before show(): the overlay and the badge must go up in the same frame, and
                // the flag has to be readable by the time onEducationFlowTriggered() below starts
                // the analysis request on this half.
                qrEducationStepRunning = true
                if (isIngredientBrandVisible()) {
                    setPoweredByGiniVisible(true)
                }
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

    /**
     * Whether the currently running half of the QR-code analysis step is the education one.
     *
     * `CameraFragmentImpl` needs this to decide whether the ingredient brand element may come down
     * in `hideActivityIndicatorAndEnableInteraction()`, the moment the live preview becomes usable
     * again. On the education half the invoice-retrieval request runs too, so that method is
     * reached there as well, but the education overlay is still on screen when it answers — hiding
     * unguarded would leave the education content unbranded. Exposed as a
     * `protected` function rather than a property so the Java subclass calls it in the same shape
     * as [isOnlyQRCodeScanningEnabled].
     */
    protected fun isQrEducationStepRunning(): Boolean = qrEducationStepRunning

    /**
     * Sets the visibility of the Gini ingredient brand element.
     *
     * Implemented by `CameraFragmentImpl`, which binds `R.id.gc_powered_by_gini` and is the only
     * writer of it. This class does not resolve views itself — it only knows which half of the
     * QR-code analysis step is starting.
     */
    protected abstract fun setPoweredByGiniVisible(visible: Boolean)

    protected abstract fun isOnlyQRCodeScanningEnabled(): Boolean
}

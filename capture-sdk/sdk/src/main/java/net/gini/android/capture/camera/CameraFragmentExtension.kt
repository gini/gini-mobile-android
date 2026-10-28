package net.gini.android.capture.camera

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import net.gini.android.capture.ingredientbrand.IngredientBrandLoadingIndicatorAdapter
import net.gini.android.capture.ingredientbrand.IngredientBrandScreen
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.provider.UnsupportedQrWarningSessionPin
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.InjectedViewAdapterInstance

internal abstract class CameraFragmentExtension(
    /**
     * Dispatcher the QR-code step's waiters ([onQrCodeRecognized], [onQrCodeFallbackToUnsupported])
     * run on while they wait for the education carousel. Off the main thread, as the step has
     * always been: `Dispatchers.IO` in production, a `TestDispatcher` in tests. The Java subclass
     * `CameraFragmentImpl` passes both dispatchers explicitly; the defaults serve Kotlin subclasses.
     */
    qrStepDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Dispatcher [onQrCodeFallbackToUnsupported] hops to for its main-thread callback. */
    private val qrStepMainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

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

    /**
     * Owns every coroutine that waits for the QR-code education carousel — [onQrCodeRecognized]
     * and [onQrCodeFallbackToUnsupported]. The waiters used to be launched on throwaway
     * `CoroutineScope(...)` instances that nothing cancelled, so a carousel that never completed
     * (camera view destroyed mid-animation) left them suspended for the process lifetime, pinning
     * this fragment, its views and the host Activity. `CameraFragmentImpl.onDestroy` cancels this
     * scope through [cancelQrStepWaiters] (PP-3937).
     */
    private val qrStepScope = CoroutineScope(SupervisorJob() + qrStepDispatcher)

    /**
     * Completion signal of the education step started last on this fragment; `null` when the last
     * step was a plain retrieval or no step ran yet. Created per education step by
     * [showQrCodePopup] and completed by the carousel's completion callback, which the Compose
     * `LaunchedEffect` in `AnimatedEducationMessageWithIntro` fires after the 4.5 s animation.
     *
     * One instance per step on purpose. Destroying the camera view disposes the composition, so an
     * interrupted carousel never completes its signal, and the next step must not inherit that.
     * The previous `Mutex`-as-signal did exactly that: the lock taken for the interrupted step was
     * never released, and the next education step on the same fragment (back from the no-results
     * screen, scan again) blocked the main thread in `lock()` for good.
     *
     * Written on the main thread by [showQrCodePopup], read by the waiters on [qrStepScope].
     */
    @Volatile
    private var educationStepCompletion: CompletableDeferred<Unit>? = null

    /**
     * Held for the lifetime of the fragment rather than rebuilt per view creation.
     * [net.gini.android.capture.view.InjectedViewContainer] tracks adapter ownership through
     * [InjectedViewAdapterInstance.viewContainer]; handing it a fresh instance on every
     * `onCreateView` would break that bookkeeping across configuration changes.
     */
    private var cameraLoadingIndicatorInstance:
            InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>? = null

    /**
     * Whether the busy state currently on screen is the QR-code invoice retrieval.
     *
     * The camera raises one loading indicator for four different busy states — QR retrieval, the
     * client document check, a multi-file import and a freshly taken photo — but only the first is
     * the analysis step of the QR flow, and only that one carries the ingredient brand. Set by
     * `CameraFragmentImpl` immediately before it raises the indicator.
     */
    private var qrInvoiceRetrievalRunning = false

    fun setQrInvoiceRetrievalRunning(running: Boolean) {
        qrInvoiceRetrievalRunning = running
    }

    /**
     * The Camera screen's loading indicator.
     *
     * Always an [IngredientBrandLoadingIndicatorAdapter], which decides between the Gini brand
     * mark and the integrator's indicator every time it is shown. The Camera screen cannot decide
     * at view-creation time: `ingredientBrandScreens` arrives asynchronously and this is the first
     * screen the SDK opens, so the flag is still empty here on launch. It also cannot decide once
     * per view, because the indicator serves four busy states and only the QR retrieval is
     * branded.
     *
     * The Gini mark appears only while an invoice is retrieved for a scanned QR code — the
     * analysis step of the QR flow, before the Analysis screen opens. That is the one camera busy
     * state the ingredient brand covers, so it follows the same [IngredientBrandScreen.ANALYSIS]
     * flag rather than a separate one. The camera's other busy states (client document check,
     * multi-file import, freshly taken photo) keep the integrator's indicator.
     */
    fun loadingIndicatorAdapterInstance(
        integratorAdapter: CustomLoadingIndicatorAdapter,
    ): InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter> =
        cameraLoadingIndicatorInstance
            ?: InjectedViewAdapterInstance<CustomLoadingIndicatorAdapter>(
                IngredientBrandLoadingIndicatorAdapter(
                    isGiniMarkEnabled = {
                        qrInvoiceRetrievalRunning &&
                                getIngredientBrandVisibleUseCase(IngredientBrandScreen.ANALYSIS)
                    },
                    integratorAdapter = { integratorAdapter },
                )
            ).also { cameraLoadingIndicatorInstance = it }

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
     * Whether the client configuration enables Wero QR code scanning. Unlike
     * [isUnsupportedQRCodeWarningEnabled], this value is **not** pinned for the session: it is
     * read fresh from the provider every time a QR code is scanned, so a configuration refresh
     * mid-session takes effect on the next scan without needing a dedicated session reset.
     */
    fun isWeroQRCodeScanningEnabled(): Boolean =
        giniBankConfigurationProvider.provide().isWeroQRCodeScanningEnabled

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
            // A retrieval step has no carousel to wait for; a signal left behind by an interrupted
            // education step must not hold up this step's waiters either.
            educationStepCompletion = null
            val type = getQrEducationTypeUseCase.execute()
            if (type != null && getEducationFeatureEnabledUseCase.invoke()) {
                // Set before show(): the overlay and the badge must go up in the same frame, and
                // the flag has to be readable by the time onEducationFlowTriggered() below starts
                // the analysis request on this half.
                qrEducationStepRunning = true
                if (isIngredientBrandVisible()) {
                    setPoweredByGiniVisible(true)
                }
                val completion = CompletableDeferred<Unit>()
                educationStepCompletion = completion
                qrCodeEducationPopup.show(type) {
                    runBlocking {
                        incrementQrCodeRecognizedCounterUseCase.execute()
                    }
                    // Completes this step's own signal, not whatever the field holds by now: a
                    // callback from an earlier carousel can never release a later step's waiters.
                    completion.complete(Unit)
                }
                onEducationFlowTriggered()
            } else {
                // The approved design shows the brand element from the QR-detected state on
                // (frames 2.3.2 and 2.3.3 android-ph-QrDetected, 35002:11735/11756), not only
                // once the invoice starts loading. QRCodePopup makes the dim visible but nothing
                // there blocks input, so the controls are taken out of reach first: R13 forbids a
                // screenReaderFocusable badge over a shutter the user can still hit. Undone in
                // CameraFragmentImpl.hideActivityIndicatorAndEnableInteraction, whatever the
                // outcome of the step.
                blockInteractionForQrCodeStep()
                if (isIngredientBrandVisible()) {
                    setPoweredByGiniVisible(true)
                }
                mPaymentQRCodePopup.show(data)
            }
        }

    /**
     * Hands the extractions to [fragmentListener] once the education carousel — if this step
     * shows one — has completed. The listener is called on [qrStepScope]'s dispatcher, as before.
     */
    fun onQrCodeRecognized(
        extractions: Map<String, GiniCaptureSpecificExtraction>
    ) {
        hideImageCorners()
        qrStepScope.launch {
            awaitEducationStep()
            fragmentListener.onExtractionsAvailable(extractions)
        }
    }

    /**
     * Wero fallback counterpart of [onQrCodeRecognized]: waits for the education carousel so the
     * unsupported-QR popup never interrupts the 4.5 s animation (iOS parity).
     */
    fun onQrCodeFallbackToUnsupported(onMainThread: () -> Unit) {
        qrStepScope.launch {
            awaitEducationStep()
            withContext(qrStepMainDispatcher) { onMainThread() }
        }
    }

    /** Suspends until the current education step's carousel completed; returns at once without one. */
    private suspend fun awaitEducationStep() {
        educationStepCompletion?.await()
    }

    /**
     * Cancels every coroutine still waiting for the carousel. `CameraFragmentImpl.onDestroy` calls
     * it so nothing outlives the fragment: a waiter parked on an interrupted carousel would
     * otherwise keep this instance — and through it the fragment, its views and the Activity —
     * reachable for the rest of the process. After this, a late [onQrCodeRecognized] or
     * [onQrCodeFallbackToUnsupported] launches nothing, which is right for a destroyed screen.
     */
    fun cancelQrStepWaiters() {
        qrStepScope.cancel()
    }

    /** Whether a waiter launched by this extension is still suspended. */
    @VisibleForTesting
    internal fun hasPendingQrStepWaiters(): Boolean =
        qrStepScope.coroutineContext[Job]?.children?.any { it.isActive } == true

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
     * Whether the QR-code analysis step shows a loading indicator behind its overlay.
     *
     * Only the invoice-retrieval half does. The education half starts the same retrieval request,
     * but its full-screen message is what the user watches, so no indicator may run behind it —
     * the dim of that overlay let the indicator show through. This holds for every indicator (the
     * Gini mark, the integrator's, the default one) and matches iOS, where
     * `QRCodeOverlay.showAnimation` shows either the education view or the loading indicator.
     */
    protected fun shouldShowQrStepLoadingIndicator(): Boolean = !qrEducationStepRunning

    /**
     * Sets the visibility of the Gini ingredient brand element.
     *
     * Implemented by `CameraFragmentImpl`, which binds `R.id.gc_powered_by_gini` and is the only
     * writer of it. This class does not resolve views itself — it only knows which half of the
     * QR-code analysis step is starting.
     */
    protected abstract fun setPoweredByGiniVisible(visible: Boolean)

    /**
     * Puts the camera controls out of reach for the duration of the QR-code analysis step.
     *
     * Implemented by `CameraFragmentImpl`, which disables the controls, makes the dim swallow
     * touches and drops the control pane out of the accessibility tree. Needed because
     * `QRCodePopup.show` only makes the dim *visible* — a plain View that is not clickable does
     * not consume touches, so without this the shutter stays reachable underneath while the
     * brand element sits over it.
     */
    protected abstract fun blockInteractionForQrCodeStep()

    protected abstract fun isOnlyQRCodeScanningEnabled(): Boolean
}

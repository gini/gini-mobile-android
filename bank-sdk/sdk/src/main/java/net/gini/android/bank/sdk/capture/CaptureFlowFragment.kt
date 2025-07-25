package net.gini.android.bank.sdk.capture

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.gini.android.bank.sdk.BuildConfig
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragmentListener
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItemsValidator
import net.gini.android.bank.sdk.capture.digitalinvoice.args.ExtractionsResultData
import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoDataExtractor
import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoExtractionsHandler
import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoInvoiceHighlightsExtractor
import net.gini.android.bank.sdk.capture.skonto.SkontoFragment
import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.di.getGiniBankKoin
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocsFeatureEnabledUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogCancelAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.ui.dialog.attachdoc.AttachDocumentToTransactionDialog
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureFragmentDirections
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.util.protectViewFromInsets

class CaptureFlowFragment(private val openWithDocument: Document? = null) :
    Fragment(),
    GiniCaptureFragmentListener,
    DigitalInvoiceFragmentListener,
    SkontoFragmentListener,
    CancelListener {

    private lateinit var navController: NavController
    private lateinit var captureFlowFragmentListener: CaptureFlowFragmentListener

    private val skontoInvoiceHighlightsExtractor: SkontoInvoiceHighlightsExtractor
            by getGiniBankKoin().inject()
    private val skontoDataExtractor: SkontoDataExtractor
            by getGiniBankKoin().inject()
    private val skontoExtractionsHandler: SkontoExtractionsHandler
            by getGiniBankKoin().inject()
    private val transactionDocShouldBeAutoAttachedUseCase: GetTransactionDocShouldBeAutoAttachedUseCase
            by getGiniBankKoin().inject()
    private val transactionDocDialogCancelAttachUseCase: TransactionDocDialogCancelAttachUseCase
            by getGiniBankKoin().inject()
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase
            by getGiniBankKoin().inject()
    private val getTransactionDocsFeatureEnabledUseCase: GetTransactionDocsFeatureEnabledUseCase
            by getGiniBankKoin().inject()

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    private var willBeRestored = false
    private var didFinishWithResult = false
    private var attachDocumentDialogShowing = false
    private var captureResult: CaptureSDKResult.Success? = null
    private val attachToTransactionDialogStateKey = "attach_to_transaction_dialog_state_key"
    private val activityResultKey = "activity_result_key"

    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }

    private lateinit var composeView: ComposeView

    private fun setReturnReasonsEventProperty() {
        userAnalyticsEventTracker?.setUserProperty(
            setOf(
                UserAnalyticsUserProperty.ReturnReasonsEnabled(GiniBank.enableReturnReasons),
                UserAnalyticsUserProperty.BankSdkVersionName(BuildConfig.VERSION_NAME),
            )
        )
    }

    fun setListener(listener: CaptureFlowFragmentListener) {
        this.captureFlowFragmentListener = listener
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val contextThemeWrapper =
            ContextThemeWrapper(requireContext(), net.gini.android.capture.R.style.GiniCaptureTheme)
        return inflater.cloneInContext(contextThemeWrapper)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.gbs_fragment_capture_flow, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView = view.findViewById(R.id.gbs_compose_view)
        setReturnReasonsEventProperty()
        view.protectViewFromInsets()
        userAnalyticsEventTracker?.trackEvent(UserAnalyticsEvent.SDK_OPENED)
        navController = (childFragmentManager.fragments[0]).findNavController()
        restoreCaptureResultIfNeeded(savedInstanceState)
    }

    private fun restoreCaptureResultIfNeeded(savedInstanceState: Bundle?) {
        attachDocumentDialogShowing =
            savedInstanceState?.getBoolean(attachToTransactionDialogStateKey, false) ?: false
        if (attachDocumentDialogShowing.not()) return
        savedInstanceState?.let {
            captureResult =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    savedInstanceState.getParcelable(
                        activityResultKey,
                        CaptureSDKResult.Success::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    savedInstanceState.getParcelable(activityResultKey)
                }
        }
        captureResult?.let {
            processOnFinishedResultSuccessState(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory =
            CaptureFlowFragmentFactory(this, openWithDocument, this, this, this)
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        GiniBank.setGiniBankTerminationCallback {
            captureFlowFragmentListener.onFinishedWithResult(CaptureResult.Cancel)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(attachToTransactionDialogStateKey , attachDocumentDialogShowing)
        outState.putParcelable(activityResultKey , captureResult)
        willBeRestored = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!didFinishWithResult && !willBeRestored) {
            finishWithResult(CaptureResult.Cancel)
        }
        GiniBank.cleanupGiniBankTerminationCallback()
    }


    override fun onResume() {
        super.onResume()
        willBeRestored = false

        originalPrimaryNavigationFragment = parentFragmentManager.primaryNavigationFragment

        // To be the first to handle back button pressed events we need to set this fragment as the primary navigation fragment
        parentFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(this)
            .commit()
    }

    override fun onPause() {
        super.onPause()

        // We need to restore the primary navigation fragment to not break the client's fragment navigation.
        // Only restore the original primary navigation fragment if the client didn't change it in the meantime.
        if (parentFragmentManager.primaryNavigationFragment == this) {
            parentFragmentManager.beginTransaction()
                .setPrimaryNavigationFragment(originalPrimaryNavigationFragment)
                .commit()
        }
    }

    private fun processOnFinishedResultSuccessState(result: CaptureSDKResult.Success) {
        when {
            GiniBank.getCaptureConfiguration()?.returnAssistantEnabled == true -> {
                try {
                    tryShowingReturnAssistant(result)
                    return
                } catch (notUsed: DigitalInvoiceException) {
                    tryShowingSkontoScreen(result) {
                        tryShowAttachDocToTransactionDialog {
                            finishWithResult(interceptSuccessResult(result).toCaptureResult())
                        }
                    }
                    return
                }
            }

            GiniBank.getCaptureConfiguration()?.skontoEnabled == true -> {
                tryShowingSkontoScreen(result) {
                    tryShowAttachDocToTransactionDialog {
                        finishWithResult(interceptSuccessResult(result).toCaptureResult())
                    }
                }
                return
            }

            else -> {
                tryShowAttachDocToTransactionDialog {
                    finishWithResult(interceptSuccessResult(result).toCaptureResult())
                }

            }
        }
    }

    override fun onFinishedWithResult(result: CaptureSDKResult) {
        when (result) {
            is CaptureSDKResult.Success -> {
                captureResult = result
                processOnFinishedResultSuccessState(result)
            }

            else -> {
                finishWithResult(result.toCaptureResult())
            }
        }
    }

    private fun resetValuesForDialogState() {
        attachDocumentDialogShowing = false
        captureResult = null
    }

    private fun tryShowAttachDocToTransactionDialog(continueFlow: () -> Unit) {
        val autoAttachDoc = runBlocking { transactionDocShouldBeAutoAttachedUseCase() }
        if (!getTransactionDocsFeatureEnabledUseCase()) {
            continueFlow()
            return
        }
        composeView.setContent {
            GiniTheme {
                if (!autoAttachDoc) {
                    attachDocumentDialogShowing = true
                    AttachDocumentToTransactionDialog(onDismiss = {
                        lifecycleScope.launch { transactionDocDialogCancelAttachUseCase() }
                        continueFlow()
                        resetValuesForDialogState()
                    }, onConfirm = {
                        lifecycleScope.launch { transactionDocDialogConfirmAttachUseCase(it) }
                        continueFlow()
                        resetValuesForDialogState()
                    })
                } else {
                    continueFlow()
                    resetValuesForDialogState()
                }
            }
        }
    }

    private fun tryShowingReturnAssistant(result: CaptureSDKResult.Success) {
        LineItemsValidator.validate(result.compoundExtractions)
        val skontoData = kotlin.runCatching {
            val data = skontoDataExtractor.extractSkontoData(
                result.specificExtractions,
                result.compoundExtractions
            )
            SkontoData(
                skontoPercentageDiscounted = data.skontoPercentageDiscounted,
                skontoPaymentMethod = when (data.skontoPaymentMethod) {
                    SkontoData.SkontoPaymentMethod.Cash -> SkontoData.SkontoPaymentMethod.Cash
                    SkontoData.SkontoPaymentMethod.PayPal -> SkontoData.SkontoPaymentMethod.PayPal
                    else -> SkontoData.SkontoPaymentMethod.Unspecified
                },
                skontoAmountToPay = data.skontoAmountToPay,
                fullAmountToPay = data.fullAmountToPay,
                skontoRemainingDays = data.skontoRemainingDays,
                skontoDueDate = data.skontoDueDate
            )
        }.getOrNull()

        val highlightBoxes = kotlin.runCatching {
            skontoInvoiceHighlightsExtractor.extract(
                result.compoundExtractions
            )
        }.getOrNull() ?: emptyList()

        navController.navigate(
            GiniCaptureFragmentDirections.toDigitalInvoiceFragment(
                ExtractionsResultData(
                    specificExtractions = result.specificExtractions,
                    compoundExtractions = result.compoundExtractions,
                    returnReasons = result.returnReasons
                ),
                skontoData = skontoData,
                skontoInvoiceHighlights = highlightBoxes.toTypedArray(),
            )
        )
    }

    private fun tryShowingSkontoScreen(
        result: CaptureSDKResult.Success,
        fallback: () -> Unit
    ) {
        try {
            skontoExtractionsHandler.initialize(
                result.specificExtractions,
                result.compoundExtractions
            )

            val skontoData = skontoDataExtractor.extractSkontoData(
                result.specificExtractions,
                result.compoundExtractions
            )

            val highlightBoxes = skontoInvoiceHighlightsExtractor.extract(
                result.compoundExtractions
            )

            navController.navigate(
                GiniCaptureFragmentDirections.toSkontoFragment(
                    data = skontoData,
                    invoiceHighlights = highlightBoxes.toTypedArray(),
                )
            )
        } catch (e: Exception) {
            fallback()
        }
    }

    private fun finishWithResult(result: CaptureResult) {
        if (!ContextHelper.isTablet(requireContext())) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        didFinishWithResult = true
        captureFlowFragmentListener.onFinishedWithResult(result)
        trackSdkClosedEvent()
    }

    private fun interceptSuccessResult(result: CaptureSDKResult.Success): CaptureSDKResult {
        return if (result.specificExtractions.isEmpty() ||
            !pay5ExtractionsAvailable(result.specificExtractions) &&
            !epsPaymentAvailable(result.specificExtractions)
        ) {
            CaptureSDKResult.Empty
        } else {
            result
        }
    }

    private fun isPay5Extraction(extractionName: String): Boolean {
        return extractionName == "amountToPay" ||
                extractionName == "bic" ||
                extractionName == "iban" ||
                extractionName == "paymentReference" ||
                extractionName == "paymentRecipient"
    }

    private fun pay5ExtractionsAvailable(specificExtractions: Map<String, GiniCaptureSpecificExtraction>) =
        specificExtractions.keys.any { key -> isPay5Extraction(key) }

    private fun epsPaymentAvailable(specificExtractions: Map<String, GiniCaptureSpecificExtraction>) =
        specificExtractions.keys.contains("epsPaymentQRCodeUrl")

    override fun onPayInvoice(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    ) {
        finishWithResult(
            CaptureResult.Success(
                specificExtractions,
                compoundExtractions,
                emptyList()
            )
        )
    }


    override fun onPayInvoiceWithSkonto(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    ) {
        finishWithResult(
            CaptureResult.Success(
                specificExtractions,
                compoundExtractions,
                emptyList()
            )
        )
    }


    override fun onCancelFlow() {
        val popBackStack = navController.popBackStack()
        if (!popBackStack) {
            finishWithResult(CaptureResult.Cancel)
        }
    }

    internal companion object {
        fun createInstance(openWithDocument: Document? = null): CaptureFlowFragment {
            return CaptureFlowFragment(openWithDocument)
        }
    }

    private fun trackSdkClosedEvent() = runCatching {
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SDK_CLOSED,
            setOf(
                UserAnalyticsEventProperty.Status(UserAnalyticsEventProperty.Status.StatusType.Successful),
            )
        )
    }
}

interface CaptureFlowFragmentListener {
    fun onFinishedWithResult(result: CaptureResult)

    fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        callback.documentAccepted()
    }
}

class CaptureFlowFragmentFactory(
    private val giniCaptureFragmentListener: GiniCaptureFragmentListener,
    private var openWithDocument: Document? = null,
    private val digitalInvoiceListener: DigitalInvoiceFragmentListener,
    private val skontoListener: SkontoFragmentListener,
    private val cancelCallback: CancelListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            GiniCaptureFragment::class.java.name -> GiniCaptureFragment.createInstance(
                openWithDocument
            ) { openWithDocument = null }
                .apply {
                    setListener(
                        giniCaptureFragmentListener
                    )
                }

            DigitalInvoiceFragment::class.java.name -> DigitalInvoiceFragment().apply {
                listener = digitalInvoiceListener
                cancelListener = cancelCallback
            }

            SkontoFragment::class.java.name -> SkontoFragment().apply {
                skontoFragmentListener = skontoListener
                cancelListener = cancelCallback
            }

            else -> super.instantiate(classLoader, className)
        }
    }
}

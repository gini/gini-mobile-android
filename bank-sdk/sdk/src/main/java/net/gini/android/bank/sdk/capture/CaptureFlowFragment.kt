package net.gini.android.bank.sdk.capture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceFragmentListener
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItemsValidator
import net.gini.android.bank.sdk.capture.skonto.SkontoDataExtractor
import net.gini.android.bank.sdk.util.disallowScreenshots
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureFragment
import net.gini.android.capture.GiniCaptureFragmentDirections
import net.gini.android.capture.GiniCaptureFragmentListener
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.internal.util.CancelListener
import java.math.BigDecimal
import java.time.LocalDate

class CaptureFlowFragment(private val openWithDocument: Document? = null) :
    Fragment(),
    GiniCaptureFragmentListener,
    DigitalInvoiceFragmentListener,
    CancelListener {

    private lateinit var navController: NavController
    private lateinit var captureFlowFragmentListener: CaptureFlowFragmentListener

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    private var willBeRestored = false
    private var didFinishWithResult = false

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
        navController = (childFragmentManager.fragments[0]).findNavController()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory =
            CaptureFlowFragmentFactory(this, openWithDocument, this, this)
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        willBeRestored = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!didFinishWithResult && !willBeRestored) {
            captureFlowFragmentListener.onFinishedWithResult(CaptureResult.Cancel)
        }
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

    override fun onFinishedWithResult(result: CaptureSDKResult) {
        when (result) {
            is CaptureSDKResult.Success -> {
                if (GiniBank.getCaptureConfiguration()?.returnAssistantEnabled == true) {
                    try {
                        tryShowingReturnAssistant(result)
                    } catch (digitalInvoiceException: DigitalInvoiceException) {
                        tryShowingSkontoScreen(result)
                    }
                } else {
                    finishWithResult(result)
                }
            }

            else -> {
                didFinishWithResult = true
                captureFlowFragmentListener.onFinishedWithResult(result.toCaptureResult())
            }
        }
    }

    private fun tryShowingSkontoScreen(result: CaptureSDKResult.Success) {
        if (GiniBank.getCaptureConfiguration()?.skontoEnabled == true) {
            try {
                val skontoData = SkontoDataExtractor.extractSkontoData(
                    result.specificExtractions,
                    result.compoundExtractions
                )

                navController.navigate(
                    GiniCaptureFragmentDirections.toSkontoFragment(data = skontoData)
                )
            } catch (e: Exception) {
                finishWithResult(result)
            }
        }
    }

    private fun tryShowingReturnAssistant(result: CaptureSDKResult.Success) {
        LineItemsValidator.validate(result.compoundExtractions)
        navController.navigate(
            GiniCaptureFragmentDirections.toDigitalInvoiceFragment(
                DigitalInvoiceFragment.getExtractionsBundle(result.specificExtractions),
                DigitalInvoiceFragment.getCompoundExtractionsBundle(result.compoundExtractions),
                result.returnReasons.toTypedArray(),
                DigitalInvoiceFragment.getAmountsAreConsistentExtraction(result.specificExtractions)
            )
        )
    }

    private fun finishWithResult(result: CaptureSDKResult.Success) {
        didFinishWithResult = true
        captureFlowFragmentListener.onFinishedWithResult(interceptSuccessResult(result).toCaptureResult())
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
        didFinishWithResult = true
        captureFlowFragmentListener.onFinishedWithResult(
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
            didFinishWithResult = true
            captureFlowFragmentListener.onFinishedWithResult(CaptureResult.Cancel)
        }
    }

    internal companion object {
        fun createInstance(openWithDocument: Document? = null): CaptureFlowFragment {
            return CaptureFlowFragment(openWithDocument)
        }
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
    private val openWithDocument: Document? = null,
    private val digitalInvoiceListener: DigitalInvoiceFragmentListener,
    private val cancelCallback: CancelListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            GiniCaptureFragment::class.java.name -> GiniCaptureFragment(openWithDocument)
                .apply {
                    setListener(
                        giniCaptureFragmentListener
                    )
                }

            DigitalInvoiceFragment::class.java.name -> DigitalInvoiceFragment().apply {
                listener = digitalInvoiceListener
                cancelListener = cancelCallback
            }

            else -> super.instantiate(classLoader, className)
        }
    }
}

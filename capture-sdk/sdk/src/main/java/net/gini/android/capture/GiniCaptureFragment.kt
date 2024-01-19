package net.gini.android.capture

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.gini.android.capture.analysis.AnalysisActivity
import net.gini.android.capture.analysis.AnalysisFragment
import net.gini.android.capture.analysis.AnalysisFragmentCompatDirections
import net.gini.android.capture.analysis.AnalysisFragmentListener
import net.gini.android.capture.camera.CameraFragment
import net.gini.android.capture.camera.CameraFragmentDirections
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.document.GiniCaptureMultiPageDocument
import net.gini.android.capture.document.QRCodeDocument
import net.gini.android.capture.error.ErrorFragment
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboarding
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboardingAtFirstRun
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.noresults.NoResultsFragment
import net.gini.android.capture.review.multipage.MultiPageReviewFragment
import net.gini.android.capture.review.multipage.MultiPageReviewFragmentDirections
import net.gini.android.capture.review.multipage.MultiPageReviewFragmentListener

class GiniCaptureFragment(private val analysisIntent: Intent? = null) :
    Fragment(),
    CameraFragmentListener,
    MultiPageReviewFragmentListener,
    AnalysisFragmentListener,
    EnterManuallyButtonListener {

    internal companion object {
        fun createInstance(intent: Intent? = null): GiniCaptureFragment {
            return GiniCaptureFragment(intent)
        }
    }

    fun setListener(listener: GiniCaptureFragmentListener) {
        this.giniCaptureFragmentListener = listener
    }

    private lateinit var navController: NavController
    private lateinit var giniCaptureFragmentListener: GiniCaptureFragmentListener
    private lateinit var oncePerInstallEventStore: OncePerInstallEventStore

    // Related to navigation logic ported from CameraActivity
    private var addPages = false

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.gc_fragment_gini_capture, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = (childFragmentManager.fragments[0]).findNavController()
        oncePerInstallEventStore = OncePerInstallEventStore(requireContext())
        if (analysisIntent != null) {
            navController.navigate(
                CameraFragmentDirections.toAnalysisFragment(
                    analysisIntent.getParcelableExtra(
                        AnalysisActivity.EXTRA_IN_DOCUMENT
                    )!!, ""
                )
            )
        } else {
            if (shouldShowOnboarding() || (shouldShowOnboardingAtFirstRun() && !oncePerInstallEventStore.containsEvent(OncePerInstallEvent.SHOW_ONBOARDING))) {
                oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SHOW_ONBOARDING)
                navController.navigate(CameraFragmentDirections.toOnboardingFragment())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = CaptureFragmentFactory(this, this, this, this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

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

    //it shows the camera in the start

    //it handles navigation between camera, review, analysis screens and .... (this navigation should be inside capture)
    //capture-sdk should have its own fragment ->  GiniCaptureFragment

    // final goal of the flow without return assistant: client -> CaptureFlowFragment -> GiniCaptureFragment -> CaptureFlowFragment -> client
    // final goal of the flow with return assistant: client -> CaptureFlowFragment -> GiniCaptureFragment -> CaptureFlowFragment -> DigitalInvoiceFragment -> client


    //new listener onFinishedWithResult -> returns the result to the client

    override fun onProceedToAnalysisScreen(document: Document) {
        navController.navigate(CameraFragmentDirections.toAnalysisFragment(document, ""))
    }

    override fun onProceedToMultiPageReviewScreen(
        multiPageDocument: GiniCaptureMultiPageDocument<*, *>,
        shouldScrollToLastPage: Boolean
    ) {
        if (multiPageDocument.type == Document.Type.IMAGE_MULTI_PAGE) {
            if (addPages) {
                // In case we returned to take more images
                // Let the app know if it should scroll to the last position
//                val intent: Intent = Intent(requireActivity(), MultiPageReviewActivity::class.java)
//                intent.putExtra(MultiPageReviewActivity.SHOULD_SCROLL_TO_LAST_PAGE, shouldScrollToLastPage)
//                requireActivity().setResult(MultiPageReviewActivity.RESULT_SCROLL_TO_LAST_PAGE, intent)

                // For subsequent images a new CameraActivity was launched from the MultiPageReviewActivity
                // and so we can simply finish to return to the review activity
                navController.popBackStack()
            } else {
                // For the first image navigate to the review fragment by replacing the camera fragment to make
                // the review fragment the new start destination
//                val intent = MultiPageReviewActivity.createIntent(requireActivity(), shouldScrollToLastPage)
                navController.navigate(CameraFragmentDirections.toReviewFragmentForFirstPage())
            }
        } else {
            throw UnsupportedOperationException("Unsupported multi-page document type.")
        }
    }

    override fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        giniCaptureFragmentListener.onCheckImportedDocument(document, callback)
    }

    override fun onProceedToAnalysisScreen(document: GiniCaptureMultiPageDocument<*, *>) {
        navController.navigate(MultiPageReviewFragmentDirections.toAnalysisFragment(document, ""))
    }

    override fun onReturnToCameraScreenToAddPages() {
        // When returning to the camera screen for adding pages we navigate to a new CameraFragment instance
        navController.navigate(MultiPageReviewFragmentDirections.toCameraFragmentForAddingPages())
        addPages = true
    }

    override fun onReturnToCameraScreenForFirstPage() {
        // When returning to the camera screen for adding the first page we navigate back to the first CameraFragment instance
        navController.navigate(MultiPageReviewFragmentDirections.toCameraFragmentForFirstPage())
        addPages = false
    }

    override fun onImportedDocumentReviewCancelled() {
        // TODO: not needed anymore because this was called when the user deleted the last image imported via "open with"
        //       and since version 3.x we upload "open with" images directly (like PDFs) and users can't delete them
    }

    override fun onError(error: GiniCaptureError) {
        // TODO: launch the error fragment
        // TODO("Not yet implemented")
    }

    override fun onExtractionsAvailable(
        extractions: MutableMap<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: MutableMap<String, GiniCaptureCompoundExtraction>,
        returnReasons: MutableList<GiniCaptureReturnReason>
    ) {
        Log.d("analysis", "extractions received: $extractions")
        giniCaptureFragmentListener.onFinishedWithResult(
            CaptureSDKResult.Success(
                extractions,
                compoundExtractions,
                returnReasons
            )
        )
    }

    override fun onProceedToNoExtractionsScreen(document: Document) {
        NoResultsFragment.navigateToNoResultsFragment(
            navController,
            AnalysisFragmentCompatDirections.toNoResultsFragment(document)
        )
    }

    override fun onDefaultPDFAppAlertDialogCancelled() {
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Cancel)
    }

    override fun onExtractionsAvailable(extractions: MutableMap<String, GiniCaptureSpecificExtraction>) {

        Log.d("analysis", "extractions received: $extractions")

        giniCaptureFragmentListener.onFinishedWithResult(
            CaptureSDKResult.Success(
                extractions,
                emptyMap(),
                emptyList()
            )
        )
    }

    override fun noExtractionsFromQRCode(qrCodeDocument: QRCodeDocument) {
        // TODO: launch the no results fragment for qr codes
        // TODO("Not yet implemented")
    }

    override fun onEnterManuallyPressed() {
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.EnterManually)
    }

}

class CaptureFragmentFactory(
    private val cameraListener: CameraFragmentListener,
    private val multiPageReviewFragmentListener: MultiPageReviewFragmentListener,
    private val analysisFragmentListener: AnalysisFragmentListener,
    private val enterManuallyButtonListener: EnterManuallyButtonListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        when (className) {
            CameraFragment::class.java.name -> return CameraFragment().apply {
                setListener(
                    cameraListener
                )
            }

            MultiPageReviewFragment::class.java.name -> return MultiPageReviewFragment().apply {
                setListener(
                    multiPageReviewFragmentListener
                )
            }

            AnalysisFragment::class.java.name -> return AnalysisFragment()
                .apply {
                setListener(
                    analysisFragmentListener
                )
            }

            ErrorFragment::class.java.name -> return ErrorFragment().apply {
                setListener(
                    enterManuallyButtonListener
                )
            }

            NoResultsFragment::class.java.name -> return NoResultsFragment()
                .apply {
                    setListener(
                        enterManuallyButtonListener
                    )
                }

            else -> return super.instantiate(classLoader, className)
        }
    }
}

interface GiniCaptureFragmentListener {
    fun onFinishedWithResult(result: CaptureSDKResult)

    fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        callback.documentAccepted()
    }
}

package net.gini.android.capture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.gini.android.capture.analysis.AnalysisFragment
import net.gini.android.capture.analysis.AnalysisFragmentDirections
import net.gini.android.capture.analysis.AnalysisFragmentListener
import net.gini.android.capture.camera.CameraFragment
import net.gini.android.capture.camera.CameraFragmentDirections
import net.gini.android.capture.camera.CameraFragmentListener
import net.gini.android.capture.error.ErrorFragment
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboarding
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboardingAtFirstRun
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.noresults.NoResultsFragment
import net.gini.android.capture.review.multipage.MultiPageReviewFragment

class GiniCaptureFragment(private val openWithDocument: Document? = null) :
    Fragment(),
    CameraFragmentListener,
    AnalysisFragmentListener,
    EnterManuallyButtonListener{

    private lateinit var navController: NavController
    private lateinit var giniCaptureFragmentListener: GiniCaptureFragmentListener
    private lateinit var oncePerInstallEventStore: OncePerInstallEventStore

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    private var willBeRestored = false
    private var didFinishWithResult = false

    fun setListener(listener: GiniCaptureFragmentListener) {
        this.giniCaptureFragmentListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = CaptureFragmentFactory(this, this, this)
        super.onCreate(savedInstanceState)
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniCaptureTheme(inflater)
    }

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
        if (openWithDocument != null) {
            navController.navigate(
                CameraFragmentDirections.toAnalysisFragment(
                    openWithDocument,
                    ""
                )
            )
        } else {
            if (shouldShowOnboarding() || (shouldShowOnboardingAtFirstRun() && !oncePerInstallEventStore.containsEvent(OncePerInstallEvent.SHOW_ONBOARDING))) {
                oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SHOW_ONBOARDING)
                navController.navigate(CameraFragmentDirections.toOnboardingFragment())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        willBeRestored = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!didFinishWithResult && !willBeRestored) {
            giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Cancel)
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

    override fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        giniCaptureFragmentListener.onCheckImportedDocument(document, callback)
    }

    override fun onError(error: GiniCaptureError) {
        didFinishWithResult = true
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Error(error))
    }

    override fun onExtractionsAvailable(
        extractions: MutableMap<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: MutableMap<String, GiniCaptureCompoundExtraction>,
        returnReasons: MutableList<GiniCaptureReturnReason>
    ) {
        didFinishWithResult = true
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
            AnalysisFragmentDirections.toNoResultsFragment(document)
        )
    }

    override fun onDefaultPDFAppAlertDialogCancelled() {
        didFinishWithResult = true
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Cancel)
    }

    override fun onExtractionsAvailable(extractions: MutableMap<String, GiniCaptureSpecificExtraction>) {
        didFinishWithResult = true
        giniCaptureFragmentListener.onFinishedWithResult(
            CaptureSDKResult.Success(
                extractions,
                emptyMap(),
                emptyList()
            )
        )
    }

    override fun onEnterManuallyPressed() {
        didFinishWithResult = true
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.EnterManually)
    }

    companion object {
        @JvmStatic
        fun createInstance(document: Document? = null): GiniCaptureFragment {
            return GiniCaptureFragment(document)
        }
    }

}

class CaptureFragmentFactory(
    private val cameraListener: CameraFragmentListener,
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

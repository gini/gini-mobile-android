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
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.error.ErrorFragment
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboarding
import net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboardingAtFirstRun
import net.gini.android.capture.internal.util.disallowScreenshots
import net.gini.android.capture.internal.util.getLayoutInflaterWithGiniCaptureTheme
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.noresults.NoResultsFragment
import net.gini.android.capture.provider.LastExtractionsProvider
import net.gini.android.capture.review.multipage.MultiPageReviewFragment
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import net.gini.android.capture.tracking.useranalytics.tracker.AmplitudeUserAnalyticsEventTracker
import net.gini.android.capture.util.safeNavigate
import net.gini.android.capture.util.protectViewFromInsets
import java.util.UUID


class GiniCaptureFragment(
    private val openWithDocument: Document? = null,
    private val resetOpenWithDocument: () -> Unit
) :
    Fragment(),
    CameraFragmentListener,
    AnalysisFragmentListener,
    EnterManuallyButtonListener,
    CancelListener {

    private lateinit var navController: NavController
    private lateinit var giniCaptureFragmentListener: GiniCaptureFragmentListener
    private lateinit var oncePerInstallEventStore: OncePerInstallEventStore

    /**
     * we have a case, that we need to show the onboarding with every launch of capture
     * if our clients mark shouldShowOnboardingAtFirstRun as true, but when we
     * were rotating the screen, it was shown again with every orientation change.
     * To tackle this we need to store [onBoardingShown] in the savedInstanceState,
     * and retrieve it in onCreate.
     * */

    private var onBoardingShown = false
    private val onBoardingShownKey = "has_onboarding_shown"

    // Remember the original primary navigation fragment so that we can restore it when this fragment is detached
    private var originalPrimaryNavigationFragment: Fragment? = null

    private var willBeRestored = false
    private var didFinishWithResult = false

    private val userAnalyticsEventTracker by lazy { UserAnalytics.getAnalyticsEventTracker() }
    private val lastExtractionsProvider: LastExtractionsProvider by getGiniCaptureKoin().inject()
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider by
    getGiniCaptureKoin().inject()


    fun setListener(listener: GiniCaptureFragmentListener) {
        this.giniCaptureFragmentListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = CaptureFragmentFactory(
            cameraListener = this,
            analysisFragmentListener = this,
            enterManuallyButtonListener = this,
            cancelListener = this
        )
        super.onCreate(savedInstanceState)
        if (GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots) {
            requireActivity().window.disallowScreenshots()
        }
        onBoardingShown = savedInstanceState?.getBoolean(onBoardingShownKey, false) ?: false
        setupUserAnalytics()
    }

    private fun setCaptureVersionProperty() {
        userAnalyticsEventTracker?.setUserProperty(
            setOf(
                UserAnalyticsUserProperty.CaptureSdkVersionName(BuildConfig.VERSION_NAME),
            )
        )
    }

    private fun setupUserAnalytics() {
        if (GiniCapture.hasInstance()) {
            UserAnalytics.initialize(requireActivity())
            setCaptureVersionProperty()
            val networkRequestsManager =
                GiniCapture.getInstance().internal().networkRequestsManager
            val response = networkRequestsManager
                ?.getConfigurations(UUID.randomUUID())
            response?.thenAcceptAsync { res ->
                giniBankConfigurationProvider.update(res.configuration)

                UserAnalytics.setPlatformTokens(
                    AmplitudeUserAnalyticsEventTracker.AmplitudeAnalyticsApiKey(
                        res.configuration.amplitudeApiKey
                    ),
                    networkRequestsManager = networkRequestsManager
                )

                res.configuration.let {
                    setEventSuperProperties(it)
                    setUserEventProperties(it)
                }
            }
        }
    }

    private fun setUserEventProperties(configuration: Configuration) {
        userAnalyticsEventTracker?.setUserProperty(
            setOf(
                UserAnalyticsUserProperty.ReturnAssistantEnabled(
                    configuration.isReturnAssistantEnabled
                ),
                UserAnalyticsUserProperty.GiniClientId(
                    configuration.clientID
                ),
                UserAnalyticsUserProperty.InstantPaymentEnabled(
                    configuration.isInstantPaymentEnabled
                )
            )
        )
    }

    private fun setEventSuperProperties(configuration: Configuration) {
        userAnalyticsEventTracker?.setEventSuperProperty(
            UserAnalyticsEventSuperProperty.GiniClientId(configuration.clientID)
        )
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
        setAnalyticsEntryPointProperty(openWithDocument != null)
        view.protectViewFromInsets()
        if (openWithDocument != null) {
            navController.navigate(
                CameraFragmentDirections.toAnalysisFragment(
                    openWithDocument,
                    ""
                )
            )
            resetOpenWithDocument()
        } else {
            if (shouldShowOnboarding() || (shouldShowOnboardingAtFirstRun() && !oncePerInstallEventStore.containsEvent(
                    OncePerInstallEvent.SHOW_ONBOARDING
                ))
            ) {
                showOnboardingScreen()
            }
        }
    }

    private fun showOnboardingScreen() {
        if (!onBoardingShown) {
            onBoardingShown = true
            oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SHOW_ONBOARDING)
            safeNavigate(navController, CameraFragmentDirections.toOnboardingFragment())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(onBoardingShownKey, onBoardingShown)
        willBeRestored = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!didFinishWithResult && !willBeRestored) {
            giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Cancel)
        }
        if (willBeRestored) {
            UserAnalytics.flushEvents()
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
        lastExtractionsProvider.update(extractions)
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
        finishWithCancel()
    }

    override fun onExtractionsAvailable(extractions: MutableMap<String, GiniCaptureSpecificExtraction>) {
        didFinishWithResult = true
        lastExtractionsProvider.update(extractions)
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

    override fun onCancelFlow() {
        finishWithCancel()
    }

    private fun finishWithCancel() {
        didFinishWithResult = true
        giniCaptureFragmentListener.onFinishedWithResult(CaptureSDKResult.Cancel)
    }

    private fun setAnalyticsEntryPointProperty(isOpenWithDocumentExists: Boolean) {
        val entryPointProperty = if (isOpenWithDocumentExists) {
            UserAnalyticsEventSuperProperty.EntryPoint(UserAnalyticsEventSuperProperty.EntryPoint.EntryPointType.OPEN_WITH)
        } else {
            UserAnalyticsEventSuperProperty.EntryPoint(
                when (GiniCapture.getInstance().entryPoint) {
                    EntryPoint.BUTTON -> UserAnalyticsEventSuperProperty.EntryPoint.EntryPointType.BUTTON
                    EntryPoint.FIELD -> UserAnalyticsEventSuperProperty.EntryPoint.EntryPointType.FIELD
                }
            )
        }
        userAnalyticsEventTracker?.setEventSuperProperty(entryPointProperty)
    }

    companion object {

        @JvmStatic
        fun createInstance(document: Document? = null): GiniCaptureFragment {
            return GiniCaptureFragment(document) { }
        }

        @JvmStatic
        fun createInstance(
            document: Document? = null,
            resetOpenWithDocument: () -> Unit
        ): GiniCaptureFragment {
            return GiniCaptureFragment(document, resetOpenWithDocument)
        }
    }
}

class CaptureFragmentFactory(
    private val cameraListener: CameraFragmentListener,
    private val analysisFragmentListener: AnalysisFragmentListener,
    private val enterManuallyButtonListener: EnterManuallyButtonListener,
    private val cancelListener: CancelListener
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        when (className) {
            CameraFragment::class.java.name -> return CameraFragment().apply {
                setListener(
                    cameraListener
                )
                setCancelListener(cancelListener)
            }

            AnalysisFragment::class.java.name -> return AnalysisFragment()
                .apply {
                    setListener(
                        analysisFragmentListener
                    )
                    setCancelListener(cancelListener)
                }

            ErrorFragment::class.java.name -> return ErrorFragment().apply {
                setListener(
                    listener = enterManuallyButtonListener,
                )
                setCancelListener(
                    cancelListener = cancelListener
                )
            }

            NoResultsFragment::class.java.name -> return NoResultsFragment()
                .apply {
                    setListeners(
                        enterManuallyButtonListener
                    )
                    setCancelListener(
                        cancelListener
                    )
                }

            MultiPageReviewFragment::class.java.name -> return MultiPageReviewFragment().apply {
                setCancelListener(cancelListener)
            }

            else -> return super.instantiate(classLoader, className)
        }
    }
}

interface GiniCaptureFragmentListener {
    fun onFinishedWithResult(
        result: CaptureSDKResult
    )

    fun onCheckImportedDocument(
        document: Document,
        callback: CameraFragmentListener.DocumentCheckResultCallback
    ) {
        callback.documentAccepted()
    }
}

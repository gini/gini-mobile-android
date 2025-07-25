package net.gini.android.capture.error

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import net.gini.android.capture.Document
import net.gini.android.capture.EnterManuallyButtonListener
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.error.view.ErrorNavigationBarBottomAdapter
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper
import net.gini.android.capture.tracking.useranalytics.UserAnalytics.getAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsDocumentType
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsErrorType
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty
import net.gini.android.capture.view.InjectedViewAdapterHolder
import net.gini.android.capture.view.InjectedViewContainer
import net.gini.android.capture.view.NavButtonType
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Main logic implementation for error handling UI presented by {@link ErrorActivity}.
 * Internal use only.
 */
class ErrorFragmentImpl(
    private val fragmentCallback: FragmentImplCallback,
    // CancelListener should be removed in the next major version - not a breaking change but better to keep it for now
    @Suppress("UnusedPrivateProperty")
    private val cancelListener: CancelListener,
    private val document: Document?,
    private val errorType: ErrorType?,
    private val customError: String?
) {

    private val defaultListener: EnterManuallyButtonListener = EnterManuallyButtonListener { }
    private lateinit var view: View
    private var enterManuallyButtonListener: EnterManuallyButtonListener? = null
    private lateinit var retakeImagesButton: Button
    private var mUserAnalyticsEventTracker: UserAnalyticsEventTracker? = null
    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Error

    fun onCreate(savedInstanceState: Bundle?) {
        // Clear the image from the memory store because the user can only exit for manual entry or in some cases
        // can go back to the camera to take new pictures
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.clear()
        }
        mUserAnalyticsEventTracker = getAnalyticsEventTracker()
    }

    fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.gc_fragment_error, container, false)
        retakeImagesButton = view.findViewById(R.id.gc_button_error_retake_images)
        handleOnBackPressed()
        addUserAnalyticEvents()

        setupTopBarNavigation()
        setupBottomBarNavigation()
        if (shouldAllowRetakeImages()) {
            retakeImagesButton.setIntervalClickListener {
                EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY)
                navigateToCameraScreen(UserAnalyticsEvent.BACK_TO_CAMERA_TAPPED)
            }
        } else {
            retakeImagesButton.visibility = View.GONE
        }

        val enterManuallyButton = view.findViewById<View>(R.id.gc_button_error_enter_manually)
        enterManuallyButton.setIntervalClickListener {
            mUserAnalyticsEventTracker?.trackEvent(
                UserAnalyticsEvent.ENTER_MANUALLY_TAPPED,
                setOf(UserAnalyticsEventProperty.Screen(screenName))
            )
            enterManuallyButtonListener?.onEnterManuallyPressed()
        }

        customError?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text = it
        }

        errorType?.let {
            view.findViewById<TextView>(R.id.gc_error_header).text =
                fragmentCallback.activity?.getString(it.titleTextResource)
            view.findViewById<TextView>(R.id.gc_error_textview).text =
                fragmentCallback.activity?.getString(it.descriptionTextResource)
            view.findViewById<ImageView>(R.id.gc_error_header_icon)
                .setImageResource(it.drawableResource)
        }

        return view
    }

    private fun addUserAnalyticEvents() {
        val errorMessage =
            customError ?: fragmentCallback.activity?.getString(errorType?.titleTextResource ?: 0)
                .toString()
        mUserAnalyticsEventTracker?.setEventSuperProperty(
            UserAnalyticsEventSuperProperty.DocumentType(document.mapToAnalyticsDocumentType())
        )
        mUserAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.DocumentId(document?.id.toString()),
                UserAnalyticsEventProperty.ErrorType(errorType.mapToAnalyticsErrorType()),
                UserAnalyticsEventProperty.ErrorMessage(errorMessage)
            ),
        )
    }

    private fun setupTopBarNavigation() {
        val topBarContainer =
            view.findViewById<InjectedViewContainer<NavigationBarTopAdapter>>(R.id.gc_injected_navigation_bar_container_top)
        if (GiniCapture.hasInstance()) {
            topBarContainer.injectedViewAdapterHolder =
                InjectedViewAdapterHolder(
                    GiniCapture.getInstance().internal().navigationBarTopAdapterInstance
                ) { injectedViewAdapter ->
                    val navType = if (GiniCapture.getInstance().isBottomNavigationBarEnabled)
                        NavButtonType.NONE else NavButtonType.BACK
                    injectedViewAdapter.setNavButtonType(navType)
                    injectedViewAdapter.setTitle(
                        fragmentCallback.activity?.getString(R.string.gc_title_error) ?: ""
                    )
                    injectedViewAdapter.setOnNavButtonClickListener(IntervalClickListener {
                        navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED)
                    })
                }
        }
    }

    private fun setupBottomBarNavigation() {
        val topBarContainer =
            view.findViewById<InjectedViewContainer<ErrorNavigationBarBottomAdapter>>(
                R.id.gc_injected_navigation_bar_container_bottom
            )

        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled) {
            topBarContainer.injectedViewAdapterHolder = InjectedViewAdapterHolder(
                GiniCapture.getInstance().internal().errorNavigationBarBottomAdapterInstance
            ) { injectedViewAdapter ->
                injectedViewAdapter.setOnBackClickListener(IntervalClickListener {
                    navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED)
                })
            }
        }
    }

    private fun handleOnBackPressed() {
        fragmentCallback.getActivity()?.onBackPressedDispatcher
            ?.addCallback(
                fragmentCallback.getViewLifecycleOwner(),
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED)
                    }
                })
    }

    private fun navigateToCameraScreen(event: UserAnalyticsEvent) {
        mUserAnalyticsEventTracker?.trackEvent(
            event,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
        fragmentCallback.findNavController()
            .navigate(ErrorFragmentDirections.toCameraFragment())
    }

    fun setListener(enterManuallyButtonListener: EnterManuallyButtonListener?) {
        this.enterManuallyButtonListener = enterManuallyButtonListener ?: defaultListener
    }

    private fun shouldAllowRetakeImages(): Boolean {
        if (document == null) {
            retakeImagesButton.text =
                fragmentCallback.activity?.getString(R.string.gc_error_back_to_camera)
            return true
        }

        if (document is ImageMultiPageDocument) {
            var isImportedDocFound = false
            var i = 0
            while (!isImportedDocFound && i < document.documents.size) {
                isImportedDocFound = !isDocumentFromCameraScreen(document.documents[i])
                i++
            }
            return !isImportedDocFound
        }

        return isDocumentFromCameraScreen(document)
    }

    private fun isDocumentFromCameraScreen(document: Document): Boolean {
        return document.importMethod != Document.ImportMethod.OPEN_WITH && document.source.name == "camera"
    }
}

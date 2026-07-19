package net.gini.android.capture.noresults

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.analysis.ConsumableEvent
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsDocumentType
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty

/**
 * ViewModel for the no results screen.
 *
 * Contains the back/retake/enter manually decisions and the analytics tracking which used to
 * live in [NoResultsFragmentImpl]. The fragment renders the [uiState] and executes the one-shot
 * [events].
 *
 * Internal use only.
 */
internal class NoResultsViewModel(private val document: Document) : ViewModel() {

    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.NoResults

    private val mutableUiState = MutableLiveData(
        NoResultsUiState(
            allowRetakeImages = shouldAllowRetakeImages(),
            showQrCodeTitle = document.type == Document.Type.QRCode ||
                    document.type == Document.Type.QR_CODE_MULTI_PAGE
        )
    )

    /**
     * What the no results screen renders.
     */
    val uiState: LiveData<NoResultsUiState> = mutableUiState

    private val mutableEvents = MutableLiveData<ConsumableEvent<NoResultsViewEvent>>()

    /**
     * One-shot commands executed by the fragment.
     */
    val events: LiveData<ConsumableEvent<NoResultsViewEvent>> = mutableEvents

    init {
        // Clear the image from the memory store because the user can only exit for manual entry
        // or in some cases can go back to the camera to take new pictures
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.clear()
        }
    }

    /**
     * Tracks the screen shown analytics events. Must be called from the fragment's
     * `onCreateView()` (the events were tracked there before the MVVM migration as well).
     */
    fun onScreenShown() {
        val userAnalyticsEventTracker = UserAnalytics.getAnalyticsEventTracker() ?: return
        userAnalyticsEventTracker.setEventSuperProperty(
            UserAnalyticsEventSuperProperty.DocumentType(document.mapToAnalyticsDocumentType())
        )
        userAnalyticsEventTracker.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.DocumentId(document.id)
            )
        )
    }

    fun onRetakeImagesClicked() {
        EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY)
        navigateToCameraScreen(UserAnalyticsEvent.RETAKE_IMAGES_TAPPED)
    }

    fun onEnterManuallyClicked() {
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(
            UserAnalyticsEvent.ENTER_MANUALLY_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
        emitEvent(NoResultsViewEvent.EnterManually)
    }

    fun onCloseClicked() {
        navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED)
    }

    private fun navigateToCameraScreen(event: UserAnalyticsEvent) {
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(
            event,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
        emitEvent(NoResultsViewEvent.NavigateToCamera)
    }

    private fun shouldAllowRetakeImages(): Boolean {
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
        return document.importMethod != Document.ImportMethod.OPEN_WITH &&
                document.source.name == "camera"
    }

    private fun emitEvent(event: NoResultsViewEvent) {
        mutableEvents.value = ConsumableEvent(event)
    }

    class Factory(private val document: Document) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NoResultsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return NoResultsViewModel(document) as T
        }
    }
}

/**
 * What the no results screen renders.
 *
 * Internal use only.
 */
internal data class NoResultsUiState(
    val allowRetakeImages: Boolean,
    val showQrCodeTitle: Boolean,
)

/**
 * One-shot commands executed by [NoResultsFragment].
 *
 * Internal use only.
 */
internal sealed interface NoResultsViewEvent {
    object NavigateToCamera : NoResultsViewEvent
    object EnterManually : NoResultsViewEvent
}

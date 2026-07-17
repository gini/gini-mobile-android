package net.gini.android.capture.error

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.EventTrackingHelper
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsDocumentType
import net.gini.android.capture.tracking.useranalytics.mapToAnalyticsErrorType
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventSuperProperty

/**
 * ViewModel for the error screen.
 *
 * Contains the error type/message resolution, the retake and enter manually decisions and the
 * analytics tracking which used to live in the former `ErrorFragmentImpl`. The fragment renders
 * the [uiState] and executes the one-shot [sideEffects].
 *
 * Internal use only.
 */
internal class ErrorViewModel(
    private val app: Application,
    private val document: Document?,
    private val errorType: ErrorType?,
    private val customError: String?
) : ViewModel() {

    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.Error

    private val _uiState = MutableStateFlow(
        ErrorUiState(
            errorType = errorType,
            customError = customError,
            allowRetakeImages = shouldAllowRetakeImages(),
            useBackToCameraButtonText = document == null
        )
    )
    val uiState: StateFlow<ErrorUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<ErrorSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ErrorSideEffect> = _sideEffects.receiveAsFlow()

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
        val userAnalyticsEventTracker = UserAnalytics.getAnalyticsEventTracker()
        val errorMessage =
            customError ?: app.getString(errorType?.titleTextResource ?: 0)
        userAnalyticsEventTracker?.setEventSuperProperty(
            UserAnalyticsEventSuperProperty.DocumentType(document.mapToAnalyticsDocumentType())
        )
        userAnalyticsEventTracker?.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOf(
                UserAnalyticsEventProperty.Screen(screenName),
                UserAnalyticsEventProperty.DocumentId(document?.id.toString()),
                UserAnalyticsEventProperty.ErrorType(errorType.mapToAnalyticsErrorType()),
                UserAnalyticsEventProperty.ErrorMessage(errorMessage)
            ),
        )
    }

    fun onRetakeImagesClicked() {
        EventTrackingHelper.trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY)
        navigateToCameraScreen(UserAnalyticsEvent.BACK_TO_CAMERA_TAPPED)
    }

    fun onEnterManuallyClicked() {
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(
            UserAnalyticsEvent.ENTER_MANUALLY_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
        sendSideEffect(ErrorSideEffect.EnterManually)
    }

    fun onCloseClicked() {
        navigateToCameraScreen(UserAnalyticsEvent.CLOSE_TAPPED)
    }

    private fun navigateToCameraScreen(event: UserAnalyticsEvent) {
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(
            event,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
        sendSideEffect(ErrorSideEffect.NavigateToCamera)
    }

    private fun shouldAllowRetakeImages(): Boolean {
        if (document == null) {
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

    private fun sendSideEffect(sideEffect: ErrorSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }
}

/**
 * What the error screen renders.
 *
 * Internal use only.
 */
internal data class ErrorUiState(
    val errorType: ErrorType?,
    val customError: String?,
    val allowRetakeImages: Boolean,
    val useBackToCameraButtonText: Boolean,
)

/**
 * One-shot side effects executed by [ErrorFragment].
 *
 * Internal use only.
 */
internal sealed interface ErrorSideEffect {
    object NavigateToCamera : ErrorSideEffect
    object EnterManually : ErrorSideEffect
}

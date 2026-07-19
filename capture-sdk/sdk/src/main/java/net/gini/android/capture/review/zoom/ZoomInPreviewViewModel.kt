package net.gini.android.capture.review.zoom

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.capture.AsyncCallback
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.analysis.ConsumableEvent
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.internal.camera.photo.Photo
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

/**
 * ViewModel for the zoom in preview screen.
 *
 * Contains the document rendering orchestration (loading the photo from the memory cache) and
 * the close decision with its analytics tracking which used to live in [ZoomInPreviewFragment].
 * The zoom/touch view handling stays in the fragment.
 *
 * Internal use only.
 */
internal class ZoomInPreviewViewModel(
    private val app: Application,
    private val imageDocument: ImageDocument?,
) : ViewModel() {

    private val screenName: UserAnalyticsScreen = UserAnalyticsScreen.ReviewZoom

    private val mutablePreview = MutableLiveData<ZoomInPreviewUiState>()

    /**
     * The rendered document preview which the fragment shows in the zoomable image view.
     */
    val preview: LiveData<ZoomInPreviewUiState> = mutablePreview

    private val mutableCloseEvent = MutableLiveData<ConsumableEvent<Unit>>()

    /**
     * One-shot event signalling that the fragment should pop the back stack.
     */
    val closeEvent: LiveData<ConsumableEvent<Unit>> = mutableCloseEvent

    /**
     * Tracks the screen shown analytics event. Must be called from the fragment's
     * `onCreateView()` (the event was tracked there before the MVVM migration as well).
     */
    fun onScreenShown() {
        trackEvent(UserAnalyticsEvent.SCREEN_SHOWN)
    }

    /**
     * Loads the photo for the document from the memory cache. Must be called from the
     * fragment's `onStart()` (the photo was loaded there before the MVVM migration as well).
     */
    fun onStart() {
        val document = imageDocument ?: return
        if (!GiniCapture.hasInstance()) {
            return
        }
        GiniCapture.getInstance().internal().photoMemoryCache
            .get(app, document, object : AsyncCallback<Photo, Exception> {
                override fun onSuccess(result: Photo) {
                    mutablePreview.value = ZoomInPreviewUiState(
                        previewBitmap = result.bitmapPreview,
                        rotationForDisplay = document.rotationForDisplay
                    )
                }

                override fun onError(exception: Exception) {
                    // Behavior kept from the fragment: no error UI is shown
                }

                override fun onCancelled() {
                    // Not used
                }
            })
    }

    fun onCloseClicked() {
        trackEvent(UserAnalyticsEvent.CLOSE_TAPPED)
        mutableCloseEvent.value = ConsumableEvent(Unit)
    }

    private fun trackEvent(event: UserAnalyticsEvent) {
        UserAnalytics.getAnalyticsEventTracker()?.trackEvent(
            event,
            setOf(UserAnalyticsEventProperty.Screen(screenName))
        )
    }

    class Factory(
        private val application: Application,
        private val imageDocument: ImageDocument?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ZoomInPreviewViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ZoomInPreviewViewModel(application, imageDocument) as T
        }
    }
}

/**
 * What the zoom in preview screen renders.
 *
 * Internal use only.
 */
internal data class ZoomInPreviewUiState(
    val previewBitmap: Bitmap?,
    val rotationForDisplay: Int,
)

package net.gini.android.bank.sdk.componentapiexample

import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import org.slf4j.LoggerFactory

object GiniCaptureEventTracker : EventTracker {
    private val LOG = LoggerFactory.getLogger(GiniCaptureEventTracker::class.java)

    override fun onOnboardingScreenEvent(event: Event<OnboardingScreenEvent>) {
        when (event.type) {
            OnboardingScreenEvent.START -> LOG.info("Onboarding started")
            OnboardingScreenEvent.FINISH -> LOG.info("Onboarding finished")
        }
    }

    override fun onCameraScreenEvent(event: Event<CameraScreenEvent>) {
        when (event.type) {
            CameraScreenEvent.TAKE_PICTURE -> LOG.info("Take picture")
            CameraScreenEvent.HELP -> LOG.info("Show help")
            CameraScreenEvent.EXIT -> LOG.info("Exit")
        }
    }

    override fun onReviewScreenEvent(event: Event<ReviewScreenEvent>) {
        when (event.type) {
            ReviewScreenEvent.NEXT -> LOG.info("Go next to analyse")
            ReviewScreenEvent.BACK -> LOG.info("Go back to the camera")
            ReviewScreenEvent.UPLOAD_ERROR -> {
                val error = event.details[ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
                LOG.info(
                    "Upload failed:\nmessage: {}\nerror:",
                    event.details[ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.MESSAGE],
                    error
                )
            }
        }
    }

    override fun onAnalysisScreenEvent(event: Event<AnalysisScreenEvent>) {
        when (event.type) {
            AnalysisScreenEvent.ERROR -> {
                val error = event.details[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
                LOG.info(
                    "Analysis failed:\nmessage: {}\nerror:",
                    event.details[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.MESSAGE],
                    error
                )
            }
            AnalysisScreenEvent.RETRY -> LOG.info("Retry analysis")
            AnalysisScreenEvent.CANCEL -> LOG.info("Analysis cancelled")
        }
    }
}
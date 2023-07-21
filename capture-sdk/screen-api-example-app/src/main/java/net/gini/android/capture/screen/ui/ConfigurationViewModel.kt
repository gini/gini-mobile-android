package net.gini.android.capture.screen.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.review.multipage.view.DefaultReviewNavigationBarBottomAdapter
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.ui.data.Configuration
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.view.DefaultLoadingIndicatorAdapter
import org.slf4j.LoggerFactory
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor() : ViewModel() {

    private val _configurationFlow = MutableStateFlow(Configuration())
    val configurationFlow: StateFlow<Configuration> = _configurationFlow

    fun setConfiguration(configuration: Configuration) {
        _configurationFlow.value = configuration
    }


    fun configureGiniCapture(builder: GiniCapture.Builder, intent: Intent) {
        val configuration = configurationFlow.value
        // 1 open with
        builder.setFileImportEnabled(configuration.isFileImportEnabled)

        // 2 QR code scanning
        builder.setQRCodeScanningEnabled(configuration.isQrCodeEnabled)
        // 3 only QR code scanning
        builder.setOnlyQRCodeScanning(configuration.isOnlyQrCodeEnabled)
        // 4 enable multi page
        builder.setMultiPageEnabled(configuration.isMultiPageEnabled)
        // 5 enable flash toggle
        builder.setFlashButtonEnabled(configuration.isFlashToggleEnabled)
        // 6 enable flash on by default
        builder.setFlashOnByDefault(configuration.isFlashOnByDefault)
        // 7 set file import type
        builder.setDocumentImportEnabledFileTypes(configuration.documentImportEnabledFileTypes)
        // 8 enable bottom navigation bar
        builder.setBottomNavigationBarEnabled(configuration.isBottomNavigationBarEnabled)


        builder.setEventTracker(GiniCaptureEventTracker())
        builder.setCustomErrorLoggerListener(CustomErrorLoggerListener())
        builder.setReviewBottomBarNavigationAdapter(DefaultReviewNavigationBarBottomAdapter())
        builder.setLoadingIndicatorAdapter(DefaultLoadingIndicatorAdapter())
        val customHelpItems: MutableList<HelpItem.Custom> = ArrayList()
        customHelpItems.add(
            HelpItem.Custom(
                R.string.custom_help_screen_title,
                intent
            )
        )
        builder.setCustomHelpItems(customHelpItems)
        builder.setShouldShowOnboarding(configuration.isOnboardingEnabled)
        builder.setShouldShowOnboardingAtFirstRun(configuration.isOnboardingAtFirstRunEnabled)
        if (/*animatedOnboardingIllustrationsSwitch!!.isChecked*/true) {
            builder.setOnboardingAlignCornersIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    R.raw.floating_document

                )
            )
            builder.setOnboardingLightingIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    R.raw.lighting
                )
            )
            builder.setOnboardingMultiPageIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    R.raw.multipage
                )
            )
            builder.setOnboardingQRCodeIllustrationAdapter(
                CustomOnboardingIllustrationAdapter(
                    R.raw.scan_qr_code
                )
            )
        }
        if (/*customLoadingAnimationSwitch!!.isChecked*/true) {
            builder.setLoadingIndicatorAdapter(
                CustomLottiLoadingIndicatorAdapter(
                    R.raw.custom_loading
                )
            )
        }

        builder.setSupportedFormatsHelpScreenEnabled(configuration.isSupportedFormatsHelpScreenEnabled)
        builder.build()
    }

    private class GiniCaptureEventTracker : EventTracker {
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
                    val error =
                        event.details[ReviewScreenEvent.UPLOAD_ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
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
                    val error =
                        event.details[AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY.ERROR_OBJECT] as Throwable?
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

    private class CustomErrorLoggerListener : ErrorLoggerListener {
        override fun handleErrorLog(errorLog: ErrorLog) {
            LOG.error("Custom error logger: {}", errorLog.toString())
        }
    }


    companion object {
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
    }
}
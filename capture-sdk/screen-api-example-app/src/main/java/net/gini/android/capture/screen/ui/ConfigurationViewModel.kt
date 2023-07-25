package net.gini.android.capture.screen.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.screen.R
import net.gini.android.capture.screen.ui.adapters.CustomCameraNavigationBarBottomAdapter
import net.gini.android.capture.screen.ui.adapters.CustomHelpNavigationBarBottomAdapter
import net.gini.android.capture.screen.ui.adapters.CustomOnboardingNavigationBarBottomAdapter
import net.gini.android.capture.screen.ui.adapters.CustomReviewNavigationBarBottomAdapter
import net.gini.android.capture.screen.ui.data.Configuration
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import net.gini.android.capture.view.DefaultNavigationBarTopAdapter
import net.gini.android.capture.view.DefaultOnButtonLoadingIndicatorAdapter
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
        // 1 file import
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
        // 9 enable Help screens custom bottom navigation bar
        if (configuration.isHelpScreensCustomBottomNavBarEnabled)
            builder.setHelpNavigationBarBottomAdapter(CustomHelpNavigationBarBottomAdapter())
        // 10 enable camera screens custom bottom navigation bar
        if (configuration.isCameraBottomNavBarEnabled)
            builder.setCameraNavigationBarBottomAdapter(CustomCameraNavigationBarBottomAdapter())
        // 11 enable review screens custom bottom navigation bar
        if (configuration.isReviewScreenCustomBottomNavBarEnabled)
            builder.setReviewBottomBarNavigationAdapter(CustomReviewNavigationBarBottomAdapter())
        // 12 enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // 13 enable onboarding screens at first launch
        builder.setShouldShowOnboardingAtFirstRun(configuration.isOnboardingAtFirstRunEnabled)
        // 14 enable onboarding at every launch
        builder.setShouldShowOnboarding(configuration.isOnboardingAtEveryLaunchEnabled)
        // 15 enable custom onboarding pages
        if (configuration.isCustomOnboardingPagesEnabled) {
            // 16 enable align corners in custom onboarding pages
            if (configuration.isAlignCornersInCustomOnboardingEnabled) {
                builder.setOnboardingAlignCornersIllustrationAdapter(
                    CustomOnboardingIllustrationAdapter(
                        R.raw.floating_document
                    )
                )
            }
            // 17 enable lighting in custom onboarding pages
            if (configuration.isLightingInCustomOnboardingEnabled) {
                builder.setOnboardingLightingIllustrationAdapter(
                    CustomOnboardingIllustrationAdapter(
                        R.raw.lighting
                    )
                )
            }
            // 18 enable QR code in custom onboarding pages
            if (configuration.isQRCodeInCustomOnboardingEnabled) {
                builder.setOnboardingQRCodeIllustrationAdapter(
                    CustomOnboardingIllustrationAdapter(
                        R.raw.scan_qr_code
                    )
                )
            }
            // 19 enable multi page in custom onboarding pages
            if (configuration.isMultiPageInCustomOnboardingEnabled) {
                builder.setOnboardingMultiPageIllustrationAdapter(
                    CustomOnboardingIllustrationAdapter(
                        R.raw.multipage
                    )
                )
            }

        }

        // 20 enable custom navigation bar in custom onboarding pages
        if (configuration.isCustomNavigationBarInCustomOnboardingEnabled)
            builder.setOnboardingNavigationBarBottomAdapter(
                CustomOnboardingNavigationBarBottomAdapter()
            )

        //TODO: should use a custom adapter
        // 21 enable button's custom loading indicator
        if (configuration.isButtonsCustomLoadingIndicatorEnabled) {
            builder.setOnButtonLoadingIndicatorAdapter(
                DefaultOnButtonLoadingIndicatorAdapter()
            )
        }
        // 22 enable screen's custom loading indicator
        if (configuration.isScreenCustomLoadingIndicatorEnabled) {
            builder.setLoadingIndicatorAdapter(
                CustomLottiLoadingIndicatorAdapter(
                    R.raw.custom_loading
                )
            )
        }
        // 23 enable supported format help screen
        builder.setSupportedFormatsHelpScreenEnabled(configuration.isSupportedFormatsHelpScreenEnabled)

        // 24 enable custom help items
        if (configuration.isCustomHelpItemsEnabled) {
            val customHelpItems: MutableList<HelpItem.Custom> = ArrayList()
            customHelpItems.add(
                HelpItem.Custom(
                    R.string.custom_help_screen_title,
                    intent
                )
            )
            builder.setCustomHelpItems(customHelpItems)
        }

        //TODO: should be replaced with a custom adapter
        // 25 enable custom navigation bar
        if (configuration.isCustomNavBarEnabled)
            builder.setNavigationBarTopAdapter(DefaultNavigationBarTopAdapter())

        // 26 enable event tracker
        if (configuration.isEventTrackerEnabled)
            builder.setEventTracker(GiniCaptureEventTracker())
        // 27 enable Gini error logger
        builder.setGiniErrorLoggerIsOn(configuration.isGiniErrorLoggerEnabled)
        // 28 enable custom error logger
        if (configuration.isCustomErrorLoggerEnabled)
            builder.setCustomErrorLoggerListener(CustomErrorLoggerListener())
        // 29 set imported file size bytes limit
        if (configuration.importedFileSizeBytesLimit != FileImportValidator.FILE_SIZE_LIMIT && configuration.importedFileSizeBytesLimit >= 0)
            builder.importedFileSizeBytesLimit = configuration.importedFileSizeBytesLimit

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
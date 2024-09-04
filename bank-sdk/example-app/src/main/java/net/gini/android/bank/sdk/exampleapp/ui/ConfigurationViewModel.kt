package net.gini.android.bank.sdk.exampleapp.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.core.DefaultNetworkServicesProvider
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomCameraNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomDigitalInvoiceHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomDigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomDigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomLottiLoadingIndicatorAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomNavigationBarTopAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomOnButtonLoadingIndicatorAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomOnboardingIllustrationAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomReviewNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomSkontoHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.adapters.CustomSkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.ui.data.Configuration
import net.gini.android.capture.GiniCaptureDebug
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.logging.ErrorLog
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.onboarding.DefaultPages
import net.gini.android.capture.onboarding.OnboardingPage
import net.gini.android.capture.tracking.AnalysisScreenEvent
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.OnboardingScreenEvent
import net.gini.android.capture.tracking.ReviewScreenEvent
import org.slf4j.LoggerFactory
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    internal val defaultNetworkServicesProvider: DefaultNetworkServicesProvider
) : ViewModel() {

    private val _disableCameraPermissionFlow = MutableStateFlow<Boolean>(false)
    val disableCameraPermissionFlow: StateFlow<Boolean> = _disableCameraPermissionFlow

    private val _configurationFlow = MutableStateFlow(Configuration())
    val configurationFlow: StateFlow<Configuration> = _configurationFlow

    fun disableCameraPermission(cameraPermission: Boolean) {
        _disableCameraPermissionFlow.value = cameraPermission
    }

    fun setConfiguration(configuration: Configuration) {
        _configurationFlow.value = configuration
    }

    fun setupSDKWithDefaultConfigurations() {
        _configurationFlow.value = Configuration.setupSDKWithDefaultConfiguration(
            configurationFlow.value,
            CaptureConfiguration(defaultNetworkServicesProvider.defaultNetworkServiceDebugEnabled)
        )
    }

    fun configureGiniBank(context: Context) {
        val intent = Intent(context, CustomHelpActivity::class.java)

        val configuration = configurationFlow.value

        var captureConfiguration = CaptureConfiguration(
            // 37 Debug mode
            networkService = if (configuration.isDebugModeEnabled) defaultNetworkServicesProvider.defaultNetworkServiceDebugEnabled else defaultNetworkServicesProvider.defaultNetworkServiceDebugDisabled,
            // 1 file import
            fileImportEnabled = configuration.isFileImportEnabled,
            // 2 QR code scanning
            qrCodeScanningEnabled = configuration.isQrCodeEnabled,
            // 3 only QR code scanning
            onlyQRCodeScanningEnabled = configuration.isOnlyQrCodeEnabled,
            // 4 enable multi page
            multiPageEnabled = configuration.isMultiPageEnabled,
            // 5 enable flash toggle
            flashButtonEnabled = configuration.isFlashButtonDisplayed,
            // 6 enable flash on by default
            flashOnByDefault = configuration.isFlashDefaultStateEnabled,
            // 7 set file import type
            documentImportEnabledFileTypes = configuration.documentImportEnabledFileTypes,
            // 8 enable bottom navigation bar
            bottomNavigationBarEnabled = configuration.isBottomNavigationBarEnabled,

            // 9-12 are implemented after captureConfiguration initialisation

            // 13 enable onboarding screens at first launch
            showOnboardingAtFirstRun = configuration.isOnboardingAtFirstRunEnabled,
            // 14 enable onboarding at every launch
            showOnboarding = configuration.isOnboardingAtEveryLaunchEnabled,

            // 15-22 are implemented after captureConfiguration initialisation

            // 23 enable supported format help screen
            supportedFormatsHelpScreenEnabled = configuration.isSupportedFormatsHelpScreenEnabled,

            // 24-26 are implemented after captureConfiguration initialisation

            // 27 enable Gini error logger
            giniErrorLoggerIsOn = configuration.isGiniErrorLoggerEnabled,

            // 28-29 are implemented after captureConfiguration initialisation

            // 30 entry point
            entryPoint = configuration.entryPoint,

            // 31 enable return assistant
            returnAssistantEnabled = configuration.isReturnAssistantEnabled,

            // allow screenshots
            allowScreenshots = configuration.isAllowScreenshotsEnabled,

            // 40 enable skonto
            skontoEnabled = configuration.isSkontoEnabled,
        )

        // 9 enable Help screens custom bottom navigation bar
        if (configuration.isHelpScreensCustomBottomNavBarEnabled)
            captureConfiguration =
                captureConfiguration.copy(helpNavigationBarBottomAdapter = CustomHelpNavigationBarBottomAdapter())

        // 10 enable camera screens custom bottom navigation bar
        if (configuration.isCameraBottomNavBarEnabled)
            captureConfiguration =
                captureConfiguration.copy(cameraNavigationBarBottomAdapter = CustomCameraNavigationBarBottomAdapter())

        // 11 enable review screens custom bottom navigation bar
        if (configuration.isReviewScreenCustomBottomNavBarEnabled)
            captureConfiguration =
                captureConfiguration.copy(reviewNavigationBarBottomAdapter = CustomReviewNavigationBarBottomAdapter())

        // 12 enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

        // 15 enable custom onboarding pages
        if (configuration.isCustomOnboardingPagesEnabled) {
            val pages = DefaultPages.asArrayList(
                configuration.isMultiPageEnabled,
                configuration.isQrCodeEnabled
            )
            pages.add(
                OnboardingPage(
                    R.string.additional_onboarding_page_title,
                    R.string.additional_onboarding_page_message,
                    null
                )
            )

            captureConfiguration =
                captureConfiguration.copy(onboardingPages = pages)
        }

        // 16 enable align corners in custom onboarding pages
        if (configuration.isAlignCornersInCustomOnboardingEnabled) {
            captureConfiguration = captureConfiguration.copy(
                onboardingAlignCornersIllustrationAdapter = CustomOnboardingIllustrationAdapter(
                    R.raw.floating_document
                )
            )
        }

        // 17 enable lighting in custom onboarding pages
        if (configuration.isLightingInCustomOnboardingEnabled) {
            captureConfiguration = captureConfiguration.copy(
                onboardingLightingIllustrationAdapter = CustomOnboardingIllustrationAdapter(
                    R.raw.lighting
                )
            )
        }

        // 18 enable QR code in custom onboarding pages
        if (configuration.isQRCodeInCustomOnboardingEnabled) {
            captureConfiguration = captureConfiguration.copy(
                onboardingQRCodeIllustrationAdapter = CustomOnboardingIllustrationAdapter(
                    R.raw.scan_qr_code
                )
            )
        }

        // 19 enable multi page in custom onboarding pages
        if (configuration.isMultiPageInCustomOnboardingEnabled) {
            captureConfiguration = captureConfiguration.copy(
                onboardingMultiPageIllustrationAdapter = CustomOnboardingIllustrationAdapter(
                    R.raw.multipage
                )
            )
        }

        // 20 enable custom navigation bar in custom onboarding pages
        if (configuration.isCustomNavigationBarInCustomOnboardingEnabled)
            captureConfiguration = captureConfiguration.copy(
                onboardingNavigationBarBottomAdapter = CustomOnboardingNavigationBarBottomAdapter()
            )

        // 21 enable button's custom loading indicator
        if (configuration.isButtonsCustomLoadingIndicatorEnabled) {
            captureConfiguration = captureConfiguration.copy(
                onButtonLoadingIndicatorAdapter = CustomOnButtonLoadingIndicatorAdapter()
            )
        }

        // 22 enable screen's custom loading indicator
        if (configuration.isScreenCustomLoadingIndicatorEnabled) {
            captureConfiguration = captureConfiguration.copy(
                customLoadingIndicatorAdapter = CustomLottiLoadingIndicatorAdapter(
                    R.raw.custom_loading
                )
            )
        }

        // 24 enable custom help items
        if (configuration.isCustomHelpItemsEnabled) {
            val customHelpItems: MutableList<HelpItem.Custom> = ArrayList()
            customHelpItems.add(
                HelpItem.Custom(
                    R.string.custom_help_screen_title,
                    intent
                )
            )
            captureConfiguration = captureConfiguration.copy(
                customHelpItems = customHelpItems
            )
        }

        // 25 enable custom navigation bar
        if (configuration.isCustomNavBarEnabled)
            captureConfiguration = captureConfiguration.copy(
                navigationBarTopAdapter = CustomNavigationBarTopAdapter()
            )

        // 26 enable event tracker
        if (configuration.isEventTrackerEnabled)
            captureConfiguration = captureConfiguration.copy(
                eventTracker = GiniCaptureEventTracker()
            )

        // 28 enable custom error logger
        if (configuration.isCustomErrorLoggerEnabled)
            captureConfiguration = captureConfiguration.copy(
                errorLoggerListener = CustomErrorLoggerListener()
            )

        // 29 set imported file size bytes limit
        if (configuration.importedFileSizeBytesLimit != FileImportValidator.FILE_SIZE_LIMIT && configuration.importedFileSizeBytesLimit >= 0)
            captureConfiguration = captureConfiguration.copy(
                importedFileSizeBytesLimit = configuration.importedFileSizeBytesLimit
            )

        GiniBank.setCaptureConfiguration(context, captureConfiguration)

        // 32 enable return reasons dialog
        GiniBank.enableReturnReasons = configuration.isReturnReasonsEnabled

        // 33 Digital invoice onboarding custom illustration
        if (configuration.isDigitalInvoiceOnboardingCustomIllustrationEnabled) {
            GiniBank.digitalInvoiceOnboardingIllustrationAdapter =
                CustomOnboardingIllustrationAdapter(
                    R.raw.ai_animation
                )
        }

        // 34 Digital invoice help bottom navigation bar
        if (configuration.isDigitalInvoiceHelpBottomNavigationBarEnabled) {
            GiniBank.digitalInvoiceHelpNavigationBarBottomAdapter =
                CustomDigitalInvoiceHelpNavigationBarBottomAdapter()
        }

        if (configuration.isSkontoCustomNavBarEnabled) {
            GiniBank.skontoNavigationBarBottomAdapter = CustomSkontoNavigationBarBottomAdapter()
        } else {
            GiniBank.skontoNavigationBarBottomAdapter = null
        }

        if (configuration.isSkontoHelpCustomNavBarEnabled) {
            GiniBank.skontoHelpNavigationBarBottomAdapter = CustomSkontoHelpNavigationBarBottomAdapter()
        } else {
            GiniBank.skontoHelpNavigationBarBottomAdapter = null
        }

        // 35 Digital invoice onboarding bottom navigation bar
        if (configuration.isDigitalInvoiceOnboardingBottomNavigationBarEnabled) {
            GiniBank.digitalInvoiceOnboardingNavigationBarBottomAdapter =
                CustomDigitalInvoiceOnboardingNavigationBarBottomAdapter()
        }

        // 36 Digital invoice bottom navigation bar
        if (configuration.isDigitalInvoiceBottomNavigationBarEnabled) {
            GiniBank.digitalInvoiceNavigationBarBottomAdapter =
                CustomDigitalInvoiceNavigationBarBottomAdapter()
        }

        // 37 Debug mode
        GiniCaptureDebug.enable()
        configureLogging()
        if (configuration.isDebugModeEnabled) {
            GiniCaptureDebug.enable()
            configureLogging()
        }

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
                AnalysisScreenEvent.NO_RESULTS -> LOG.info("No results analysis")
            }
        }
    }

    private class CustomErrorLoggerListener : ErrorLoggerListener {
        override fun handleErrorLog(errorLog: ErrorLog) {
            LOG.error("Custom error logger: {}", errorLog.toString())
        }
    }

    private fun configureLogging() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.reset()
        val layoutEncoder = PatternLayoutEncoder()
        layoutEncoder.context = lc
        layoutEncoder.pattern = "%-5level %file:%line [%thread] - %msg%n"
        layoutEncoder.start()
        val logcatAppender = LogcatAppender()
        logcatAppender.context = lc
        logcatAppender.encoder = layoutEncoder
        logcatAppender.start()
        val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(logcatAppender)
    }


    fun clearGiniCaptureNetworkInstances() {
        defaultNetworkServicesProvider.defaultNetworkServiceDebugDisabled.cleanup()
        defaultNetworkServicesProvider.defaultNetworkServiceDebugEnabled.cleanup()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainActivity::class.java)
    }
}
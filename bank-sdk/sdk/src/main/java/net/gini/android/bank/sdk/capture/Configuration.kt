package net.gini.android.bank.sdk.capture

import net.gini.android.bank.sdk.BuildConfig
import net.gini.android.bank.sdk.capture.skonto.SkontoNavigationBarBottomAdapter
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.camera.view.CameraNavigationBarBottomAdapter
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter
import net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.onboarding.OnboardingPage
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.review.multipage.view.ReviewNavigationBarBottomAdapter
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.NavigationBarTopAdapter
import net.gini.android.capture.view.OnButtonLoadingIndicatorAdapter

/**
 * Configuration class for Capture feature.
 */
data class CaptureConfiguration(
    val USER_COMMENT_GINI_BANK_VERSION: String = "GiniBankVer",

    /**
     * Set the [GiniCaptureNetworkService] instance which will be used by the library to
     * request document related network calls (e.g. upload, analysis or deletion).
     */
    val networkService: GiniCaptureNetworkService,

    /**
     * Screen API only
     *
     * When enabled shows the OnboardingActivity the first time the CameraActivity is launched.
     * We highly recommend having it enabled.
     */
    val showOnboardingAtFirstRun: Boolean = true,

    /**
     * Set custom pages to be shown in the Onboarding Screen.
     */
    val onboardingPages: List<OnboardingPage> = emptyList(),

    /**
     * Screen API only
     *
     * When enabled shows the Onboarding Screen every time the [CameraActivity]
     * starts.
     */
    val showOnboarding: Boolean = false,

    /**
     * Enable/disable the multi-page feature.
     */
    val multiPageEnabled: Boolean = false,

    /**
     * Enable and configure the document import feature or disable it by passing in [DocumentImportEnabledFileTypes.NONE].
     */
    val documentImportEnabledFileTypes: DocumentImportEnabledFileTypes = DocumentImportEnabledFileTypes.NONE,

    /**
     * Enable/disable the file import feature.
     */
    val fileImportEnabled: Boolean = false,

    /**
     * Enable/disable the QRCode scanning feature.
     */
    val qrCodeScanningEnabled: Boolean = false,

    /**
     * Enable/disable only the QRCode scanning feature.
     */
    val onlyQRCodeScanningEnabled: Boolean = false,

    /**
     * Enable/disable the Supported Formats help screen.
     */
    val supportedFormatsHelpScreenEnabled: Boolean = true,

    /**
     * Enable/disable the flash button in the Camera Screen.
     */
    val flashButtonEnabled: Boolean = false,

    /**
     * Set whether the camera flash should be on or off when the SDK starts. The flash is off by default.
     */
    val flashOnByDefault: Boolean = false,

    /**
     * Enable/disable the return assistant feature.
     */
    val returnAssistantEnabled: Boolean = true,

    /**
     * [EventTracker] instance which will be called from the different screens to inform you about the various events
     * which can occur during the usage of the Capture feature.
     */
    val eventTracker: EventTracker? = null,

    /**
     * A list of [HelpItem.Custom] defining the custom help items to be shown in the Help Screen.
     */
    val customHelpItems: List<HelpItem.Custom> = emptyList(),

    /**
     * Set whether the default Gini error logging implementation is on or not.
     *
     * On by default.
     */
    val giniErrorLoggerIsOn: Boolean = true,

    /**
     * Set an [ErrorLoggerListener] to be notified of errors.
     */
    val errorLoggerListener: ErrorLoggerListener? = null,

    /**
     * Set a custom imported file size limit in bytes.
     */
    val importedFileSizeBytesLimit: Int = FILE_SIZE_LIMIT,

    /**
     * Set an adapter implementation to show a custom top navigation bar.
     */
    val navigationBarTopAdapter: NavigationBarTopAdapter? = null,

    /**
     * Enable/disable the bottom navigation bar.
     *
     * Disabled by default.
     */
    val bottomNavigationBarEnabled: Boolean = false,

    /**
     * Set an adapter implementation to show a custom bottom navigation bar on the onboarding screen.
     */
    val onboardingNavigationBarBottomAdapter: OnboardingNavigationBarBottomAdapter? = null,

    /**
     * Set an adapter implementation to show a custom illustration on the "align corners" onboarding page.
     */
    val onboardingAlignCornersIllustrationAdapter: OnboardingIllustrationAdapter? = null,

    /**
     * Set an adapter implementation to show a custom illustration on the "lighting" onboarding page.
     */
    val onboardingLightingIllustrationAdapter: OnboardingIllustrationAdapter? = null,

    /**
     * Set an adapter implementation to show a custom illustration on the "multi-page" onboarding page.
     */
    val onboardingMultiPageIllustrationAdapter: OnboardingIllustrationAdapter? = null,

    /**
     * Set an adapter implementation to show a custom illustration on the "QR code" onboarding page.
     */
    val onboardingQRCodeIllustrationAdapter: OnboardingIllustrationAdapter? = null,

    /**
     * Set an adapter implementation to show a custom loading animation during analyse and scan.
     */
    val customLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter? = null,

    /**
     * Set an adapter implementation to show a custom loading animation during analyse and scan.
     */
    val onButtonLoadingIndicatorAdapter: OnButtonLoadingIndicatorAdapter? = null,

    /**
     * Set an adapter implementation to show a custom bottom navigation bar on the camera screen.
     */
    val cameraNavigationBarBottomAdapter: CameraNavigationBarBottomAdapter? = null,

    /**
     * Set an adapter implementation to show a custom bottom navigation bar on the review screen.
     */
    val reviewNavigationBarBottomAdapter: ReviewNavigationBarBottomAdapter? = null,

    /**
     * Set an adapter implementation to show a custom bottom navigation bar on the Skonto screen.
     */
    val skontoNavigationBarBottomAdapter: SkontoNavigationBarBottomAdapter? = null,

    /**
     * Set an adapter implementation to show a custom bottom navigation bar on the help screen.
     */
    val helpNavigationBarBottomAdapter: HelpNavigationBarBottomAdapter? = null,

    /**
     * Set the entry point used for launching the SDK. See [EntryPoint] for possible values.
     *
     * Default value is [EntryPoint.BUTTON].
     */
    val entryPoint: EntryPoint = GiniCapture.Internal.DEFAULT_ENTRY_POINT,

    /**
     * Set whether screenshots should be allowed or not.
     *
     * Screenshots are allowed by default.
     *
     * IMPORTANT: If you disallow screenshots and use the [CaptureFlowFragment] for launching the SDK in your activity, please clear the [android.view.WindowManager.LayoutParams.FLAG_SECURE]
     * on your activity's window after the SDK has finished to allow users to take screenshots of your app again.
     */
    val allowScreenshots: Boolean = true,

    /**
     * Enable/disable the skonto feature.
     */
    val skontoEnabled: Boolean = true,
)

internal fun GiniCapture.Builder.applyConfiguration(configuration: CaptureConfiguration): GiniCapture.Builder {
    return this.setGiniCaptureNetworkService(configuration.networkService)
        .setShouldShowOnboardingAtFirstRun(configuration.showOnboardingAtFirstRun)
        .setShouldShowOnboarding(configuration.showOnboarding)
        .setMultiPageEnabled(configuration.multiPageEnabled)
        .setDocumentImportEnabledFileTypes(configuration.documentImportEnabledFileTypes)
        .setFileImportEnabled(configuration.fileImportEnabled)
        .setQRCodeScanningEnabled(configuration.qrCodeScanningEnabled)
        .setOnlyQRCodeScanning(configuration.onlyQRCodeScanningEnabled)
        .setSupportedFormatsHelpScreenEnabled(configuration.supportedFormatsHelpScreenEnabled)
        .setFlashButtonEnabled(configuration.flashButtonEnabled)
        .setFlashOnByDefault(configuration.flashOnByDefault)
        .setCustomHelpItems(configuration.customHelpItems)
        .setGiniErrorLoggerIsOn(configuration.giniErrorLoggerIsOn)
        .setImportedFileSizeBytesLimit(configuration.importedFileSizeBytesLimit)
        .setBottomNavigationBarEnabled(configuration.bottomNavigationBarEnabled)
        .setEntryPoint(configuration.entryPoint)
        .setAllowScreenshots(configuration.allowScreenshots)
        .addCustomUploadMetadata(configuration.USER_COMMENT_GINI_BANK_VERSION, BuildConfig.VERSION_NAME)
        .apply {
            configuration.eventTracker?.let { setEventTracker(it) }
            configuration.errorLoggerListener?.let { setCustomErrorLoggerListener(it) }
            if (configuration.onboardingPages.isNotEmpty()) {
                setCustomOnboardingPages(arrayListOf<OnboardingPage>().apply {
                    configuration.onboardingPages.forEach { add(it) }
                })
            }
            configuration.navigationBarTopAdapter?.let { setNavigationBarTopAdapter(it) }
            configuration.onboardingAlignCornersIllustrationAdapter?.let {
                setOnboardingAlignCornersIllustrationAdapter(
                    it
                )
            }
            configuration.onboardingLightingIllustrationAdapter?.let {
                setOnboardingLightingIllustrationAdapter(
                    it
                )
            }
            configuration.onboardingMultiPageIllustrationAdapter?.let {
                setOnboardingMultiPageIllustrationAdapter(
                    it
                )
            }
            configuration.onboardingQRCodeIllustrationAdapter?.let {
                setOnboardingQRCodeIllustrationAdapter(
                    it
                )
            }
            configuration.customLoadingIndicatorAdapter?.let { setLoadingIndicatorAdapter(it) }
            configuration.onButtonLoadingIndicatorAdapter?.let {
                setOnButtonLoadingIndicatorAdapter(
                    it
                )
            }
            configuration.onboardingNavigationBarBottomAdapter?.let {
                setOnboardingNavigationBarBottomAdapter(
                    it
                )
            }
            configuration.cameraNavigationBarBottomAdapter?.let {
                setCameraNavigationBarBottomAdapter(
                    it
                )
            }
            configuration.reviewNavigationBarBottomAdapter?.let {
                setReviewBottomBarNavigationAdapter(
                    it
                )
            }
            configuration.helpNavigationBarBottomAdapter?.let { setHelpNavigationBarBottomAdapter(it) }
        }
}

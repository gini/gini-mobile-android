package net.gini.android.bank.sdk.capture

import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.analysis.AnalysisActivity
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.help.HelpItem
import net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT
import net.gini.android.capture.logging.ErrorLoggerListener
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.onboarding.OnboardingPage
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import net.gini.android.capture.onboarding.view.OnboardingNavigationBarBottomAdapter
import net.gini.android.capture.review.ReviewActivity
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import net.gini.android.capture.view.NavigationBarTopAdapter

/**
 * Configuration class for Capture feature.
 */
data class CaptureConfiguration(

    /**
     * Set the [GiniCaptureNetworkService] instance which will be used by the library to
     * request document related network calls (e.g. upload, analysis or deletion).
     */
    val networkService: GiniCaptureNetworkService,

    /**
     * Set the [GiniCaptureNetworkApi] instance which clients can use to request network
     * calls (e.g. for sending feedback).
     */
    val networkApi: GiniCaptureNetworkApi,

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
     * Enable/disable the Supported Formats help screen.
     */
    val supportedFormatsHelpScreenEnabled: Boolean = true,

    /**
     * Enable/disable the flash button in the Camera Screen.
     */
    val flashButtonEnabled: Boolean = false,

    /**
     * Set whether the camera flash is on or off by default.
     */
    val flashOnByDefault: Boolean = true,

    /**
     * Screen API only
     *
     * Enable/disable back buttons in all Activities except [ReviewActivity] and
     * [AnalysisActivity], which always show back buttons.
     */
    val backButtonsEnabled: Boolean = true,

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

    val customLoadingIndicatorAdapter: CustomLoadingIndicatorAdapter? = null
)

internal fun GiniCapture.Builder.applyConfiguration(configuration: CaptureConfiguration): GiniCapture.Builder {
    return this.setGiniCaptureNetworkService(configuration.networkService)
        .setGiniCaptureNetworkApi(configuration.networkApi)
        .setShouldShowOnboardingAtFirstRun(configuration.showOnboardingAtFirstRun)
        .setShouldShowOnboarding(configuration.showOnboarding)
        .setMultiPageEnabled(configuration.multiPageEnabled)
        .setDocumentImportEnabledFileTypes(configuration.documentImportEnabledFileTypes)
        .setFileImportEnabled(configuration.fileImportEnabled)
        .setQRCodeScanningEnabled(configuration.qrCodeScanningEnabled)
        .setSupportedFormatsHelpScreenEnabled(configuration.supportedFormatsHelpScreenEnabled)
        .setFlashButtonEnabled(configuration.flashButtonEnabled)
        .setBackButtonsEnabled(configuration.backButtonsEnabled)
        .setFlashOnByDefault(configuration.flashOnByDefault)
        .setCustomHelpItems(configuration.customHelpItems)
        .setGiniErrorLoggerIsOn(configuration.giniErrorLoggerIsOn)
        .setImportedFileSizeBytesLimit(configuration.importedFileSizeBytesLimit)
        .setBottomNavigationBarEnabled(configuration.bottomNavigationBarEnabled)
        .apply {
            configuration.eventTracker?.let { setEventTracker(it) }
            configuration.errorLoggerListener?.let { setCustomErrorLoggerListener(it) }
            if (configuration.onboardingPages.isNotEmpty()) {
                setCustomOnboardingPages(arrayListOf<OnboardingPage>().apply {
                    configuration.onboardingPages.forEach { add(it) }
                })
            }
            configuration.navigationBarTopAdapter?.let { setNavigationBarTopAdapter(it) }
            configuration.onboardingNavigationBarBottomAdapter?.let { setOnboardingNavigationBarBottomAdapter(it) }
            configuration.onboardingAlignCornersIllustrationAdapter?.let { setOnboardingAlignCornersIllustrationAdapter(it) }
            configuration.onboardingLightingIllustrationAdapter?.let { setOnboardingLightingIllustrationAdapter(it) }
            configuration.onboardingMultiPageIllustrationAdapter?.let { setOnboardingMultiPageIllustrationAdapter(it) }
            configuration.onboardingQRCodeIllustrationAdapter?.let { setOnboardingQRCodeIllustrationAdapter(it) }
            configuration.customLoadingIndicatorAdapter?.let { setLoadingIndicatorAdapter(it) }
        }
}

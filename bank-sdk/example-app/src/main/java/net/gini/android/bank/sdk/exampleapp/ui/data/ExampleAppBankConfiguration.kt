package net.gini.android.bank.sdk.exampleapp.ui.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.DocumentImportEnabledFileTypes
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.FileImportValidator


@Parcelize
data class ExampleAppBankConfiguration(
    // setup sdk with default configurations
    val isDefaultSDKConfigurationsEnabled: Boolean = false,

    // file import
    // net.gini.android.capture.GiniCapture.Builder#setFileImportEnabled → on/off switch
    val isFileImportEnabled: Boolean = true,

    // QR code scanning
    // net.gini.android.capture.GiniCapture.Builder#setOnlyQRCodeScanning → on/off switch
    val isQrCodeEnabled: Boolean = true,

    // only QR code scanning
    // net.gini.android.capture.GiniCapture.Builder#setQRCodeScanningEnabled → on/off switch
    val isOnlyQrCodeEnabled: Boolean = false,

    // enable multi page
    // net.gini.android.capture.GiniCapture.Builder#setMultiPageEnabled →  on/off switch
    val isMultiPageEnabled: Boolean = true,

    // enable flash toggle
    // net.gini.android.capture.GiniCapture.Builder#setFlashButtonEnabled → on/off switch
    val isFlashButtonDisplayed: Boolean = true,

    // enable flash on by default
    // net.gini.android.capture.GiniCapture.Builder#setFlashOnByDefault → on/off switch
    val isFlashDefaultStateEnabled: Boolean = false,

    // set import document type support
    // net.gini.android.capture.GiniCapture.Builder#setDocumentImportEnabledFileTypes →
    //      radio buttons to select an available enum value
    val documentImportEnabledFileTypes: DocumentImportEnabledFileTypes =
        DocumentImportEnabledFileTypes.PDF_AND_IMAGES,

    // enable bottom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setBottomNavigationBarEnabled → on/off switch
    val isBottomNavigationBarEnabled: Boolean = false,

    // enable Help screens custom bottom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setHelpNavigationBarBottomAdapter →
    //      on/off switch to show a custom adapter implementation
    val isHelpScreensCustomBottomNavBarEnabled: Boolean = false,

    // enable Error screens custom bottom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setErrorNavigationBarBottomAdapter →
    // on/off switch to show a custom adapter implementation
    val isErrorScreensCustomBottomNavBarEnabled: Boolean = false,

    // enable camera screens custom bottom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setCameraNavigationBarBottomAdapter →
    //      on/off switch to show a custom adapter implementation
    val isCameraBottomNavBarEnabled: Boolean = false,

    // enable review screens custom bottom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setReviewBottomBarNavigationAdapter →
    //      on/off switch to show a custom adapter implementation
    val isReviewScreenCustomBottomNavBarEnabled: Boolean = false,

    // enable image picker screens custom bottom navigation bar -> was implemented on iOS, not needed for Android

    // enable onboarding screens at first launch
    // net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboardingAtFirstRun → on/off switch
    val isOnboardingAtFirstRunEnabled: Boolean = true,

    // enable onboarding at every launch
    // net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboarding → on/off switch
    val isOnboardingAtEveryLaunchEnabled: Boolean = false,

    // enable custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setCustomOnboardingPages →
    //      on/off switch to show custom onboarding pages
    val isCustomOnboardingPagesEnabled: Boolean = false,

    // enable align corners in custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setOnboardingAlignCornersIllustrationAdapter
    val isAlignCornersInCustomOnboardingEnabled: Boolean = false,

    // enable lighting in custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setOnboardingLightingIllustrationAdapter
    val isLightingInCustomOnboardingEnabled: Boolean = false,

    // enable QR code in custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setOnboardingQRCodeIllustrationAdapter->
    //      on/off switch to show custom adapter with animated illustrations
    val isQRCodeInCustomOnboardingEnabled: Boolean = false,

    // enable multi page in custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setOnboardingMultiPageIllustrationAdapter
    val isMultiPageInCustomOnboardingEnabled: Boolean = false,

    //  enable custom navigation bar in custom onboarding pages
    // net.gini.android.capture.GiniCapture.Builder#setOnboardingNavigationBarBottomAdapter
    val isCustomNavigationBarInCustomOnboardingEnabled: Boolean = false,


    // enable button's custom loading indicator
    // net.gini.android.capture.GiniCapture.Builder#setOnButtonLoadingIndicatorAdapter →
    //      on/off switch to show a custom adapter implementation
    val isButtonsCustomLoadingIndicatorEnabled: Boolean = false,

    // enable screen's custom loading indicator
    // net.gini.android.capture.GiniCapture.Builder#setLoadingIndicatorAdapter →
    //      on/off switch to show a custom adapter implementation

    val isScreenCustomLoadingIndicatorEnabled: Boolean = false,

    // enable supported format help screen
    // net.gini.android.capture.GiniCapture.Builder#setSupportedFormatsHelpScreenEnabled → on/off switch
    val isSupportedFormatsHelpScreenEnabled: Boolean = true,

    // enable custom help items
    // net.gini.android.capture.GiniCapture.Builder#setCustomHelpItems → on/off switch to show a custom help item
    val isCustomHelpItemsEnabled: Boolean = false,

    // enable custom navigation bar
    // net.gini.android.capture.GiniCapture.Builder#setNavigationBarTopAdapter →
    //    on/off switch to show a custom adapter implementation
    val isCustomNavBarEnabled: Boolean = false,

    // enable custom primary button in compose
    val isCustomPrimaryComposeButtonEnabled: Boolean = false,

    // enable event tracker
    // net.gini.android.capture.GiniCapture.Builder#setEventTracker → ignore
    val isEventTrackerEnabled: Boolean = true,

    // enable Gini error logger
    // net.gini.android.capture.GiniCapture.Builder#setGiniErrorLoggerIsOn → ignore
    val isGiniErrorLoggerEnabled: Boolean = true,

    // enable custom error logger
    // net.gini.android.capture.GiniCapture.Builder#setCustomErrorLoggerListener → ignore
    //val setCustomErrorLoggerListener: ErrorLoggerListener? = null,
    val isCustomErrorLoggerEnabled: Boolean = false,

    // set imported file size bytes limit
    // net.gini.android.capture.GiniCapture.Builder#setImportedFileSizeBytesLimit → numeric text field
    val importedFileSizeBytesLimit: Int = FileImportValidator.FILE_SIZE_LIMIT,

    val clientId: String = "",

    val clientSecret: String = "",

    // entry point
    val entryPoint: EntryPoint = EntryPoint.BUTTON,

    // enable return assistant
    val isReturnAssistantEnabled: Boolean = true,

    // enable return reasons dialog
    val isReturnReasonsEnabled: Boolean = false,

    // enable show warning for paid invoices
    val isAlreadyPaidHintEnabled: Boolean = true,

    // enable payment due hint
    val isPaymentDueHintEnabled: Boolean = true,

    //  payment due hint threshold days
    val paymentDueHintThresholdDays: Int = GiniCapture.PAYMENT_DUE_HINT_THRESHOLD_DAYS,

    // enable credit note hint
    val isCreditNoteHintEnabled: Boolean = true,

    // Digital invoice onboarding custom illustration
    val isDigitalInvoiceOnboardingCustomIllustrationEnabled: Boolean = false,

    // Digital invoice help bottom navigation bar
    val isDigitalInvoiceHelpBottomNavigationBarEnabled: Boolean = false,

    // Digital invoice onboarding bottom navigation bar
    val isDigitalInvoiceOnboardingBottomNavigationBarEnabled: Boolean = false,

    // Digital invoice bottom navigation bar
    val isDigitalInvoiceBottomNavigationBarEnabled: Boolean = false,

    // Debug mode
    val isDebugModeEnabled: Boolean = true,

    // Is Allow Screenshots
    val isAllowScreenshotsEnabled: Boolean = true,

    // Skonto Custom bottom navigation
    val isSkontoCustomNavBarEnabled: Boolean = false,

    // enable Skonto
    val isSkontoEnabled: Boolean = true,

    // Skonto help Custom bottom navigation
    val isSkontoHelpCustomNavBarEnabled: Boolean = false,

    // Digital Invoice Skonto Custom bottom navigation
    val isDigitalInvoiceSkontoCustomNavBarEnabled: Boolean = false,

    // enable transaction docs
    val isTransactionDocsEnabled: Boolean = true,

    // enable Capture Sdk
    val isCaptureSDK: Boolean = false,

    // enable/disable save invoices locally feature
    val saveInvoicesLocallyEnabled: Boolean = true,

) : Parcelable {

    companion object Companion {
        fun setupSDKWithDefaultConfiguration(
            currentConfiguration: ExampleAppBankConfiguration,
            defaultCaptureConfiguration: CaptureConfiguration,
        ): ExampleAppBankConfiguration {
            return currentConfiguration.copy(
                isFileImportEnabled = defaultCaptureConfiguration.fileImportEnabled,
                isQrCodeEnabled = defaultCaptureConfiguration.qrCodeScanningEnabled,
                isOnlyQrCodeEnabled = defaultCaptureConfiguration.onlyQRCodeScanningEnabled,
                isMultiPageEnabled = defaultCaptureConfiguration.multiPageEnabled,
                isFlashButtonDisplayed = defaultCaptureConfiguration.flashButtonEnabled,
                isFlashDefaultStateEnabled = defaultCaptureConfiguration.flashOnByDefault,
                documentImportEnabledFileTypes = defaultCaptureConfiguration.documentImportEnabledFileTypes,
                isBottomNavigationBarEnabled = defaultCaptureConfiguration.bottomNavigationBarEnabled,
                isAlreadyPaidHintEnabled = defaultCaptureConfiguration.alreadyPaidHintEnabled,
                isPaymentDueHintEnabled = defaultCaptureConfiguration.paymentDueHintEnabled,
                paymentDueHintThresholdDays = defaultCaptureConfiguration.paymentDueHintThresholdDays,
                isCreditNoteHintEnabled = defaultCaptureConfiguration.creditNoteHintEnabled,
                isOnboardingAtFirstRunEnabled = defaultCaptureConfiguration.showOnboardingAtFirstRun,
                isOnboardingAtEveryLaunchEnabled = defaultCaptureConfiguration.showOnboarding,
                isSupportedFormatsHelpScreenEnabled = defaultCaptureConfiguration.supportedFormatsHelpScreenEnabled,
                isGiniErrorLoggerEnabled = defaultCaptureConfiguration.giniErrorLoggerIsOn,
                isReturnAssistantEnabled = defaultCaptureConfiguration.returnAssistantEnabled,
                isAllowScreenshotsEnabled = defaultCaptureConfiguration.allowScreenshots,
                isSkontoEnabled = defaultCaptureConfiguration.skontoEnabled,
                isTransactionDocsEnabled = defaultCaptureConfiguration.transactionDocsEnabled,
                isCaptureSDK = currentConfiguration.isCaptureSDK,
                saveInvoicesLocallyEnabled = currentConfiguration.saveInvoicesLocallyEnabled
            )
        }
    }
}

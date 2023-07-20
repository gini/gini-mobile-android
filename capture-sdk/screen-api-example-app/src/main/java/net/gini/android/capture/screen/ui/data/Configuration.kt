package net.gini.android.capture.screen.ui.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Configuration(
    // net.gini.android.capture.GiniCapture.Builder#setBackButtonsEnabled → ignore (will be removed)

//    net.gini.android.capture.GiniCapture.Builder#setOnlyQRCodeScanning → on/off switch
    val isQrCodeEnabled: Boolean = true,
//    net.gini.android.capture.GiniCapture.Builder#setQRCodeScanningEnabled → on/off switch
    val isOnlyQrCodeEnabled: Boolean = false,
//    net.gini.android.capture.GiniCapture.Builder#setMultiPageEnabled →  on/off switch
    val isMultiPageEnabled: Boolean = true,
//    net.gini.android.capture.GiniCapture.Builder#setFlashButtonEnabled → on/off switch
    val isFlashToggleEnabled: Boolean = true,
//    net.gini.android.capture.GiniCapture.Builder#setFlashOnByDefault → on/off switch
    val isFlashOnByDefault: Boolean = true,
// net.gini.android.capture.GiniCapture.Builder#setBottomNavigationBarEnabled → on/off switch
    val isBottomNavigationBarEnabled: Boolean = true,
// net.gini.android.capture.GiniCapture.Builder#setCustomHelpItems → on/off switch to show a custom help item
    val isHelpScreensCustomBottomNavBarEnabled: Boolean = true,
//    net.gini.android.capture.GiniCapture.Builder#setCameraNavigationBarBottomAdapter → on/off switch to show a custom adapter implementation
    val isCameraBottomNavBarEnabled: Boolean = true,
//    net.gini.android.capture.GiniCapture.Builder#setReviewBottomBarNavigationAdapter →  on/off switch to show a custom adapter implementation
    val isReviewScreenCustomBottomNavBarEnabled: Boolean = true,

    val isEventTrackerEnabled: Boolean = true,


    // net.gini.android.capture.GiniCapture.Builder#setCustomErrorLoggerListener → ignore
    //val setCustomErrorLoggerListener: ErrorLoggerListener? = null,

    // net.gini.android.capture.GiniCapture.Builder#setCustomOnboardingPages → on/off switch to show custom onboarding pages
    val isCustomOnboardingPagesEnabled: Boolean = true,


//    net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboarding → on/off switch
    val isOnboardingEnabled: Boolean = true,

//    net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboardingAtFirstRun → on/off switch
    val isOnboardingAtFirstRunEnabled: Boolean = true,


    /*
    net.gini.android.capture.GiniCapture.Builder#setDocumentImportEnabledFileTypes → radio buttons to select an available enum value

    net.gini.android.capture.GiniCapture.Builder#setErrorNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setEventTracker → ignore

    net.gini.android.capture.GiniCapture.Builder#setFileImportEnabled → on/off switch

    net.gini.android.capture.GiniCapture.Builder#setGiniCaptureNetworkService → ignore

    net.gini.android.capture.GiniCapture.Builder#setGiniErrorLoggerIsOn → ignore

    net.gini.android.capture.GiniCapture.Builder#setHelpNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setImportedFileSizeBytesLimit → numeric text field

    net.gini.android.capture.GiniCapture.Builder#setLoadingIndicatorAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setNavigationBarTopAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setNoResultsNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setOnboardingAlignCornersIllustrationAdapternet.gini.android.capture.GiniCapture.Builder#setOnboardingLightingIllustrationAdapternet.gini.android.capture.GiniCapture.Builder#setOnboardingMultiPageIllustrationAdapternet.gini.android.capture.GiniCapture.Builder#setOnboardingNavigationBarBottomAdapternet.gini.android.capture.GiniCapture.Builder#setOnboardingQRCodeIllustrationAdapter-> on/off switch to show custom adapter with animated illustrations

    net.gini.android.capture.GiniCapture.Builder#setOnButtonLoadingIndicatorAdapter →  on/off switch to show a custom adapter implementation



    net.gini.android.capture.GiniCapture.Builder#setSupportedFormatsHelpScreenEnabled → on/off switch*/
) : Parcelable

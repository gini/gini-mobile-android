package net.gini.android.capture.screen.ui.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.DocumentImportEnabledFileTypes


@Parcelize
data class Configuration(
    // net.gini.android.capture.GiniCapture.Builder#setBackButtonsEnabled → ignore (will be removed)

    // 1 open with
//    net.gini.android.capture.GiniCapture.Builder#setFileImportEnabled → on/off switch
    val isFileImportEnabled: Boolean = true,

    // 2 QR code scanning
//    net.gini.android.capture.GiniCapture.Builder#setOnlyQRCodeScanning → on/off switch
    val isQrCodeEnabled: Boolean = true,

    // 3 only QR code scanning
//    net.gini.android.capture.GiniCapture.Builder#setQRCodeScanningEnabled → on/off switch
    val isOnlyQrCodeEnabled: Boolean = false,

    // 4 enable multi page
//    net.gini.android.capture.GiniCapture.Builder#setMultiPageEnabled →  on/off switch
    val isMultiPageEnabled: Boolean = true,

    // 5 enable flash toggle
//    net.gini.android.capture.GiniCapture.Builder#setFlashButtonEnabled → on/off switch
    val isFlashToggleEnabled: Boolean = true,

    // 6 enable flash on by default
//    net.gini.android.capture.GiniCapture.Builder#setFlashOnByDefault → on/off switch
    val isFlashOnByDefault: Boolean = true,

    // 7 set import document type support
//    net.gini.android.capture.GiniCapture.Builder#setDocumentImportEnabledFileTypes → radio buttons to select an available enum value
    val documentImportEnabledFileTypes: DocumentImportEnabledFileTypes = DocumentImportEnabledFileTypes.PDF_AND_IMAGES,

    // 8 enable bottom navigation bar
// net.gini.android.capture.GiniCapture.Builder#setBottomNavigationBarEnabled → on/off switch
    val isBottomNavigationBarEnabled: Boolean = true,

    // 9 enable Help screens custom bottom navigation bar
// net.gini.android.capture.GiniCapture.Builder#setCustomHelpItems → on/off switch to show a custom help item
    val isHelpScreensCustomBottomNavBarEnabled: Boolean = true,

    // 10 enable camera screens custom bottom navigation bar
//    net.gini.android.capture.GiniCapture.Builder#setCameraNavigationBarBottomAdapter → on/off switch to show a custom adapter implementation
    val isCameraBottomNavBarEnabled: Boolean = true,

    // 11 enable review screens custom bottom navigation bar
//    net.gini.android.capture.GiniCapture.Builder#setReviewBottomBarNavigationAdapter →  on/off switch to show a custom adapter implementation
    val isReviewScreenCustomBottomNavBarEnabled: Boolean = true,

    // 12 enable image picker screens custom bottom navigation bar

    // 13 enable onboarding screens at first launch
//    net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboardingAtFirstRun → on/off switch
    val isOnboardingAtFirstRunEnabled: Boolean = true,

    // 14 enable onboarding at every launch
//    net.gini.android.capture.GiniCapture.Builder#setShouldShowOnboarding → on/off switch
    val isOnboardingAtEveryLaunchEnabled: Boolean = false,

    // 15 enable custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setCustomOnboardingPages → on/off switch to show custom onboarding pages
    val isCustomOnboardingPagesEnabled: Boolean = false,

    // 16 enable align corners in custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setOnboardingAlignCornersIllustrationAdapter
    val isAlignCornersInCustomOnboardingEnabled: Boolean = false,

    // 17 enable lighting in custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setOnboardingLightingIllustrationAdapter
    val isLightingInCustomOnboardingEnabled: Boolean = false,

    // 18 enable QR code in custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setOnboardingQRCodeIllustrationAdapter-> on/off switch to show custom adapter with animated illustrations
    val isQRCodeInCustomOnboardingEnabled: Boolean = false,

    // 19 enable multi page in custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setOnboardingMultiPageIllustrationAdapter
    val isMultiPageInCustomOnboardingEnabled: Boolean = false,

    // 20 enable custom navigation bar in custom onboarding pages
//    net.gini.android.capture.GiniCapture.Builder#setOnboardingNavigationBarBottomAdapter
    val isCustomNavigationBarInCustomOnboardingEnabled: Boolean = false,


    // 21 enable button's custom loading indicator
//    net.gini.android.capture.GiniCapture.Builder#setOnButtonLoadingIndicatorAdapter →  on/off switch to show a custom adapter implementation
    val isButtonsCustomLoadingIndicatorEnabled: Boolean = false,

    // 22 enable screen's custom loading indicator
    val isScreenCustomLoadingIndicatorEnabled: Boolean = false,

    // 23 enable supported format help screen
    //    net.gini.android.capture.GiniCapture.Builder#setSupportedFormatsHelpScreenEnabled → on/off switch
    val isSupportedFormatsHelpScreenEnabled: Boolean = true,

    val isEventTrackerEnabled: Boolean = true,


//    net.gini.android.capture.GiniCapture.Builder#setCustomErrorLoggerListener → ignore
    //val setCustomErrorLoggerListener: ErrorLoggerListener? = null,




    /*

    net.gini.android.capture.GiniCapture.Builder#setErrorNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setEventTracker → ignore


    net.gini.android.capture.GiniCapture.Builder#setGiniCaptureNetworkService → ignore

    net.gini.android.capture.GiniCapture.Builder#setGiniErrorLoggerIsOn → ignore

    net.gini.android.capture.GiniCapture.Builder#setHelpNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setImportedFileSizeBytesLimit → numeric text field

    net.gini.android.capture.GiniCapture.Builder#setLoadingIndicatorAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setNavigationBarTopAdapter →  on/off switch to show a custom adapter implementation

    net.gini.android.capture.GiniCapture.Builder#setNoResultsNavigationBarBottomAdapter →  on/off switch to show a custom adapter implementation





 */
) : Parcelable

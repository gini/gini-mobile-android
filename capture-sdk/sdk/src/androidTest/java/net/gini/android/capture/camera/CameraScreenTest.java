package net.gini.android.capture.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.os.RemoteException;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.RequiresDevice;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import net.gini.android.capture.Document;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivityTestSpy;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.document.QRCodeDocumentHelper;
import net.gini.android.capture.internal.camera.api.CameraControllerFake;
import net.gini.android.capture.internal.camera.photo.PhotoFactory;
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData;
import net.gini.android.capture.network.GiniCaptureNetworkApi;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.onboarding.OnboardingPage;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.review.ReviewActivityTestSpy;
import net.gini.android.capture.test.EspressoAssertions;
import net.gini.android.capture.test.PermissionsHelper;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static net.gini.android.capture.OncePerInstallEventStoreHelper.clearOnboardingWasShownPreference;
import static net.gini.android.capture.OncePerInstallEventStoreHelper.setOnboardingWasShownPreference;
import static net.gini.android.capture.camera.CameraFragmentImpl.GC_SHARED_PREFS;
import static net.gini.android.capture.camera.CameraFragmentImpl.SHOW_QRCODE_SCANNER_HINT_POP_UP;
import static net.gini.android.capture.camera.CameraFragmentImpl.SHOW_UPLOAD_HINT_POP_UP;
import static net.gini.android.capture.test.EspressoMatchers.hasComponent;
import static net.gini.android.capture.test.Helpers.convertJpegToNV21;
import static net.gini.android.capture.test.Helpers.isTablet;
import static net.gini.android.capture.test.Helpers.loadAsset;
import static net.gini.android.capture.test.Helpers.prepareLooper;
import static net.gini.android.capture.test.Helpers.resetDeviceOrientation;
import static net.gini.android.capture.test.Helpers.waitForWindowUpdate;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class CameraScreenTest {

    private static final int PAUSE_DURATION = 500;
    private static final long CLOSE_CAMERA_PAUSE_DURATION = 1000;
    private static final long TAKE_PICTURE_PAUSE_DURATION = 4000;

    @Before
    public void setup() throws Exception {
        prepareLooper();
        CameraFragmentHostActivityNotListener.sListener = null;
        PermissionsHelper.grantCameraPermission();
        getNewGiniCaptureInstanceBuilder().build();
    }

    private GiniCapture.Builder getNewGiniCaptureInstanceBuilder() {
        GiniCapture.cleanup(ApplicationProvider.getApplicationContext());
        return GiniCapture.newInstance()
                .setGiniCaptureNetworkService(mock(GiniCaptureNetworkService.class))
                .setGiniCaptureNetworkApi(mock(GiniCaptureNetworkApi.class));
    }

    @After
    public void teardown() throws Exception {
        clearOnboardingWasShownPreference();
        // Wait a little for the camera to close
        Thread.sleep(CLOSE_CAMERA_PAUSE_DURATION);
        resetDeviceOrientation();
        GiniCapture.cleanup(ApplicationProvider.getApplicationContext());
        Intents.release();
    }

    @Test
    public void should_showOnboarding_onFirstLaunch_ifNotDisabled() {
        try (final ActivityScenario<CameraActivity> scenario = ActivityScenario.launch(CameraActivity.class)) {
            Espresso.onView(ViewMatchers.withId(R.id.gc_onboarding_viewpager))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void should_notShowOnboarding_onFirstLaunch_ifDisabled() {
        startCameraActivityWithoutOnboarding();

        Espresso.onView(ViewMatchers.withId(R.id.gc_onboarding_viewpager))
                .check(ViewAssertions.doesNotExist());
    }

    @Test
    public void should_notShowOnboarding_onFirstLaunch_ifDisabledUsingGiniCapture() {
        getNewGiniCaptureInstanceBuilder()
                .setShouldShowOnboardingAtFirstRun(false)
                .build();

        try (final ActivityScenario<CameraActivity> scenario = ActivityScenario.launch(CameraActivity.class)) {
            Espresso.onView(ViewMatchers.withId(R.id.gc_onboarding_viewpager))
                    .check(ViewAssertions.doesNotExist());
        }
    }

    @NonNull
    private ActivityScenario<CameraActivity> startCameraActivityWithoutOnboarding() {
        getNewGiniCaptureInstanceBuilder()
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        return ActivityScenario.launch(CameraActivity.class);
    }

    @NonNull
    private ActivityScenario<CameraActivityFake> startCameraActivityFakeWithoutOnboarding() {
        getNewGiniCaptureInstanceBuilder()
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        return ActivityScenario.launch(CameraActivityFake.class);
    }

    @Test
    public void should_showOnboarding_ifRequested_andWasAlreadyShownOnFirstLaunch() {
        setOnboardingWasShownPreference();

        getNewGiniCaptureInstanceBuilder()
                .setShouldShowOnboarding(true)
                .build();

        try (final ActivityScenario<CameraActivity> scenario = ActivityScenario.launch(CameraActivity.class)) {
            Espresso.onView(ViewMatchers.withId(R.id.gc_onboarding_viewpager))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void should_passCustomOnboardingPages_toOnboardingActivity()
            throws Exception {
        final ArrayList<OnboardingPage> onboardingPages = new ArrayList<>(1);
        onboardingPages.add(
                new OnboardingPage(R.string.gc_onboarding_align, R.drawable.gc_onboarding_align));

        getNewGiniCaptureInstanceBuilder()
                .setCustomOnboardingPages(onboardingPages)
                .build();

        try (final ActivityScenario<CameraActivity> scenario = ActivityScenario.launch(CameraActivity.class)) {
            Intents.init();

            scenario.onActivity(activity -> {
                activity.startOnboardingActivity();
            });

            Intents.intended(IntentMatchers.hasComponent(OnboardingActivity.class.getName()));
        }
    }

    @Test
    public void should_showOnboarding_whenOnboardingMenuItem_wasTapped() {
        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            Intents.init();

            scenario.onActivity(activity -> {
                activity.startOnboardingActivity();
            });

            Intents.intended(IntentMatchers.hasComponent(OnboardingActivity.class.getName()));
        }
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void should_showNoPermissionView_ifNoCameraPermission() throws Exception {
        PermissionsHelper.revokeCameraPermission();

        // Gini Capture SDK does not handle runtime permissions and the no permission view is
        // shown by default
        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            Espresso.onView(ViewMatchers.withId(R.id.gc_layout_camera_no_permission))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void should_showCameraPreview_afterCameraPermission_wasGranted() throws Exception {
        PermissionsHelper.revokeCameraPermission();

        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {

            final UiDevice uiDevice = UiDevice.getInstance(
                    InstrumentationRegistry.getInstrumentation());

            // Open the Application Details in the Settings
            Espresso.onView(ViewMatchers.withId(R.id.gc_button_camera_no_permission))
                    .perform(ViewActions.click());

            // Open the Permissions settings
            final UiObject permissionsItem = uiDevice.findObject(new UiSelector().text("Permissions"));
            permissionsItem.clickAndWaitForNewWindow();

            // Grant Camera permission
            final UiObject cameraItem = uiDevice.findObject(new UiSelector().text("Camera"));
            if (!cameraItem.isChecked()) {
                cameraItem.click();
            }

            // Go back to our test app
            uiDevice.pressBack();
            uiDevice.pressBack();

            // Verifiy that the no permission view was removed
            Espresso.onView(ViewMatchers.withId(R.id.gc_layout_camera_no_permission))
                    .check(ViewAssertions.matches(
                            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

            // Verify that the camera preview is visible
            Espresso.onView(ViewMatchers.withId(R.id.gc_camera_preview_container))
                    .check(ViewAssertions.matches(ViewMatchers.hasChildCount(1)));
        }
    }

    @RequiresDevice
    @Test
    public void should_showReviewScreen_afterPictureWasTaken() throws InterruptedException {
        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            Intents.init();

            Espresso.onView(ViewMatchers.withId(R.id.gc_button_camera_trigger))
                    .perform(ViewActions.click());

            // Give some time for the camera to take a picture
            Thread.sleep(TAKE_PICTURE_PAUSE_DURATION);

            Intents.intended(IntentMatchers.hasComponent(ReviewActivity.class.getName()));
        }
    }

    @RequiresDevice
    @Test
    public void should_takeOnlyOnePicture_ifTrigger_wasPressedMultipleTimes()
            throws InterruptedException {
        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            Intents.init();

            Espresso.onView(ViewMatchers.withId(R.id.gc_button_camera_trigger))
                    .perform(ViewActions.doubleClick());

            // Give some time for the camera to take a picture
            Thread.sleep(TAKE_PICTURE_PAUSE_DURATION);

            Intents.intended(IntentMatchers.hasComponent(ReviewActivity.class.getName()));
        }
    }

    @Test
    public void
    should_notFinish_whenReceivingActivityResult_withResultCodeCancelled_fromReviewActivity() {
        final CameraActivity cameraActivitySpy = Mockito.spy(new CameraActivity());

        cameraActivitySpy.onActivityResult(CameraActivity.REVIEW_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(cameraActivitySpy, never()).finish();
    }

    @Test
    public void
    should_finishIfEnabledByClient_whenReceivingActivityResult_withResultCodeCancelled_fromReviewActivity() {
        final CameraActivity cameraActivity = new CameraActivity();
        final CameraActivity cameraActivitySpy = Mockito.spy(cameraActivity);

        cameraActivitySpy.onActivityResult(CameraActivity.REVIEW_DOCUMENT_REQUEST,
                Activity.RESULT_CANCELED, new Intent());

        verify(cameraActivitySpy).finish();
    }

    @RequiresDevice
    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_adaptCameraPreviewSize_toLandscapeOrientation_onTablets() throws Exception {
        // Given
        assumeTrue(isTablet());

        final UiDevice uiDevice = UiDevice.getInstance(
                InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationNatural();
        waitForWindowUpdate(uiDevice);

        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            scenario.onActivity(activity -> {
                final View cameraPreview = activity.findViewById(R.id.gc_camera_preview_container);
                final int initialWidth = cameraPreview.getWidth();
                final int initialHeight = cameraPreview.getHeight();

                // When
                try {
                    uiDevice.setOrientationRight();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    fail("Failed to change device orientation");
                }
                waitForWindowUpdate(uiDevice);

                // Then
                // Preview should have the reverse aspect ratio
                Espresso.onView(
                        ViewMatchers.withId(R.id.gc_camera_preview_container)).check(
                        EspressoAssertions.hasSizeRatio((float) initialHeight / initialWidth));
            });
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_forcePortraitOrientation_onPhones() throws Exception {
        // Given
        assumeTrue(!isTablet());

        final UiDevice uiDevice = UiDevice.getInstance(
                InstrumentationRegistry.getInstrumentation());
        uiDevice.setOrientationLeft();
        waitForWindowUpdate(uiDevice);

        try (final ActivityScenario<CameraActivity> scenario = startCameraActivityWithoutOnboarding()) {
            waitForWindowUpdate(uiDevice);

            // Then
            scenario.onActivity(activity -> {
                final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                assertThat(rotation)
                        .isEqualTo(Surface.ROTATION_0);
            });
        }
    }

    @NonNull
    private ArgumentMatcher<Intent> intentWithExtraBackButtonShouldCloseLibrary() {
        return new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(final Intent intent) {
                final boolean shouldCloseLibrary = intent.getBooleanExtra(
                        ReviewActivity.EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, false);
                return shouldCloseLibrary;
            }

            @Override
            public String toString() {
                return "Intent { EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY=true }";
            }
        };
    }

    @Test
    public void should_detectBezahlCode_andShowPopup_andReturnPaymentData_whenPopupClicked()
            throws IOException, InterruptedException {
        detectAndCheckQRCode("qrcode_bezahlcode.jpeg", "qrcode_bezahlcode_nv21.bmp",
                new PaymentQRCodeData(
                        PaymentQRCodeData.Format.BEZAHL_CODE,
                        "bank://singlepaymentsepa?name=GINI%20GMBH&reason=BezahlCode%20Test&iban=DE27100777770209299700&bic=DEUTDEMMXXX&amount=140%2C4",
                        "GINI GMBH",
                        "BezahlCode Test",
                        "DE27100777770209299700",
                        "DEUTDEMMXXX",
                        "140.40:EUR"));
    }

    private void detectAndCheckQRCode(@NonNull final String jpegFilename,
                                      @NonNull final String nv21Filename, @NonNull final PaymentQRCodeData paymentData)
            throws IOException, InterruptedException {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);


            detectQRCode(activity, jpegFilename, nv21Filename);

            // When
            Thread.sleep(PAUSE_DURATION);
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .perform(ViewActions.click());

            // Then
            final QRCodeDocument qrCodeDocument = activity.getCameraFragmentImplFake().getQRCodeDocument();
            final PaymentQRCodeData actualPaymentData;
            if (qrCodeDocument != null) {
                actualPaymentData = QRCodeDocumentHelper.getPaymentData(qrCodeDocument);
            } else {
                actualPaymentData = activity.getCameraFragmentImplFake().getPaymentQRCodeData();
            }
            assertThat(actualPaymentData).isEqualTo(paymentData);
        }
    }

    private void disableHintPopups() {
        final SharedPreferences gcSharedPrefs = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(GC_SHARED_PREFS, Context.MODE_PRIVATE);
        gcSharedPrefs.edit()
                .putBoolean(SHOW_QRCODE_SCANNER_HINT_POP_UP, false)
                .putBoolean(SHOW_UPLOAD_HINT_POP_UP, false)
                .apply();
    }

    private void detectQRCode(
            final CameraActivityFake cameraActivityFake,
            @NonNull final String jpegFilename,
            @NonNull final String nv21Filename)
            throws IOException {
        final CameraControllerFake cameraControllerFake =
                cameraActivityFake.getCameraControllerFake();
        assertThat(cameraControllerFake.getPreviewCallback()).isNotNull();
        cameraControllerFake.showImageAsPreview(loadAsset(jpegFilename), loadAsset(nv21Filename));
    }

    @Test
    public void should_detectEPC069_andShowPopup_andReturnPaymentData_whenPopupClicked()
            throws IOException, InterruptedException {
        convertJpegToNV21("qrcode_eps_payment.jpg", "qrcode_eps_payment_nv21.bmp");
        detectAndCheckQRCode("qrcode_epc069_12.jpeg", "qrcode_epc069_12_nv21.bmp",
                new PaymentQRCodeData(
                        PaymentQRCodeData.Format.EPC069_12,
                        "BCD\n001\n2\nSCT\nSOLADES1PFD\nGirosolution GmbH\nDE19690516200000581900\nEUR140.4\n\n\nBezahlCode Test",
                        "Girosolution GmbH",
                        "BezahlCode Test",
                        "DE19690516200000581900",
                        "SOLADES1PFD",
                        "140.40:EUR"));
    }

    @Test
    public void should_detectEpsPayment_andShowPopup_andReturnPaymentData_whenPopupClicked()
            throws IOException, InterruptedException {
        detectAndCheckQRCode("qrcode_eps_payment.jpg", "qrcode_eps_payment_nv21.bmp",
                new PaymentQRCodeData(
                        PaymentQRCodeData.Format.EPS_PAYMENT,
                        "epspayment://eps.or.at/?transactionid=epsJUJQQV9U2",
                        null, null, null, null, null));
    }

    @Test
    public void should_showQRCodeNotSupported_forIFSC_QRCode() throws Exception {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);

            // When
            detectQRCode(activity, "qrcode_unsupported_ifsc.jpeg", "qrcode_unsupported_ifsc_nv21.bmp");

            // Then
            Thread.sleep(PAUSE_DURATION);
            Espresso.onView(ViewMatchers.withText(R.string.gc_unsupported_qrcode_detected_popup_message_1))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    @Test
    public void should_detectQRCode_whenConfiguredUsingGiniCapture()
            throws IOException, InterruptedException {
        detectAndCheckQRCode("qrcode_epc069_12.jpeg", "qrcode_epc069_12_nv21.bmp",
                new PaymentQRCodeData(
                        PaymentQRCodeData.Format.EPC069_12,
                        "BCD\n001\n2\nSCT\nSOLADES1PFD\nGirosolution GmbH\nDE19690516200000581900\nEUR140.4\n\n\nBezahlCode Test",
                        "Girosolution GmbH",
                        "BezahlCode Test",
                        "DE19690516200000581900",
                        "SOLADES1PFD",
                        "140.40:EUR")
                );
    }

    @Test
    public void should_hidePaymentDataDetectedPopup_afterSomeDelay()
            throws IOException, InterruptedException {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);

            // When
            detectQRCode(activity, "qrcode_bezahlcode.jpeg", "qrcode_bezahlcode_nv21.bmp");

            // Then
            final long hideDelay = activity.getCameraFragmentImplFake().getHideQRCodeDetectedPopupDelayMs();
            Thread.sleep(hideDelay + PAUSE_DURATION);
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .check(ViewAssertions.matches(ViewMatchers.withAlpha(0)));
        }
    }

    @Test
    public void should_hideAndShowPaymentDataDetectedPopup_whenNewPaymentData_wasDetected()
            throws IOException, InterruptedException {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);

            // When
            detectQRCode(activity, "qrcode_bezahlcode.jpeg", "qrcode_bezahlcode_nv21.bmp");

            // Then
            Thread.sleep(PAUSE_DURATION);
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .check(ViewAssertions.matches(ViewMatchers.withAlpha(1)));

            // When
            detectQRCode(activity, "qrcode_epc069_12.jpeg", "qrcode_epc069_12_nv21.bmp");

            // Then
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .check(ViewAssertions.matches(ViewMatchers.withAlpha(1)));
        }
    }

    @Test
    public void should_notShowPaymentDataDetectedPopup_whenInterfaceIsHidden()
            throws Throwable {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);

            activity.hideInterface();

            // When
            detectQRCode(activity, "qrcode_bezahlcode.jpeg", "qrcode_bezahlcode_nv21.bmp");

            // Then
            Thread.sleep(CameraFragmentImpl.DEFAULT_ANIMATION_DURATION + 100);
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .check(ViewAssertions.matches(ViewMatchers.withAlpha(0)));
        }
    }

    @Test
    public void should_notShowPaymentDataDetectedPopup_whenDocumentUploadHint_isShown()
            throws Throwable {
        // Given
        assumeTrue(!isTablet());
        getNewGiniCaptureInstanceBuilder()
                .setQRCodeScanningEnabled(true)
                .setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes.PDF_AND_IMAGES)
                .setShouldShowOnboardingAtFirstRun(false)
                .build();
        disableHintPopups();

        try (final ActivityScenario<CameraActivityFake> scenario = ActivityScenario.launch(CameraActivityFake.class)) {
            final AtomicReference<CameraActivityFake> reference = new AtomicReference<>();

            scenario.onActivity(reference::set);

            CameraActivityFake activity;
            do {
                activity = reference.get();
            } while (activity == null);

            // When
            detectQRCode(activity, "qrcode_bezahlcode.jpeg", "qrcode_bezahlcode_nv21.bmp");

            // Then
            Thread.sleep(CameraFragmentImpl.DEFAULT_ANIMATION_DURATION + 100);
            Espresso.onView(ViewMatchers.withId(R.id.gc_qrcode_detected_popup_container))
                    .check(ViewAssertions.matches(ViewMatchers.withAlpha(0)));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenListener_wasNotSet() {
        final CameraFragmentCompat cameraFragment = CameraFragmentCompat.createInstance();
        cameraFragment.onCreate(null);
    }

    @Test
    public void should_useExplicitListener_whenActivity_isNotListener() throws Exception {
        // Given
        final AtomicBoolean isDocumentAvailable = new AtomicBoolean();
        CameraFragmentHostActivityNotListener.sListener = new CameraFragmentListener() {
            @Override
            public void onDocumentAvailable(@NonNull final Document document) {
                isDocumentAvailable.set(true);
            }

            @Override
            public void onProceedToMultiPageReviewScreen(
                    @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {

            }

            @Override
            public void onCheckImportedDocument(@NonNull final Document document,
                    @NonNull final DocumentCheckResultCallback callback) {

            }

            @Override
            public void onError(@NonNull final GiniCaptureError error) {

            }

            @Override
            public void onExtractionsAvailable(
                    @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {

            }
        };

        try (final ActivityScenario<CameraFragmentHostActivityNotListener> scenario = ActivityScenario.launch(CameraFragmentHostActivityNotListener.class)) {
            scenario.onActivity(activity -> {
                // When
                try {
                    activity.getFragment().getCameraControllerFake()
                            .showImageAsPreview(loadAsset("invoice.jpg"), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("Failed to show image as preview");
                }

                activity.getFragment()
                        .getCameraFragmentImplFake().mButtonCameraTrigger.performClick();

                // Then
                assertThat(isDocumentAvailable.get()).isTrue();
            });
        }
    }

    @Test
    public void should_useActivity_asListener_whenAvailable() throws Exception {
        // Given
        try (final ActivityScenario<CameraFragmentHostActivity> scenario = ActivityScenario.launch(CameraFragmentHostActivity.class)) {
            final AtomicReference<CameraFragmentHostActivity> reference = new AtomicReference<>();
            scenario.onActivity(activity -> {
                // When
                try {
                    activity.getFragment().getCameraControllerFake()
                            .showImageAsPreview(loadAsset("invoice.jpg"), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("Failed to show image as preview");
                }

                activity.getFragment()
                        .getCameraFragmentImplFake().mButtonCameraTrigger.performClick();

                // Then
                reference.set(activity);
            });

            // Then
            Thread.sleep(PAUSE_DURATION);

            CameraFragmentHostActivity activity;
            do {
                activity = reference.get();
            } while (activity == null);

            assertThat(activity.hasDocument()).isTrue();
        }
    }

    @Test
    public void should_turnOffFlash_whenRequested() {
        // Given
        getNewGiniCaptureInstanceBuilder()
                .setFlashOnByDefault(false)
                .build();
        // When
        try (final ActivityScenario<CameraActivityFake> scenario = startCameraActivityFakeWithoutOnboarding()) {
            scenario.onActivity(activity -> {
                // Then
                assertThat(activity.getCameraControllerFake().isFlashEnabled()).isFalse();
            });
        }
    }

    @Test
    public void should_turnOnFlashByDefault_ifNotChanged() {
        // When
        try (final ActivityScenario<CameraActivityFake> scenario = startCameraActivityFakeWithoutOnboarding()) {
            scenario.onActivity(activity -> {
                // Then
                assertThat(activity.getCameraControllerFake().isFlashEnabled()).isTrue();
            });
        }
    }
}
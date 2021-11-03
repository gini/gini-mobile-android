package net.gini.android.capture.screen.screenshots;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.capture.screen.screenshots.Helper.openApp;
import static net.gini.android.capture.screen.screenshots.ScreenshotHelper.screenshotFileForBitBar;
import static net.gini.android.capture.screen.screenshots.ScreenshotHelper.takeUIAutomatorScreenshot;
import static net.gini.android.capture.screen.testhelper.PermissionsHelper.grantCameraPermission;

import android.os.RemoteException;

import net.gini.android.capture.screen.screenshots.screens.screenapi.AnalysisScreen;
import net.gini.android.capture.screen.screenshots.screens.screenapi.CameraScreen;
import net.gini.android.capture.screen.screenshots.screens.screenapi.MainScreen;
import net.gini.android.capture.screen.screenshots.screens.screenapi.OnboardingScreen;
import net.gini.android.capture.screen.screenshots.screens.screenapi.ReviewScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;

@SdkSuppress(minSdkVersion = 18)
@RunWith(AndroidJUnit4.class)
public class ScreenshotsTest {

    private static final String SCREEN_API_EXAMPLE_APP = "net.gini.android.capture.screenapiexample";

    private UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        assertThat(mDevice).isNotNull();
        // Start from the home screen
        mDevice.pressHome();
    }

    @Test
    public void screenApiExample_takeScreenshots_naturalOrientation()
            throws InterruptedException, UiObjectNotFoundException, RemoteException {

        grantCameraPermission(SCREEN_API_EXAMPLE_APP);
        openApp(SCREEN_API_EXAMPLE_APP, mDevice);

        mDevice.setOrientationNatural();
        mDevice.waitForWindowUpdate(SCREEN_API_EXAMPLE_APP, 5000);

        takeScreenshots("normal-");
    }

    @Test
    public void screenApiExample_takeScreenshots_rightOrientation()
            throws InterruptedException, UiObjectNotFoundException, RemoteException {
        grantCameraPermission(SCREEN_API_EXAMPLE_APP);
        openApp(SCREEN_API_EXAMPLE_APP, mDevice);

        mDevice.setOrientationLeft();
        mDevice.waitForWindowUpdate(SCREEN_API_EXAMPLE_APP, 5000);

        takeScreenshots("right-");
    }

    private void takeScreenshots(final String namePrefix)
            throws UiObjectNotFoundException, InterruptedException {
        final MainScreen mainScreen = new MainScreen(mDevice);
        final CameraScreen cameraScreen = new CameraScreen(mDevice);
        final ReviewScreen reviewScreen = new ReviewScreen(mDevice);
        final AnalysisScreen analysisScreen = new AnalysisScreen(mDevice);

        // Main Screen
        assertThat(mainScreen.isVisible()).named("Main Screen is displayed").isTrue();
        mainScreen.startGiniCapture();

        // Camera Screen
        assertThat(cameraScreen.isVisible()).named("Camera Screen is displayed").isTrue();

        // Onboarding Screen
        if (cameraScreen.isOnboardingVisible()) {
            takeOnboardingScreenshots(namePrefix);
        } else {
            cameraScreen.showOnboarding();
            takeOnboardingScreenshots(namePrefix);
        }

        // Camera Screen
        takeScreenshot(namePrefix + "CameraScreen");
        cameraScreen.triggerCamera();

        // Review Screen
        assertThat(reviewScreen.isVisible()).named("Review Screen is displayed").isTrue();
        takeScreenshot(namePrefix + "ReviewScreen");

        reviewScreen.proceedToAnalysis();

        // Analysis Screen
        assertThat(analysisScreen.isVisible()).named("Analysis Screen is displayed").isTrue();
        takeScreenshot(namePrefix + "AnalysisScreen");
    }

    private void takeOnboardingScreenshots(final String namePrefix)
            throws InterruptedException, UiObjectNotFoundException {
        final OnboardingScreen onboardingScreen = new OnboardingScreen(mDevice);

        assertThat(onboardingScreen.isVisible()).named("Onboarding Screen is displayed").isTrue();
        takeScreenshot(namePrefix + "OnboardingScreen-Page1");

        onboardingScreen.goToNextPage();
        takeScreenshot(namePrefix + "OnboardingScreen-Page2");

        onboardingScreen.goToNextPage();
        takeScreenshot(namePrefix + "OnboardingScreen-Page3");

        onboardingScreen.closeSelf();
    }

    private void takeScreenshot(final String name) throws InterruptedException {
        final File screenshotFile = screenshotFileForBitBar(name);
        takeUIAutomatorScreenshot(screenshotFile, mDevice);
    }

}

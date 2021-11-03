package net.gini.android.capture.analysis;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.capture.test.DocumentSubject.document;
import static net.gini.android.capture.test.Helpers.createDocument;
import static net.gini.android.capture.test.Helpers.getTestJpeg;
import static net.gini.android.capture.test.Helpers.isTablet;
import static net.gini.android.capture.test.Helpers.resetDeviceOrientation;
import static net.gini.android.capture.test.Helpers.waitForWindowUpdate;

import static org.junit.Assume.assumeTrue;

import android.content.Intent;
import android.view.Surface;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.PhotoFactory;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.review.ReviewActivity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

@RunWith(AndroidJUnit4.class)
public class AnalysisScreenTest {

    private static final long TEST_PAUSE_DURATION = 500;

    private static byte[] TEST_JPEG = null;

    @Rule
    public ActivityTestRule<AnalysisActivityTestSpy> mActivityTestRule = new ActivityTestRule<>(
            AnalysisActivityTestSpy.class, true, false);
    @Rule
    public ActivityTestRule<AnalysisFragmentHostActivityNotListener>
            mAnalysisFragmentHostActivityNotListenerTR = new ActivityTestRule<>(
            AnalysisFragmentHostActivityNotListener.class, true, false);
    @Rule
    public ActivityTestRule<AnalysisFragmentHostActivity>
            mAnalysisFragmentHostActivityTR = new ActivityTestRule<>(
            AnalysisFragmentHostActivity.class, true, false);

    @Rule
    public IntentsTestRule<AnalysisActivity> mIntentsTestRule = new IntentsTestRule<>(
            AnalysisActivity.class, true, false);

    @BeforeClass
    public static void setupClass() throws IOException {
        TEST_JPEG = getTestJpeg();
    }

    @AfterClass
    public static void teardownClass() throws IOException {
        TEST_JPEG = null;
    }

    @After
    public void tearDown() throws Exception {
        resetDeviceOrientation();
        AnalysisFragmentHostActivityNotListener.sListener = null;
    }

    @Test
    public void should_invokeAnalyzeDocument_whenLaunched() throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        assertThat(activity.analyzeDocument).isNotNull();

        assertAbout(document()).that(activity.analyzeDocument).isEqualToDocument(
                DocumentFactory.newImageDocumentFromPhoto(
                        PhotoFactory.newPhotoFromJpeg(TEST_JPEG, 0, "portrait", "phone",
                                ImageDocument.Source.newCameraSource())));
    }

    @Test
    public void should_notInvokeAddDataToResult_whenFinished_withoutDocumentAnalyzed_beingCalled()
            throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        activity.finish();

        assertThat(activity.addDataToResultIntent).isNull();
        assertThat(activity.finishWasCalled).isTrue();
    }

    @Test
    public void should_notInvokeAddDataToResult_whenHomeButton_wasPressed()
            throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        // Click home (back)
        Espresso.onView(ViewMatchers.withContentDescription("Navigate up"))
                .perform(ViewActions.click());

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.addDataToResultIntent).isNull();
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    public void should_notInvokeAddDataToResult_whenBackButton_wasPressed()
            throws InterruptedException {
        final AnalysisActivityTestSpy activity = startAnalysisActivity(TEST_JPEG, 0);

        // Allow the activity to run a little for listeners to be invoked
        Thread.sleep(TEST_PAUSE_DURATION);

        // Click back
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertThat(activity.addDataToResultIntent).isNull();
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

        final AnalysisActivity analysisActivity = startAnalysisActivity(TEST_JPEG, 90);
        waitForWindowUpdate(uiDevice);

        // Then
        final int rotation = analysisActivity.getWindowManager().getDefaultDisplay().getRotation();
        assertThat(rotation).isEqualTo(Surface.ROTATION_0);
    }

    private AnalysisActivityTestSpy startAnalysisActivity(final byte[] jpeg,
            final int orientation) {
        final Intent intent = getAnalysisActivityIntent();
        addDocumentExtraToIntent(intent, jpeg, orientation);
        return mActivityTestRule.launchActivity(intent);
    }

    private Intent getAnalysisActivityIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                AnalysisActivityTestSpy.class);
    }

    private void addDocumentExtraToIntent(final Intent intent, final byte[] jpeg,
            final int orientation) {
        intent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, DocumentFactory.newImageDocumentFromPhoto(
                PhotoFactory.newPhotoFromJpeg(jpeg, orientation, "portrait", "phone",
                        ImageDocument.Source.newCameraSource())));
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_whenListener_wasNotSet() throws Exception {
        final AnalysisFragmentCompat fragment = AnalysisFragmentCompat.createInstance(
                createDocument(getTestJpeg(), 0, "portrait", "phone", ImageDocument.Source.newCameraSource()), null);
        fragment.onCreate(null);
    }

    @Test
    public void should_useExplicitListener_whenActivity_isNotListener() throws Exception {
        // Given
        final AtomicBoolean analysisRequested = new AtomicBoolean();
        AnalysisFragmentHostActivityNotListener.sListener = new AnalysisFragmentListener() {

            @Override
            public void onError(@NonNull final GiniCaptureError error) {

            }

            @Override
            public void onExtractionsAvailable(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                                               @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                                               @NonNull final List<GiniCaptureReturnReason> returnReasons) {

            }

            @Override
            public void onProceedToNoExtractionsScreen(@NonNull final Document document) {

            }

            @Override
            public void onDefaultPDFAppAlertDialogCancelled() {

            }
        };
        final Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AnalysisFragmentHostActivityNotListener.class);
        final AnalysisFragmentHostActivityNotListener activity =
                mAnalysisFragmentHostActivityNotListenerTR.launchActivity(intent);
        // When
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Then
        assertThat(analysisRequested.get()).isTrue();
    }

}

package net.gini.android.capture.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.DocumentImportEnabledFileTypes;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureCoordinator;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.help.HelpActivity;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.review.multipage.MultiPageReviewActivity;
import net.gini.android.capture.tracking.CameraScreenEvent;

import java.util.Map;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboarding;
import static net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboardingAtFirstRun;
import static net.gini.android.capture.review.ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackCameraScreenEvent;

/**
 * <h3>Screen API</h3>
 *
 * <p> {@code CameraActivity} is the main entry point to the Gini Capture SDK when using the
 * Screen API.
 *
 * <p> It shows a camera preview with tap-to-focus functionality, a trigger button and an optional
 * flash on/off button. The camera preview also shows document corner guides to which the user
 * should align the document.
 *
 * <p> On tablets in landscape orientation the camera trigger button is shown on the right side of
 * the screen for easier access.
 *
 * <p> If you enabled document import with {@link GiniCapture.Builder#setDocumentImportEnabledFileTypes(DocumentImportEnabledFileTypes)}
 * then a button for importing documents is shown next to the trigger button. A hint popup is
 * displayed the first time the Gini Capture SDK is used to inform the user about document
 * importing.
 *
 * <p> For importing documents {@code READ_EXTERNAL_STORAGE} permission is required and if the
 * permission is not granted the Gini Capture SDK will prompt the user to grant the permission.
 * See {@code Customizing the Camera Screen} on how to override the message and button titles for
 * the rationale and on permission denial alerts.
 *
 * <p> Start the {@code CameraActivity} with {@link android.app.Activity#startActivityForResult(Intent,
 * int)} to receive the extractions or a {@link GiniCaptureError} in case there was an error.
 *
 * <p> The following result codes need to be handled:
 *
 * <ul>
 *
 * <li>{@link CameraActivity#RESULT_OK} - image of a document was taken, reviewed and analyzed
 *
 * <li>{@link CameraActivity#RESULT_CANCELED} - image of document was not taken, user canceled the
 * Gini Capture SDK
 *
 * <li>{@link CameraActivity#RESULT_ERROR} - an error occured
 *
 * </ul>
 *
 * <p> Result extra returned by the {@code CameraActivity}:
 *
 * <ul>
 *
 * <li>{@link CameraActivity#EXTRA_OUT_EXTRACTIONS} - set when result is {@link
 * CameraActivity#RESULT_OK}, contains a Bundle with the extraction labels as keys and {@link
 * GiniCaptureSpecificExtraction} as values.
 *
 * <li>{@link CameraActivity#EXTRA_OUT_ERROR} - set when result is {@link
 * CameraActivity#RESULT_ERROR}, contains a {@link GiniCaptureError} object detailing what went
 * wrong
 *
 * </ul>
 *
 * <p> If the camera could not be opened due to missing permissions, the content of the Camera
 * Screen is replaced with a no-camera icon, a short message and an optional button. The button is
 * shown only on Android 6.0+ and tapping the button leads the user to the Application Details page
 * in the Settings. If these are shown on Android 5.0 and earlier means that the camera permission
 * was not declared in your manifest.
 *
 * <h3>Customizing the Camera Screen</h3>
 *
 * <p> Customizing the look of the Camera Screen is done via overriding of app resources.
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li> <b>Document corner guides:</b> via the color resource named {@code
 * gc_camera_preview_corners}
 *
 * <li> <b>Camera trigger button:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_camera_trigger_default.png} and {@code gc_camera_trigger_pressed.png}
 *
 * <li> <b>Document import button:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 * {@code gc_document_import_icon.png}
 *
 * <li> <b>Document import button subtitle text:</b> via the string resource named {@code
 * gc_camera_document_import_subtitle}
 *
 * <li> <b>Document import button subtitle text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.DocumentImportSubtitle.TextStyle}
 *
 * <li> <b>Document import button subtitle font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.DocumentImportSubtitle.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Document import hint background:</b> via the color resource named {@code
 * gc_document_import_hint_background}
 *
 * <li> <b>Document import hint close icon color:</b> via the color resource name {@code
 * gc_hint_close}
 *
 * <li> <b>Document import hint text:</b> via the string resource named {@code
 * gc_document_import_hint_text}
 *
 * <li> <b>Document import hint text size:</b>  via overriding the style named {@code
 * GiniCaptureTheme.Camera.DocumentImportHint.TextStyle} and setting an item named {@code
 * android:textSize} with the desired {@code sp} size
 *
 * <li> <b>Document import hint text color:</b> via the color resource name {@code
 * gc_document_import_hint_text}
 *
 * <li> <b>Document import hint font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.DocumentImportHint.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Images stack badge text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.ImageStackBadge.TextStyle}
 *
 * <li> <b>Images stack badge font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.ImageStackBadge.TextStyle} and setting an item named {@code gcCustomFont}
 * with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Images stack badge background colors:</b> via the color resources named {@code
 * gc_camera_image_stack_badge_background} and {@code gc_camera_image_stack_badge_background_border}
 *
 * <li> <b>Images stack badge background size:</b> via the dimension resource named {@code
 * gc_camera_image_stack_badge_size}
 *
 * <li> <b>Images stack subtitle text:</b> via the string resource named {@code
 * gc_camera_image_stack_subtitle}
 *
 * <li> <b>Images stack subtitle text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.ImageStackSubtitle.TextStyle}
 *
 * <li> <b>Images stack subtitle font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.ImageStackSubtitle.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Multi-page document page limit exceeded alert message:</b> via the string resource named {@code
 * gc_document_error_too_many_pages}
 *
 * <li> <b>Multi-page document page limit exceeded alert positive button text:</b> via the string resource named
 * {@code gc_document_error_multi_page_limit_review_pages_button}
 *
 * <li> <b>Multi-page document page limit exceeded alert cancel button text:</b> via the string resource named
 * {@code gc_document_error_multi_page_limit_cancel_button}
 *
 * <li> <b>Read storage permission denied button color:</b> via the color resource named {@code
 * gc_accent}
 *
 * <li> <b>QRCode detected popup background:</b> via the color resource named {@code
 * gc_qrcode_detected_popup_background}
 *
 * <li> <b>QRCode detected popup texts:</b> via the string resources named {@code
 * gc_qrcode_detected_popup_message_1} and {@code gc_qrcode_detected_popup_message_2}
 *
 * <li> <b>QRCode detected popup text sizes:</b>  via overriding the styles named {@code
 * GiniCaptureTheme.Camera.QRCodeDetectedPopup.Message1.TextStyle} and {@code
 * GiniCaptureTheme.Camera.QRCodeDetectedPopup.Message2.TextStyle} and setting an item named {@code
 * android:textSize} with the desired {@code sp} size
 *
 * <li> <b>QRCode detected popup text colors:</b> via the color resource name {@code
 * gc_qrcode_detected_popup_message_1} and {@code gc_qrcode_detected_popup_message_2}
 *
 * <li> <b>QRCode detected popup fonts:</b>  via overriding the styles named {@code
 * GiniCaptureTheme.Camera.QRCodeDetectedPopup.Message1.TextStyle} and {@code
 * GiniCaptureTheme.Camera.QRCodeDetectedPopup.Message2.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Read storage permission rationale text:</b> via the string resource named {@code
 * gc_storage_permission_rationale}
 *
 * <li> <b>Read storage permission rationale positive button text:</b> via the string resource named
 * {@code gc_storage_permission_rationale_positive_button}
 *
 * <li> <b>Read storage permission rationale negative button text:</b> via the string resource named
 * {@code gc_storage_permission_rationale_negative_button}
 *
 * <li> <b>Read storage permission rationale button color:</b> via the color resource named {@code
 * gc_accent}
 *
 * <li> <b>Read storage permission denied text:</b> via the string resource named {@code
 * gc_storage_permission_denied}
 *
 * <li> <b>Read storage permission denied positive button text:</b> via the string resource named
 * {@code gc_storage_permission_denied_positive_button}
 *
 * <li> <b>Read storage permission denied negative button text:</b> via the string resource named
 * {@code gc_storage_permission_denied_negative_button}
 *
 * <li> <b>Read storage permission denied button color:</b> via the color resource named {@code
 * gc_accent}
 *
 * <li> <b>Tap-to-focus indicator:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 * {@code gc_camera_focus_indicator.png}
 *
 * <li> <b>Help menu item icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_help_icon.png}
 *
 * <li> <b>Onboarding menu item title:</b> via the string resource named {@code gc_show_onboarding}
 *
 * <li> <b>Background color:</b> via the color resource named {@code gc_background}. <b>Note:</b>
 * this color resource is global to all Activities ({@link CameraActivity}, {@link
 * OnboardingActivity}, {@link ReviewActivity}, {@link AnalysisActivity})
 *
 * <li> <b>No-camera icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_no_camera.png}
 *
 * <li> <b>No camera permission text:</b> via the string resource named {@code
 * gc_camera_error_no_permission}
 *
 * <li> <b>No camera permission text color:</b> via the color resource named {@code
 * gc_camera_error_no_permission}
 *
 * <li> <b>No camera permission font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>No camera permission text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.TextStyle} and setting an item named {@code
 * android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *
 * <li> <b>No camera permission text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.TextStyle} and setting an item named {@code
 * android:textSize} to the desired {@code sp} size
 *
 * <li> <b>No camera permission button title:</b> via the string resource named {@code
 * gc_camera_error_no_permission_button_title}
 *
 * <li> <b>No camera permission button title color:</b> via the color resources named {@code
 * gc_camera_error_no_permission_button_title} and {@code gc_camera_error_no_permission_button_title_pressed}
 *
 * <li> <b>No camera permission button font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.Button.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>No camera permission button text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.Button.TextStyle} and setting an item named {@code
 * android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *
 * <li> <b>No camera permission button text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Camera.Error.NoPermission.Button.TextStyle} and setting an item named {@code
 * android:textSize} to the desired {@code sp} size
 *
 * </ul>
 *
 * <p> <b>Important:</b> All overriden styles must have their respective {@code Root.} prefixed
 * style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Camera.Error.NoPermission.TextStyle}
 * must be {@code Root.GiniCaptureTheme.Camera.Error.NoPermission.TextStyle}.
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * <p> Customizing the Action Bar is also done via overriding of app resources and each one - except
 * the title string resource - is global to all Activities ({@link CameraActivity}, {@link
 * OnboardingActivity}, {@link ReviewActivity}, {@link MultiPageReviewActivity}, {@link
 * AnalysisActivity}).
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li> <b>Background color:</b> via the color resource named {@code gc_action_bar} (highly
 * recommended for Android 5+: customize the status bar color via {@code gc_status_bar})
 *
 * <li> <b>Title:</b> via the string resource name {@code gc_title_camera}
 *
 * <li> <b>Title color:</b> via the color resource named {@code gc_action_bar_title}
 *
 * <li> <b>Back button:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 * {@code gc_action_bar_back}
 *
 * </ul>
 **/
public class CameraActivity extends AppCompatActivity implements CameraFragmentListener,
        CameraFragmentInterface {

    /**
     * <p> Returned when the result code is {@link CameraActivity#RESULT_ERROR} and contains a
     * {@link GiniCaptureError} object detailing what went wrong. </p>
     */
    public static final String EXTRA_OUT_ERROR = "GC_EXTRA_OUT_ERROR";

    /**
     * Returned when extractions are available. Contains a Bundle with the extraction labels as keys
     * and {@link GiniCaptureSpecificExtraction} as values.
     */
    public static final String EXTRA_OUT_EXTRACTIONS = "GC_EXTRA_OUT_EXTRACTIONS";

    /**
     * Returned when compound extractions are available. Contains a Bundle with the extraction labels as keys and {@link
     * GiniCaptureCompoundExtraction} as values.
     */
    public static final String EXTRA_OUT_COMPOUND_EXTRACTIONS = "GC_EXTRA_OUT_COMPOUND_EXTRACTIONS";

    /**
     * Returned when return reasons are available. Contains a Parcelable ArrayList extra with
     * {@link GiniCaptureReturnReason} as values.
     */
    public static final String EXTRA_OUT_RETURN_REASONS = "GC_EXTRA_OUT_RETURN_REASONS";

    /**
     * <p> Returned result code in case something went wrong. You should retrieve the {@link
     * CameraActivity#EXTRA_OUT_ERROR} extra to find out what went wrong. </p>
     */
    public static final int RESULT_ERROR = RESULT_FIRST_USER + 1;

    @VisibleForTesting
    static final int REVIEW_DOCUMENT_REQUEST = 1;
    private static final int ONBOARDING_REQUEST = 2;
    private static final int ANALYSE_DOCUMENT_REQUEST = 3;
    private static final int MULTI_PAGE_REVIEW_REQUEST = 4;
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private static final String ONBOARDING_SHOWN_KEY = "ONBOARDING_SHOWN_KEY";

    private boolean mOnboardingShown;
    private GiniCaptureCoordinator mGiniCaptureCoordinator;
    private Document mDocument;

    private CameraFragmentCompat mFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_camera);
        createGiniCaptureCoordinator();
        if (savedInstanceState == null) {
            initFragment();
        } else {
            restoreSavedState(savedInstanceState);
            retainFragment();
        }
        showOnboardingIfRequested();
        setupHomeButton();
    }

    private void setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            enableHomeAsUp(this);
        }
    }

    private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mOnboardingShown = savedInstanceState.getBoolean(ONBOARDING_SHOWN_KEY);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ONBOARDING_SHOWN_KEY, mOnboardingShown);
    }

    private void createFragment() {
        mFragment = createCameraFragmentCompat();
    }

    protected CameraFragmentCompat createCameraFragmentCompat() {
        return CameraFragmentCompat.createInstance();
    }

    private void initFragment() {
        if (!isFragmentShown()) {
            createFragment();
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(CAMERA_FRAGMENT) != null;
    }

    private void retainFragment() {
        mFragment = (CameraFragmentCompat) getSupportFragmentManager().findFragmentByTag(
                CAMERA_FRAGMENT);
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_camera, mFragment, CAMERA_FRAGMENT)
                .commit();
    }

    private void showOnboardingIfRequested() {
        if (shouldShowOnboarding()) {
            startOnboardingActivity();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGiniCaptureCoordinator.onCameraStarted();
        if (mOnboardingShown) {
            hideInterface();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMemory();
    }

    private void createGiniCaptureCoordinator() {
        mGiniCaptureCoordinator = GiniCaptureCoordinator.createInstance(this);
        mGiniCaptureCoordinator
                .setShowOnboardingAtFirstRun(shouldShowOnboardingAtFirstRun())
                .setListener(new GiniCaptureCoordinator.Listener() {
                    @Override
                    public void onShowOnboarding() {
                        startOnboardingActivity();
                    }
                });
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.gc_camera, menu);
        return true;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.gc_action_show_onboarding) {
            startHelpActivity();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        trackCameraScreenEvent(CameraScreenEvent.EXIT);
    }

    private void startHelpActivity() {
        final Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
        trackCameraScreenEvent(CameraScreenEvent.HELP);
    }

    @VisibleForTesting
    void startOnboardingActivity() {
        if (mOnboardingShown) {
            return;
        }
        final Intent intent = new Intent(this, OnboardingActivity.class);
        hideInterface();
        startActivityForResult(intent, ONBOARDING_REQUEST);
        mOnboardingShown = true;
    }

    @Override
    public void onDocumentAvailable(@NonNull final Document document) {
        mDocument = document;
        if (mDocument.isReviewable()) {
            startReviewActivity(document);
        } else {
            startAnalysisActivity(document);
        }
    }

    @Override
    public void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        if (multiPageDocument.getType() == Document.Type.IMAGE_MULTI_PAGE) {
            final Intent intent = MultiPageReviewActivity.createIntent(this);
            startActivityForResult(intent, MULTI_PAGE_REVIEW_REQUEST);
        } else {
            throw new UnsupportedOperationException("Unsupported multi-page document type.");
        }
    }

    @Override
    public void onCheckImportedDocument(@NonNull final Document document,
            @NonNull final DocumentCheckResultCallback callback) {
        callback.documentAccepted();
    }

    private void startReviewActivity(@NonNull final Document document) {
        final Intent reviewIntent = new Intent(this, ReviewActivity.class);
        reviewIntent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, document);
        reviewIntent.putExtra(EXTRA_IN_ANALYSIS_ACTIVITY, new Intent(this, AnalysisActivity.class));
        reviewIntent.setExtrasClassLoader(CameraActivity.class.getClassLoader());
        startActivityForResult(reviewIntent, REVIEW_DOCUMENT_REQUEST);
    }

    private void startAnalysisActivity(@NonNull final Document document) {
        final Intent analysisIntent = new Intent(this, AnalysisActivity.class);
        analysisIntent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT, document);
        analysisIntent.setExtrasClassLoader(CameraActivity.class.getClassLoader());
        startActivityForResult(analysisIntent, ANALYSE_DOCUMENT_REQUEST);
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        final Intent result = new Intent();
        result.putExtra(EXTRA_OUT_ERROR, error);
        setResult(RESULT_ERROR, result);
        finish();
    }

    @Override
    public void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {
        final Intent result = new Intent();
        final Bundle extractionsBundle = new Bundle();
        for (final Map.Entry<String, GiniCaptureSpecificExtraction> extraction
                : extractions.entrySet()) {
            extractionsBundle.putParcelable(extraction.getKey(), extraction.getValue());
        }
        result.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, extractionsBundle);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        switch (requestCode) {
            case REVIEW_DOCUMENT_REQUEST:
            case ANALYSE_DOCUMENT_REQUEST:
                if (resultCode != Activity.RESULT_CANCELED
                        && resultCode != AnalysisActivity.RESULT_NO_EXTRACTIONS
                        && resultCode != ReviewActivity.RESULT_NO_EXTRACTIONS) {
                    setResult(resultCode, data);
                    finish();
                    clearMemory();
                }
                break;
            case ONBOARDING_REQUEST:
                mOnboardingShown = false;
                showInterface();
                break;
            case MULTI_PAGE_REVIEW_REQUEST:
                if (resultCode != Activity.RESULT_CANCELED
                        && resultCode != AnalysisActivity.RESULT_NO_EXTRACTIONS) {
                    setResult(resultCode, data);
                    finish();
                    clearMemory();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void setListener(@NonNull final CameraFragmentListener listener) {
        throw new IllegalStateException("CameraFragmentListener must not be altered in the "
                + "CameraActivity. Override listener methods in a CameraActivity subclass "
                + "instead.");
    }

    @Override
    public void showInterface() {
        mFragment.showInterface();
    }

    @Override
    public void hideInterface() {
        mFragment.hideInterface();
    }

    @Override
    public void showActivityIndicatorAndDisableInteraction() {
        mFragment.showActivityIndicatorAndDisableInteraction();
    }

    @Override
    public void hideActivityIndicatorAndEnableInteraction() {
        mFragment.hideActivityIndicatorAndEnableInteraction();
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        mFragment.showError(message, duration);
    }

    private void clearMemory() {
        mDocument = null; // NOPMD
    }
}

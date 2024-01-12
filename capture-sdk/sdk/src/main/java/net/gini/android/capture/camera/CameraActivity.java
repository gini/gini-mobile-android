package net.gini.android.capture.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureCoordinator;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.QRCodeDocument;
import net.gini.android.capture.internal.util.ContextHelper;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.review.multipage.MultiPageReviewActivity;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.CameraScreenEvent;

import java.util.Map;

import static net.gini.android.capture.analysis.AnalysisActivity.RESULT_NO_EXTRACTIONS;
import static net.gini.android.capture.error.ErrorActivity.ERROR_SCREEN_REQUEST;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;
import static net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboarding;
import static net.gini.android.capture.internal.util.FeatureConfiguration.shouldShowOnboardingAtFirstRun;
import static net.gini.android.capture.review.multipage.MultiPageReviewActivity.RESULT_SCROLL_TO_LAST_PAGE;
import static net.gini.android.capture.review.multipage.MultiPageReviewActivity.SHOULD_SCROLL_TO_LAST_PAGE;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackCameraScreenEvent;

/**
 * The {@code CameraActivity} is the main entry point to the Gini Capture SDK.
 **/
public class
CameraActivity extends AppCompatActivity implements CameraFragmentListener,
        CameraFragmentInterface {

    public static final String EXTRA_IN_ADD_PAGES = "GC_EXTRA_IN_ADD_PAGES";

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

    /**
     * <p> Returned result code in case the user wants to enter data manually if
     * the scanning gives no results</p>
     */
    public static final int RESULT_ENTER_MANUALLY = RESULT_FIRST_USER + 99;

    /**
     * <p> Returned result code in case the user wants to go back to camera screen </p>
     */
    public static final int RESULT_CAMERA_SCREEN = RESULT_FIRST_USER + 100;

    @VisibleForTesting
    static final int REVIEW_DOCUMENT_REQUEST = 1;
    private static final int ONBOARDING_REQUEST = 2;
    private static final int ANALYSE_DOCUMENT_REQUEST = 3;
    private static final int MULTI_PAGE_REVIEW_REQUEST = 4;
    private static final String CAMERA_FRAGMENT = "CAMERA_FRAGMENT";
    private static final String ONBOARDING_SHOWN_KEY = "ONBOARDING_SHOWN_KEY";

    private boolean mOnboardingShown;
    private GiniCaptureCoordinator mGiniCaptureCoordinator;
    private boolean mAddPages;

    private CameraFragment mFragment;

    /**
     * Internal use only.
     *
     * @suppress
     */
    private static final int NO_RESULT_REQUEST = 999;

    /**
     * Internal use only.
     *
     * @param context android context
     * @param addPages pass `true` when launching to add more pages. If there are no pages, then pass `false`.
     * @return the intent to launch the {@link CameraActivity}
     */
    public static Intent createIntent(@NonNull final Context context, boolean addPages) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra(EXTRA_IN_ADD_PAGES, addPages);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_camera);
        readExtras();
        createGiniCaptureCoordinator();
        if (savedInstanceState == null) {
            initFragment();
        } else {
            restoreSavedState(savedInstanceState);
            retainFragment();
        }
        showOnboardingIfRequested();
        handleOnBackPressed();
        setTitleOnTablets();
    }

    private void readExtras() {
        mAddPages = getIntent().getBooleanExtra(EXTRA_IN_ADD_PAGES, false);
    }

    private void handleOnBackPressed() {
        interceptOnBackPressed(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                trackCameraScreenEvent(CameraScreenEvent.EXIT);
            }
        });
    }

    private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mOnboardingShown = savedInstanceState.getBoolean(ONBOARDING_SHOWN_KEY);
    }

    private void setTitleOnTablets() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(ContextHelper.isTablet(this) ? getString(R.string.gc_camera_info_label_invoice_and_qr) : getString(R.string.gc_title_camera));
        }
    }


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ONBOARDING_SHOWN_KEY, mOnboardingShown);
    }

    private void createFragment() {
        mFragment = createCameraFragment();
    }

    protected CameraFragment createCameraFragment() {
        return CameraFragment.createInstance();
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
        mFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag(
                CAMERA_FRAGMENT);
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_camera, mFragment, CAMERA_FRAGMENT)
                .commit();
    }

    private void showOnboardingIfRequested() {
        if (shouldShowOnboarding() && !mAddPages) {
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    public void onProceedToAnalysisScreen(@NonNull final Document document) {
        startAnalysisActivity(document);
    }

    @Override
    public void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument, boolean shouldScrollToLastPage) {
        if (multiPageDocument.getType() == Document.Type.IMAGE_MULTI_PAGE) {
            if (mAddPages) {

                // In case we returned to take more images
                // Let the app know if it should scroll to the last position
                Intent intent = new Intent(this, MultiPageReviewActivity.class);
                intent.putExtra(SHOULD_SCROLL_TO_LAST_PAGE, shouldScrollToLastPage);
                setResult(RESULT_SCROLL_TO_LAST_PAGE, intent);

                // For subsequent images a new CameraActivity was launched from the MultiPageReviewActivity
                // and so we can simply finish to return to the review activity
                finish();
            } else {
                // For the first image navigate to the review activity and when it returns a result
                // we will return it directly to the client
                final Intent intent = MultiPageReviewActivity.createIntent(this, shouldScrollToLastPage);
                startActivityForResult(intent, MULTI_PAGE_REVIEW_REQUEST);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported multi-page document type.");
        }
    }

    @Override
    public void onCheckImportedDocument(@NonNull final Document document,
            @NonNull final DocumentCheckResultCallback callback) {
        callback.documentAccepted();
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
    public void noExtractionsFromQRCode(QRCodeDocument qrCodeDocument) {
        trackAnalysisScreenEvent(AnalysisScreenEvent.NO_RESULTS);
        final Intent noResultsActivity = new Intent(this, NoResultsActivity.class);
        noResultsActivity.putExtra(NoResultsActivity.EXTRA_IN_DOCUMENT, qrCodeDocument);
        noResultsActivity.setExtrasClassLoader(CameraActivity.class.getClassLoader());
        startActivityForResult(noResultsActivity, NO_RESULT_REQUEST);
        setResult(RESULT_NO_EXTRACTIONS);
        if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {

        switch (requestCode) {
            case REVIEW_DOCUMENT_REQUEST:
            case ANALYSE_DOCUMENT_REQUEST:
            case MULTI_PAGE_REVIEW_REQUEST:
            case ERROR_SCREEN_REQUEST:
            case NO_RESULT_REQUEST:
                // The first CameraActivity instance is invisible to the user
                // after we navigate to the review or analysis activity.
                // Once we get a result it means we are back at the first CameraActivity instance
                // so we need to return the result to the client if result is not retake images
                // from No Results or Error screens

                if (resultCode == RESULT_CAMERA_SCREEN) {
                    // Clear the image from the memory store because the user will take new pictures
                    if (GiniCapture.hasInstance()) {
                        GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
                    }
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                }

                setResult(resultCode, data);
                finish();
                break;
            case ONBOARDING_REQUEST:
                mOnboardingShown = false;
                showInterface();
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

}

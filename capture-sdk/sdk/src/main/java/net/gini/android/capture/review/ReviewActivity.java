package net.gini.android.capture.review;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

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
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.network.GiniCaptureNetworkApi;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.tracking.ReviewScreenEvent;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

/**
 * <h3>Screen API</h3>
 *
 * When you use the Screen API, the {@code ReviewActivity} displays the photographed or imported
 * image and allows the user to review it by checking the sharpness, quality and orientation of
 * the image. The user can correct the orientation by rotating the image.
 *
 * <p> The preferred way of adding network calls to the Gini Capture SDK is by creating a
 * {@link GiniCapture} instance with a {@link GiniCaptureNetworkService} and a
 * {@link GiniCaptureNetworkApi} implementation.
 *
 * <p> The {@code ReviewActivity} is started by the {@link CameraActivity} after the user has taken
 * a photo or imported an image of a document.
 *
 * <h3>Customizing the Review Screen</h3>
 *
 * Customizing the look of the Review Screen is done via overriding of app resources.
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li><b>Rotate button icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_review_button_rotate.png}
 *
 * <li><b>Rotate button color:</b>  via the color resources named {@code gc_review_fab_mini} and
 * {@code gc_review_fab_mini_pressed}
 *
 * <li><b>Next button icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_review_fab_next.png}
 *
 * <li><b>Next button color:</b> via the color resources named {@code gc_review_fab} and {@code
 * gc_review_fab_pressed}
 *
 * <li><b>Bottom advice text:</b> via the string resource named {@code gc_review_bottom_panel_text}
 *
 * <li><b>Bottom text color:</b> via the color resource named {@code gc_review_bottom_panel_text}
 *
 * <li><b>Bottom text font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.BottomPanel.TextStyle} and setting an item named {@code gcCustomFont} with
 * the path to the font file in your {@code assets} folder
 *
 * <li><b>Bottom text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.BottomPanel.TextStyle} and setting an item named {@code android:textStyle}
 * to {@code normal}, {@code bold} or {@code italic}
 *
 * <li><b>Bottom text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.BottomPanel.TextStyle} and setting an item named {@code android:textSize}
 * to the desired {@code sp} size
 *
 * <li><b>Bottom panel background color:</b> via the color resource named {@code
 * gc_review_bottom_panel_background}
 *
 * <li><b>Background color:</b> via the color resource named {@code gc_background}. <b>Note:</b>
 * this color resource is global to all Activities ({@link CameraActivity}, {@link
 * OnboardingActivity}, {@link ReviewActivity}, {@link AnalysisActivity})
 *
 * </ul>
 *
 * <p> <b>Important:</b> All overridden styles must have their respective {@code Root.} prefixed
 * style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Review.BottomPanel.TextStyle}
 * must be {@code Root.GiniCaptureTheme.Review.BottomPanel.TextStyle}.
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * Customizing the Action Bar is also done via overriding of app resources and each one - except the
 * title string resource - is global to all Activities ({@link CameraActivity}, {@link
 * OnboardingActivity}, {@link ReviewActivity}, {@link AnalysisActivity}).
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li><b>Background color:</b> via the color resource named {@code gc_action_bar} (highly
 * recommended for Android 5+: customize the status bar color via {@code gc_status_bar})
 *
 * <li><b>Title:</b> via the string resource you set in your {@code AndroidManifest.xml} when
 * declaring your Activity that extends {@link ReviewActivity}. The default title string resource is
 * named {@code gc_title_review}
 *
 * <li><b>Title color:</b> via the color resource named {@code gc_action_bar_title}
 *
 * <li><b>Back button:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 * {@code gc_action_bar_back}
 *
 * </ul>
 */
public class ReviewActivity extends AppCompatActivity implements ReviewFragmentListener,
        ReviewFragmentInterface {

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final String EXTRA_IN_DOCUMENT = "GC_EXTRA_IN_DOCUMENT";
    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final String EXTRA_IN_ANALYSIS_ACTIVITY = "GC_EXTRA_IN_ANALYSIS_ACTIVITY";
    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final String EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY =
            "GC_EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY";
    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final String EXTRA_OUT_DOCUMENT = "GC_EXTRA_OUT_DOCUMENT";
    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final String EXTRA_OUT_ERROR = "GC_EXTRA_OUT_ERROR";

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final int RESULT_ERROR = RESULT_FIRST_USER + 1;

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final int RESULT_NO_EXTRACTIONS = RESULT_FIRST_USER + 2;

    @VisibleForTesting
    static final int ANALYSE_DOCUMENT_REQUEST = 1;


    private static final String NO_EXTRACTIONS_FOUND_KEY = "NO_EXTRACTIONS_FOUND_KEY";
    private static final String REVIEW_FRAGMENT = "REVIEW_FRAGMENT";

    private ReviewFragmentCompat mFragment;
    private Document mDocument;
    private boolean mBackButtonShouldCloseLibrary;

    private Intent mAnalyzeDocumentActivityIntent;
    private boolean mNoExtractionsFound;

    @VisibleForTesting
    ReviewFragmentCompat getFragment() {
        return mFragment;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_review);
        readExtras();
        if (savedInstanceState == null) {
            initFragment();
        } else {
            restoreSavedState(savedInstanceState);
            retainFragment();
        }
        enableHomeAsUp(this);
    }

    private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mNoExtractionsFound = savedInstanceState.getBoolean(NO_EXTRACTIONS_FOUND_KEY);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        trackReviewScreenEvent(ReviewScreenEvent.BACK);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(NO_EXTRACTIONS_FOUND_KEY, mNoExtractionsFound);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMemory();
    }

    private void clearMemory() {
        mDocument = null;  // NOPMD
    }

    @VisibleForTesting
    void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDocument = extras.getParcelable(EXTRA_IN_DOCUMENT);
            mAnalyzeDocumentActivityIntent = extras.getParcelable(EXTRA_IN_ANALYSIS_ACTIVITY);
            mBackButtonShouldCloseLibrary = extras.getBoolean(
                    EXTRA_IN_BACK_BUTTON_SHOULD_CLOSE_LIBRARY, false);
        }
        checkRequiredExtras();
    }

    private void checkRequiredExtras() {
        if (mDocument == null) {
            throw new IllegalStateException(
                    "ReviewActivity requires a Document. Set it as an extra using the EXTRA_IN_DOCUMENT key.");
        }
        if (mAnalyzeDocumentActivityIntent == null) {
            throw new IllegalStateException(
                    "ReviewActivity requires an AnalyzeDocumentActivity class. Call setAnalyzeDocumentActivityExtra() to set it.");
        }
    }

    private void initFragment() {
        if (!isFragmentShown()) {
            createFragment();
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(REVIEW_FRAGMENT) != null;
    }

    private void createFragment() {
        mFragment = ReviewFragmentCompat.createInstance(mDocument);
    }

    private void retainFragment() {
        mFragment = (ReviewFragmentCompat) getSupportFragmentManager().findFragmentByTag(
                REVIEW_FRAGMENT);
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_review_document, mFragment, REVIEW_FRAGMENT)
                .commit();
    }

    @Override
    public void onProceedToAnalysisScreen(@NonNull final Document document,
            @Nullable final String errorMessage) {
        if (mNoExtractionsFound) {
            if (GiniCaptureCoordinator.shouldShowGiniCaptureNoResultsScreen(document)) {
                final Intent noResultsActivity = new Intent(this, NoResultsActivity.class);
                noResultsActivity.putExtra(NoResultsActivity.EXTRA_IN_DOCUMENT, mDocument);
                noResultsActivity.setExtrasClassLoader(ReviewActivity.class.getClassLoader());
                startActivity(noResultsActivity);
                setResult(RESULT_NO_EXTRACTIONS);
            } else {
                final Intent result = new Intent();
                setResult(RESULT_OK, result);
            }
            finish();
        } else {
            mAnalyzeDocumentActivityIntent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT, document);
            if (errorMessage != null) {
                mAnalyzeDocumentActivityIntent.putExtra(
                        AnalysisActivity.EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE,
                        errorMessage);
            }
            mAnalyzeDocumentActivityIntent.setExtrasClassLoader(
                    ReviewActivity.class.getClassLoader());
            startActivityForResult(mAnalyzeDocumentActivityIntent, ANALYSE_DOCUMENT_REQUEST);
        }
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        final Intent result = new Intent();
        result.putExtra(EXTRA_OUT_ERROR, error);
        setResult(RESULT_ERROR, result);
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ANALYSE_DOCUMENT_REQUEST) {
            if (resultCode == RESULT_NO_EXTRACTIONS) {
                finish();
                clearMemory();
            } else if (mBackButtonShouldCloseLibrary
                    || resultCode != Activity.RESULT_CANCELED) {
                setResult(resultCode, data);
                finish();
                clearMemory();
            }
        }
    }

    @Override
    public void setListener(@NonNull final ReviewFragmentListener listener) {
        throw new IllegalStateException("ReviewFragmentListener must not be altered in the "
                + "ReviewActivity. Override listener methods in a ReviewActivity subclass "
                + "instead.");
    }
}

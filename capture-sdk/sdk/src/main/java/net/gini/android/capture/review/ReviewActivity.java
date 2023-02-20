package net.gini.android.capture.review;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.tracking.ReviewScreenEvent;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;
import static net.gini.android.capture.noresults.NoResultsActivity.NO_RESULT_CANCEL_KEY;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

/**
 * Internal use only.
 *
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
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        interceptOnBackPressed(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                trackReviewScreenEvent(ReviewScreenEvent.BACK);
            }
        });
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
                    || resultCode != Activity.RESULT_CANCELED || (data != null && data.hasExtra(NO_RESULT_CANCEL_KEY))) {
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

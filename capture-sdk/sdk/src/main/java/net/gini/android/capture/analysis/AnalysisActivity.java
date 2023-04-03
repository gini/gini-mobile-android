package net.gini.android.capture.analysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureCoordinator;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.tracking.AnalysisScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import static net.gini.android.capture.camera.CameraActivity.RESULT_CAMERA_SCREEN;
import static net.gini.android.capture.camera.CameraActivity.RESULT_ENTER_MANUALLY;
import static net.gini.android.capture.error.ErrorActivity.ERROR_SCREEN_REQUEST;
import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;
import static net.gini.android.capture.noresults.NoResultsActivity.NO_RESULT_CANCEL_KEY;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

/**
 * Internal use only.
 */
public class AnalysisActivity extends AppCompatActivity implements
        AnalysisFragmentListener, AnalysisFragmentInterface {

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
    public static final String EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE =
            "GC_EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE";
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

    /**
     * Internal use only.
     *
     * @suppress
     */
    private static final int NO_RESULT_REQUEST = 999;

    /**
     * Internal use only.
     *
     * @suppress
     */
    private static final String ANALYSIS_FRAGMENT = "ANALYSIS_FRAGMENT";

    private String mAnalysisErrorMessage;
    private Document mDocument;
    private AnalysisFragmentCompat mFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_analysis);
        setTitle("");
        readExtras();
        if (savedInstanceState == null) {
            initFragment();
        } else {
            retainFragment();
        }
        enableHomeAsUp(this);
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        interceptOnBackPressed(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                trackAnalysisScreenEvent(AnalysisScreenEvent.CANCEL);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMemory();
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        final Intent result = new Intent();
        result.putExtra(EXTRA_OUT_ERROR, error);
        setResult(RESULT_ERROR, result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @VisibleForTesting
    AnalysisFragmentCompat getFragment() {
        return mFragment;
    }

    private void checkRequiredExtras() {
        if (mDocument == null) {
            throw new IllegalStateException(
                    "AnalysisActivity requires a Document. Set it as an extra using the "
                            + "EXTRA_IN_DOCUMENT key.");
        }
    }

    private void clearMemory() {
        mDocument = null; // NOPMD
    }

    private void createFragment() {
        mFragment = AnalysisFragmentCompat.createInstance(mDocument, mAnalysisErrorMessage);
    }

    private void initFragment() {
        if (!isFragmentShown()) {
            createFragment();
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(ANALYSIS_FRAGMENT) != null;
    }

    private void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDocument = extras.getParcelable(EXTRA_IN_DOCUMENT);
            mAnalysisErrorMessage = extras.getString(EXTRA_IN_DOCUMENT_ANALYSIS_ERROR_MESSAGE);
        }
        checkRequiredExtras();
    }

    private void retainFragment() {
        mFragment = (AnalysisFragmentCompat) getSupportFragmentManager().findFragmentByTag(
                ANALYSIS_FRAGMENT);
    }

    private void showFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.gc_fragment_analyze_document,
                mFragment, ANALYSIS_FRAGMENT).commit();
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        throw new IllegalStateException("AnalysisFragmentListener must not be altered in the "
                + "AnalysisActivity. Override listener methods in an AnalysisActivity subclass "
                + "instead.");
    }

    @Override
    public void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
            @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
            @NonNull final List<GiniCaptureReturnReason> returnReasons) {
        final Intent result = new Intent();

        final Bundle extractionsBundle = new Bundle();
        for (final Map.Entry<String, GiniCaptureSpecificExtraction> extraction
                : extractions.entrySet()) {
            extractionsBundle.putParcelable(extraction.getKey(), extraction.getValue());
        }
        result.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, extractionsBundle);

        final Bundle compoundExtractionsBundle = new Bundle();
        for (final Map.Entry<String, GiniCaptureCompoundExtraction> extraction
                : compoundExtractions.entrySet()) {
            compoundExtractionsBundle.putParcelable(extraction.getKey(), extraction.getValue());
        }
        result.putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, compoundExtractionsBundle);

        ArrayList<GiniCaptureReturnReason> returnReasonsExtra = new ArrayList<>(returnReasons);
        result.putParcelableArrayListExtra(CameraActivity.EXTRA_OUT_RETURN_REASONS, returnReasonsExtra);

        setResult(RESULT_OK, result);
        finish();
        clearMemory();
    }

    @Override
    public void onProceedToNoExtractionsScreen(@NonNull final Document document) {
        if (GiniCaptureCoordinator.shouldShowGiniCaptureNoResultsScreen(mDocument)) {
            final Intent noResultsActivity = new Intent(this, NoResultsActivity.class);
            noResultsActivity.putExtra(NoResultsActivity.EXTRA_IN_DOCUMENT, mDocument);
            noResultsActivity.setExtrasClassLoader(AnalysisActivity.class.getClassLoader());
            startActivityForResult(noResultsActivity, NO_RESULT_REQUEST);
            setResult(RESULT_NO_EXTRACTIONS);
            if (GiniCapture.hasInstance()) {
                GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
            }
        } else {
            final Intent result = new Intent();
            setResult(RESULT_OK, result);
            finish();
        }
    }

    @Override
    public void onDefaultPDFAppAlertDialogCancelled() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if ((requestCode == NO_RESULT_REQUEST || requestCode == ERROR_SCREEN_REQUEST) &&
                ((resultCode == RESULT_CANCELED && data != null && data.hasExtra(NO_RESULT_CANCEL_KEY)) || resultCode == RESULT_ENTER_MANUALLY || resultCode == RESULT_CAMERA_SCREEN)) {
            if (resultCode == RESULT_CAMERA_SCREEN) {
                if (GiniCapture.hasInstance()) {
                    GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
                }
            }
            setResult(resultCode, data);
        }

        finish();
    }
}

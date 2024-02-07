package net.gini.android.capture.camera;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import net.gini.android.capture.CaptureSDKResult;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.GiniCaptureFragment;
import net.gini.android.capture.GiniCaptureFragmentListener;
import net.gini.android.capture.R;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.ArrayList;
import java.util.Map;

/**
 * The {@code CameraActivity} is the main entry point to the Gini Capture SDK.
 **/
public class CameraActivity extends AppCompatActivity implements GiniCaptureFragmentListener {

    /**
     * Internal use only.
     */
    public static final String EXTRA_IN_OPEN_WITH_DOCUMENT = "GC_EXTRA_IN_OPEN_WITH_DOCUMENT";

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

    private GiniCaptureFragment mFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_camera);
        final Document openWithDocument = IntentCompat.getParcelableExtra(getIntent(), EXTRA_IN_OPEN_WITH_DOCUMENT, Document.class);
        if (savedInstanceState == null) {
            initFragment(openWithDocument);
        } else {
            retainFragment();
        }
    }

    private void createFragment(@Nullable final Document openWithDocument) {
        if (GiniCapture.hasInstance()) {
            if (openWithDocument != null) {
                mFragment = GiniCapture.Internal.createGiniCaptureFragmentForOpenWithDocument(openWithDocument);
            } else {
                mFragment = GiniCapture.createGiniCaptureFragment();
            }
            mFragment.setListener(this);
        }
    }

    private void initFragment(@Nullable final Document document) {
        if (!isFragmentShown()) {
            createFragment(document);
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(GiniCaptureFragment.class.getName()) != null;
    }

    private void retainFragment() {
        mFragment = (GiniCaptureFragment) getSupportFragmentManager().findFragmentByTag(GiniCaptureFragment.class.getName());
        if (mFragment != null) {
            mFragment.setListener(this);
        }
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_camera, mFragment, GiniCaptureFragment.class.getName())
                .commit();
    }

    @Override
    public void onFinishedWithResult(@NonNull CaptureSDKResult result) {
        if (result instanceof CaptureSDKResult.Success) {
            final CaptureSDKResult.Success successResult = (CaptureSDKResult.Success) result;
            final Intent resultIntent = new Intent();

            final Bundle extractionsBundle = new Bundle();
            for (final Map.Entry<String, GiniCaptureSpecificExtraction> extraction
                    : successResult.getSpecificExtractions().entrySet()) {
                extractionsBundle.putParcelable(extraction.getKey(), extraction.getValue());
            }
            resultIntent.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, extractionsBundle);

            final Bundle compoundExtractionsBundle = new Bundle();
            for (final Map.Entry<String, GiniCaptureCompoundExtraction> extraction
                    : successResult.getCompoundExtractions().entrySet()) {
                compoundExtractionsBundle.putParcelable(extraction.getKey(), extraction.getValue());
            }
            resultIntent.putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, compoundExtractionsBundle);

            ArrayList<GiniCaptureReturnReason> returnReasonsExtra = new ArrayList<>(successResult.getReturnReasons());
            resultIntent.putParcelableArrayListExtra(CameraActivity.EXTRA_OUT_RETURN_REASONS, returnReasonsExtra);

            setResult(RESULT_OK, resultIntent);
            finish();
        } else if (result instanceof CaptureSDKResult.Empty) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(CameraActivity.EXTRA_OUT_EXTRACTIONS, new Bundle());
            resultIntent.putExtra(CameraActivity.EXTRA_OUT_COMPOUND_EXTRACTIONS, new Bundle());
            ArrayList<GiniCaptureReturnReason> returnReasonsExtra = new ArrayList<>();
            resultIntent.putParcelableArrayListExtra(CameraActivity.EXTRA_OUT_RETURN_REASONS, returnReasonsExtra);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else if (result instanceof CaptureSDKResult.Cancel) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (result instanceof CaptureSDKResult.EnterManually) {
            setResult(RESULT_ENTER_MANUALLY);
            finish();
        } else if (result instanceof CaptureSDKResult.Error) {
            final CaptureSDKResult.Error errorResult = (CaptureSDKResult.Error) result;
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(CameraActivity.EXTRA_OUT_ERROR, errorResult.getValue());
            setResult(RESULT_ERROR, resultIntent);
            finish();
        }
    }

    @Override
    public void onCheckImportedDocument(@NonNull Document document, @NonNull CameraFragmentListener.DocumentCheckResultCallback callback) {
        callback.documentAccepted();
    }
}

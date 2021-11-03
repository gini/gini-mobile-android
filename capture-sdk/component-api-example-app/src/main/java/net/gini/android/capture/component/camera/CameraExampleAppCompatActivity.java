package net.gini.android.capture.component.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.camera.CameraFragmentCompat;
import net.gini.android.capture.camera.CameraFragmentListener;
import net.gini.android.capture.component.CaptureComponentContract;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity;
import net.gini.android.capture.component.review.ReviewExampleAppCompatActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.help.HelpActivity;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.onboarding.OnboardingFragmentCompat;
import net.gini.android.capture.onboarding.OnboardingFragmentListener;

import java.util.Map;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * AppCompatActivity using the {@link CameraScreenHandler} to host the
 * {@link CameraFragmentCompat} and the {@link OnboardingFragmentCompat} and to start the
 * {@link ReviewExampleAppCompatActivity}, the {@link AnalysisExampleAppCompatActivity} or the {@link HelpActivity}.
 */
public class CameraExampleAppCompatActivity extends AppCompatActivity implements
        CameraFragmentListener,
        OnboardingFragmentListener {

    public static Intent newInstance(final Context context) {
        return new Intent(context, CameraExampleAppCompatActivity.class);
    }

    private CameraScreenHandler mCameraScreenHandler;

    private final ActivityResultCallback<CaptureComponentContract.Result> activityResultCallback =
            new ActivityResultCallback<CaptureComponentContract.Result>() {
                @Override
                public void onActivityResult(CaptureComponentContract.Result result) {
                    if (result.getError() != null) {
                        final Intent data = new Intent();
                        data.putExtra(MainActivity.EXTRA_OUT_ERROR, result.getError());
                        CameraExampleAppCompatActivity.this.setResult(MainActivity.RESULT_ERROR,
                                data);
                        CameraExampleAppCompatActivity.this.finish();
                    } else if (result.getResultCode() == RESULT_OK) {
                        CameraExampleAppCompatActivity.this.setResult(RESULT_OK);
                        CameraExampleAppCompatActivity.this.finish();
                    }
                }
            };

    private final ActivityResultLauncher<Intent> mStartReview = registerForActivityResult(
            new CaptureComponentContract(), activityResultCallback);
    private final ActivityResultLauncher<Intent> mStartAnalysis = registerForActivityResult(
            new CaptureComponentContract(), activityResultCallback);
    private final ActivityResultLauncher<Intent> mStartMultiPageReview = registerForActivityResult(
            new CaptureComponentContract(), activityResultCallback);

    @Override
    public void onBackPressed() {
        if (mCameraScreenHandler.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        mCameraScreenHandler.onNewIntent(intent);
    }

    @Override
    public void onCloseOnboarding() {
        mCameraScreenHandler.onCloseOnboarding();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_compat);
        mCameraScreenHandler = new CameraScreenHandler(this, mStartReview, mStartMultiPageReview, mStartAnalysis);
        mCameraScreenHandler.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return mCameraScreenHandler.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mCameraScreenHandler.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDocumentAvailable(@NonNull final Document document) {
        mCameraScreenHandler.onDocumentAvailable(document);
    }

    @Override
    public void onProceedToMultiPageReviewScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        mCameraScreenHandler.onProceedToMultiPageReviewScreen(multiPageDocument);
    }

    @Override
    public void onCheckImportedDocument(@NonNull final Document document,
            @NonNull final DocumentCheckResultCallback callback) {
        mCameraScreenHandler.onCheckImportedDocument(document, callback);
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        mCameraScreenHandler.onError(error);
    }

    @Override
    public void onExtractionsAvailable(
            @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions) {
        mCameraScreenHandler.onExtractionsAvailable(extractions);
    }
}

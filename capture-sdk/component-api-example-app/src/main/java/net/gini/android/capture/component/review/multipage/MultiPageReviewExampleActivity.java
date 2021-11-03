package net.gini.android.capture.component.review.multipage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.component.CaptureComponentContract;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity;
import net.gini.android.capture.component.camera.CameraScreenHandler;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.review.multipage.MultiPageReviewFragment;
import net.gini.android.capture.review.multipage.MultiPageReviewFragmentListener;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Alpar Szotyori on 08.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class MultiPageReviewExampleActivity extends AppCompatActivity implements
        MultiPageReviewFragmentListener {

    private static final Logger LOG = LoggerFactory.getLogger(MultiPageReviewExampleActivity.class);

    private static final int ANALYSIS_REQUEST = 1;
    private MultiPageReviewFragment mMultiPageReviewFragment;

    public static Intent newInstance(final Context context) {
        return new Intent(context, MultiPageReviewExampleActivity.class);
    }

    private final ActivityResultCallback<CaptureComponentContract.Result> activityResultCallback =
            new ActivityResultCallback<CaptureComponentContract.Result>() {
                @Override
                public void onActivityResult(CaptureComponentContract.Result result) {
                    if (result.getError() != null) {
                        final Intent data = new Intent();
                        data.putExtra(MainActivity.EXTRA_OUT_ERROR, result.getError());
                        MultiPageReviewExampleActivity.this.setResult(MainActivity.RESULT_ERROR,
                                data);
                        MultiPageReviewExampleActivity.this.finish();
                    } else if (result.getResultCode() == RESULT_OK) {
                        MultiPageReviewExampleActivity.this.setResult(RESULT_OK);
                        MultiPageReviewExampleActivity.this.finish();
                    }
                }
            };

    private final ActivityResultLauncher<Intent> mStartAnalysis = registerForActivityResult(
            new CaptureComponentContract(), activityResultCallback);

    @Override
    public void onProceedToAnalysisScreen(@NonNull final GiniCaptureMultiPageDocument document) {
        mStartAnalysis.launch(AnalysisExampleAppCompatActivity.newInstance(document, null,
                this));
    }

    @Override
    public void onReturnToCameraScreen() {
        finish();
    }

    @Override
    public void onImportedDocumentReviewCancelled() {
        finish();
    }

    @Override
    public void onError(@NonNull @NotNull GiniCaptureError error) {
        LOG.error("Gini Capture SDK error: {} - {}", error.getErrorCode(), error.getMessage());
        final Intent result = new Intent();
        result.putExtra(MainActivity.EXTRA_OUT_ERROR, error);
        setResult(MainActivity.RESULT_ERROR, result);
        finish();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_page_review);
        setUpActionBar();
        setTitles();

        if (savedInstanceState == null) {
            createMultiPageReviewFragment();
            showMultiPageReviewFragment();
        } else {
            retrieveMultiPageReviewFragment();
        }
    }

    private void setUpActionBar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    private void setTitles() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(R.string.multi_page_review_screen_title);
        actionBar.setSubtitle(getString(R.string.multi_page_review_screen_subtitle));
    }

    private void createMultiPageReviewFragment() {
        mMultiPageReviewFragment = MultiPageReviewFragment.createInstance();
    }

    private void showMultiPageReviewFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.multi_page_review_screen_container, mMultiPageReviewFragment)
                .commit();
    }

    private void retrieveMultiPageReviewFragment() {
        mMultiPageReviewFragment =
                (MultiPageReviewFragment) getSupportFragmentManager().findFragmentById(
                        R.id.multi_page_review_screen_container);
    }
}

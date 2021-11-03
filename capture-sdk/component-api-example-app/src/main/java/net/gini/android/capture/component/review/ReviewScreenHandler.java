package net.gini.android.capture.component.review;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity;
import net.gini.android.capture.component.camera.CameraScreenHandler;
import net.gini.android.capture.review.ReviewFragmentCompat;
import net.gini.android.capture.review.ReviewFragmentInterface;
import net.gini.android.capture.review.ReviewFragmentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static android.app.Activity.RESULT_OK;
import static net.gini.android.capture.component.review.ReviewExampleAppCompatActivity.EXTRA_IN_DOCUMENT;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * Contains the logic for the Review Screen.
 */
public class ReviewScreenHandler implements ReviewFragmentListener {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewScreenHandler.class);
    private final AppCompatActivity mActivity;
    private Document mDocument;
    private ReviewFragmentInterface mReviewFragmentInterface;
    private final ActivityResultLauncher<Intent> startAnalysis;

    protected ReviewScreenHandler(final AppCompatActivity activity, ActivityResultLauncher<Intent> startAnalysis) {
        mActivity = activity;
        this.startAnalysis = startAnalysis;
    }

    @Override
    public void onProceedToAnalysisScreen(@NonNull final Document document,
            @Nullable final String errorMessage) {
        startAnalysis.launch(getAnalysisActivityIntent(document, errorMessage));
    }

    private Intent getAnalysisActivityIntent(final Document document, final String errorMessage) {
        return AnalysisExampleAppCompatActivity.newInstance(document, errorMessage, mActivity);
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        LOG.error("Gini Capture SDK error: {} - {}", error.getErrorCode(), error.getMessage());
        final Intent result = new Intent();
        result.putExtra(MainActivity.EXTRA_OUT_ERROR, error);
        mActivity.setResult(MainActivity.RESULT_ERROR, result);
        mActivity.finish();
    }

    public Document getDocument() {
        return mDocument;
    }

    public void onCreate(final Bundle savedInstanceState) {
        setUpActionBar();
        setTitles();
        readDocumentFromExtras();

        if (savedInstanceState == null) {
            createReviewFragment();
            showReviewFragment();
        } else {
            retrieveReviewFragment();
        }
    }

    private void readDocumentFromExtras() {
        mDocument = mActivity.getIntent().getParcelableExtra(EXTRA_IN_DOCUMENT);
    }

    private ReviewFragmentInterface createReviewFragment() {
        mReviewFragmentInterface = ReviewFragmentCompat.createInstance(getDocument());
        return mReviewFragmentInterface;
    }

    private void showReviewFragment() {
        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.review_screen_container, (Fragment) mReviewFragmentInterface)
                .commit();
    }

    private ReviewFragmentInterface retrieveReviewFragment() {
        mReviewFragmentInterface =
                (ReviewFragmentCompat) mActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.review_screen_container);
        return mReviewFragmentInterface;
    }

    private void setTitles() {
        final ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(R.string.review_screen_title);
        actionBar.setSubtitle(mActivity.getString(R.string.review_screen_subtitle));
    }

    private void setUpActionBar() {
        mActivity.setSupportActionBar(
                (Toolbar) mActivity.findViewById(R.id.toolbar));
    }
}

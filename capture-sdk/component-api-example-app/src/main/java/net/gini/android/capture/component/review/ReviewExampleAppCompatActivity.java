package net.gini.android.capture.component.review;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.component.CaptureComponentContract;
import net.gini.android.capture.component.ExtractionsActivity;
import net.gini.android.capture.component.MainActivity;
import net.gini.android.capture.component.R;
import net.gini.android.capture.component.analysis.AnalysisExampleAppCompatActivity;
import net.gini.android.capture.component.camera.CameraExampleAppCompatActivity;
import net.gini.android.capture.component.noresults.NoResultsExampleAppCompatActivity;
import net.gini.android.capture.review.ReviewFragmentCompat;
import net.gini.android.capture.review.ReviewFragmentListener;

/**
 * Created by Alpar Szotyori on 04.12.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

/**
 * AppCompatActivity using the {@link ReviewScreenHandler} to host the
 * {@link ReviewFragmentCompat} and to start the {@link AnalysisExampleAppCompatActivity}, the
 * {@link NoResultsExampleAppCompatActivity} or the {@link ExtractionsActivity}.
 */
public class ReviewExampleAppCompatActivity extends AppCompatActivity implements
        ReviewFragmentListener {

    public static final String EXTRA_IN_DOCUMENT = "EXTRA_IN_DOCUMENT";

    private final ActivityResultCallback<CaptureComponentContract.Result> activityResultCallback =
            new ActivityResultCallback<CaptureComponentContract.Result>() {
                @Override
                public void onActivityResult(CaptureComponentContract.Result result) {
                    if (result.getError() != null) {
                        Toast.makeText(ReviewExampleAppCompatActivity.this, "Error: "
                                        + result.getError().getErrorCode() + " - "
                                        + result.getError().getMessage(),
                                Toast.LENGTH_LONG).show();
                        ReviewExampleAppCompatActivity.this.finish();
                    } else if (result.getResultCode() == RESULT_OK) {
                        ReviewExampleAppCompatActivity.this.setResult(RESULT_OK);
                        ReviewExampleAppCompatActivity.this.finish();
                    }

                }
            };

    private final ActivityResultLauncher<Intent> mStartAnalysis = registerForActivityResult(
            new CaptureComponentContract(), activityResultCallback);

    private ReviewScreenHandler mReviewScreenHandler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_compat);
        mReviewScreenHandler = new ReviewScreenHandler(this, mStartAnalysis);
        mReviewScreenHandler.onCreate(savedInstanceState);
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {
        mReviewScreenHandler.onError(error);
    }

    @Override
    public void onProceedToAnalysisScreen(@NonNull final Document document,
            @Nullable final String errorMessage) {
        mReviewScreenHandler.onProceedToAnalysisScreen(document, errorMessage);
    }

    public static Intent newInstance(final Document document, final Context context) {
        final Intent intent = new Intent(context, ReviewExampleAppCompatActivity.class);
        intent.putExtra(ReviewExampleAppCompatActivity.EXTRA_IN_DOCUMENT, document);
        return intent;
    }
}

package net.gini.android.capture.review.multipage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.tracking.ReviewScreenEvent;

import java.util.List;

import static net.gini.android.capture.camera.CameraActivity.RESULT_CAMERA_SCREEN;
import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

/**
 * Created by Alpar Szotyori on 16.02.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class MultiPageReviewActivity extends AppCompatActivity implements
        MultiPageReviewFragmentListener {

    private static final String MP_REVIEW_FRAGMENT = "MP_REVIEW_FRAGMENT";
    private static final int ANALYSE_DOCUMENT_REQUEST = 1;

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
    public static final int REQUEST_SCROLL_TO_LAST_PAGE = RESULT_FIRST_USER + 2;

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final int RESULT_SCROLL_TO_LAST_PAGE = RESULT_FIRST_USER + 3;


    public static final String SHOULD_SCROLL_TO_LAST_PAGE = "GC_SHOULD_SCROLL_TO_LAST_PAGE";


    private MultiPageReviewFragment mFragment;
    private boolean mShouldScrollToLastPage = true;
    private int mScrollToPosition = -1;

    public static Intent createIntent(@NonNull final Context context, boolean shouldScrollToLastPage) {
        Intent intent = new Intent(context, MultiPageReviewActivity.class);
        intent.putExtra(SHOULD_SCROLL_TO_LAST_PAGE, shouldScrollToLastPage);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_multi_page_review);

        if (getIntent() != null) {
            mShouldScrollToLastPage = getIntent().getBooleanExtra(SHOULD_SCROLL_TO_LAST_PAGE, false);
        }

        if (savedInstanceState == null) {
            initFragment();
        } else {
            retainFragment();
        }
        enableHomeAsUp(this);
        handleOnBackPressed();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        createFragment();
        showFragment();
    }

    private void handleOnBackPressed() {
        interceptOnBackPressed(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                trackReviewScreenEvent(ReviewScreenEvent.BACK);
            }
        });
    }

    private void initFragment() {
        if (!isFragmentShown()) {
            createFragment();
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(MP_REVIEW_FRAGMENT) != null;
    }

    private void createFragment() {
        mFragment = MultiPageReviewFragment.newInstance();
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_review_multi_page_document, mFragment, MP_REVIEW_FRAGMENT)
                .commit();
    }

    private void retainFragment() {
        mFragment = (MultiPageReviewFragment) getSupportFragmentManager().findFragmentByTag(
                MP_REVIEW_FRAGMENT);
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
    public void onProceedToAnalysisScreen(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        final List documents = multiPageDocument.getDocuments();
        if (documents.isEmpty()) {
            return;
        }
        final Intent intent = new Intent(this, AnalysisActivity.class);
        intent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT, multiPageDocument);
        intent.setExtrasClassLoader(MultiPageReviewActivity.class.getClassLoader());
        startActivityForResult(intent, ANALYSE_DOCUMENT_REQUEST);
    }

    @Override
    public void onReturnToCameraScreenToAddPages() {
        Intent intent = CameraActivity.createIntent(this, true);
        startActivityForResult(intent, REQUEST_SCROLL_TO_LAST_PAGE);
    }

    @Override
    public void onReturnToCameraScreenForFirstPage() {
        setResult(RESULT_CAMERA_SCREEN);
        finish();
    }


    @Override
    public void onImportedDocumentReviewCancelled() {
        finish();
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
            if (resultCode != Activity.RESULT_CANCELED || (data != null)) {
                setResult(resultCode, data);
            }
            finish();
        }

        if (requestCode == REQUEST_SCROLL_TO_LAST_PAGE) {
            if (resultCode == RESULT_SCROLL_TO_LAST_PAGE && data != null) {
                if (data.hasExtra(SHOULD_SCROLL_TO_LAST_PAGE)) {
                    setShouldScrollToLastPage(data.getBooleanExtra(SHOULD_SCROLL_TO_LAST_PAGE, false));
                    data.removeExtra(SHOULD_SCROLL_TO_LAST_PAGE);
                }
            }
        }

    }

    public int getScrollToPosition() {
        return this.mScrollToPosition;
    }

    public void setScrollToPosition(int mScrollToPosition) {
        this.mScrollToPosition = mScrollToPosition;
    }

    public boolean shouldScrollToLastPage() {
        return this.mShouldScrollToLastPage;
    }

    public void setShouldScrollToLastPage(boolean shouldScrollToLastPage) {
        this.mShouldScrollToLastPage = shouldScrollToLastPage;
    }

}

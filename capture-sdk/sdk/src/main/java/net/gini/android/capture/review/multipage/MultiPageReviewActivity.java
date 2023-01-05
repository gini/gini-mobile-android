package net.gini.android.capture.review.multipage;

import static net.gini.android.capture.analysis.AnalysisActivity.RESULT_NO_EXTRACTIONS;
import static net.gini.android.capture.camera.CameraActivity.RESULT_CAMERA_SCREEN;
import static net.gini.android.capture.camera.CameraActivity.RESULT_ENTER_MANUALLY;
import static net.gini.android.capture.error.ErrorActivity.ERROR_SCREEN_REQUEST;
import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;
import static net.gini.android.capture.noresults.NoResultsActivity.NO_RESULT_CANCEL_KEY;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackReviewScreenEvent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.tracking.ReviewScreenEvent;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Alpar Szotyori on 16.02.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * <h3>Screen API</h3>
 *
 * When you use the Screen API and have enabled the multi-page feature, the {@code
 * MultiPageReviewActivity} displays the photographed or imported images and allows the user to
 * review them by checking the order, sharpness, quality and orientation of the images.
 *
 * <p> If multi-page has been enabled then the {@code MultiPageReviewActivity} is started by the
 * {@link CameraActivity} after the user has taken the first photo or imported the first image of a
 * document. For subsequent images the user has to tap on the image stack in the Camera Screen to
 * launch it.
 *
 * <p> <b>Important:</b> A {@link GiniCapture} instance is required to use the {@code
 * MultiPageReviewActivity}
 *
 * <h3>Customizing the Multi-Page Review Screen</h3>
 *
 * Customizing the look of the Review Screen is done via overriding of app resources.
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li><b>Next button style via overriding {@code GiniCaptureTheme.Widget.Button.UnelevatedButton}
 *
 * <li><b>Thumbnails panel background color:</b> via the color resource named {@code
 * gc_multi_page_review_thumbnails_panel_background}
 *
 * <li><b>Add page icon:</b> via svg image {@code gc_add_page.svg}
 *
 * <li> <b>Add page icon subtitle text:</b> via the string resource named {@code gc_pages_txt}
 *
 * <li> <b>Add page icon subtitle text style:</b> via overriding the style named {@code GiniCaptureTheme.Typography.Subtitle2}
 *
 * <li><b>Delete icon:</b> via svg images named {@code gc_button_delete.svg}
 *
 * </ul>
 *
 * <p> <b>Important:</b> All overridden styles must have their respective {@code Root.} prefixed
 * style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Typography.Subtitle2}
 * must be {@code Root.GiniCaptureTheme.Typography.Subtitle2}.
 *
 * <h3>Customizing the Top navigation bar</h3>
 *
 <p>Customizing the top navigation bar is done via setNavigationBarTopAdapter in GiniCapture builder,
 * this is global to all Activities ({@link CameraActivity}, {@link OnboardingActivity}, {@link ReviewActivity},
 * {@link net.gini.android.capture.review.multipage.MultiPageReviewActivity}, {@link AnalysisActivity}).
 *
 * </ul>
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


    public static final String SHOULD_SCROLL_TO_LAST_PAGE = "GC_SHOULD_SCROLL_TO_LAST_PAGE";


    private MultiPageReviewFragment mFragment;
    private boolean mShouldScrollToLastPage = true;

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
        mFragment = MultiPageReviewFragment.newInstance(mShouldScrollToLastPage);
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
        startActivity(intent);
    }

    @Override
    public void onReturnToCameraScreenForFirstPage() {
        Intent intent = CameraActivity.createIntent(this, false);
        startActivity(intent);
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
            if (resultCode == RESULT_NO_EXTRACTIONS) {
                finish();
            } else if (resultCode != Activity.RESULT_CANCELED || (data != null && data.hasExtra(NO_RESULT_CANCEL_KEY))) {
                setResult(resultCode, data);
                finish();
            }
        }

        if (requestCode == ERROR_SCREEN_REQUEST) {
            if (resultCode == RESULT_CAMERA_SCREEN) {
                if (GiniCapture.hasInstance()) {
                    GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore().clear();
                }
                startActivity(CameraActivity.createIntent(MultiPageReviewActivity.this, false));
            }
            if (resultCode == RESULT_ENTER_MANUALLY) {
                setResult(resultCode, data);
            }
            finish();
        }
    }

}

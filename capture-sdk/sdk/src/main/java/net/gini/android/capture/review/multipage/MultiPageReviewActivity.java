package net.gini.android.capture.review.multipage;

import static net.gini.android.capture.analysis.AnalysisActivity.RESULT_NO_EXTRACTIONS;
import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Alpar Szotyori on 16.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * <h3>Screen API</h3>
 *
 * When you use the Screen API and have enabled the multi-page feature, the {@code
 * MultiPageReviewActivity} displays the photographed or imported images and allows the user to
 * review them by checking the order, sharpness, quality and orientation of the images. The user can
 * correct the order by dragging the thumbnails of the images and can also correct the orientation
 * by rotating the images.
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
 * <li> <b>Page indicator text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle}
 *
 * <li> <b>Page indicator font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li><b>Page indicator background color:</b> via the color resource named {@code
 * gc_multi_page_review_page_indicator_background}
 *
 * <li><b>Next button icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_review_fab_checkmark.png}
 *
 * <li><b>Next button color:</b> via the color resources named {@code gc_review_fab} and {@code
 * gc_review_fab_pressed}
 *
 * <li><b>Thumbnails panel background color:</b> via the color resource named {@code
 * gc_multi_page_review_thumbnails_panel_background}
 *
 * <li><b>Thumbnail card background color:</b> via the color resource named {@code
 * gc_multi_page_review_thumbnail_card_background}
 *
 * <li> <b>Thumbnail badge text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle}
 *
 * <li> <b>Thumbnail badge font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li><b>Thumbnail badge background border color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_badge_background_border}
 *
 * <li><b>Thumbnail drag indicator bumps icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
 * named {@code gc_bumps_icon.png}
 *
 * <li><b>Thumbnail highlight strip color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_highlight_strip}
 *
 * <li> <b>Thumbnail activity indicator color:</b> via the color resource named {@code
 * gc_analysis_activity_indicator}
 *
 * <li> <b>Thumbnail upload success background circle color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_upload_success_icon_background}
 *
 * <li> <b>Thumbnail upload success foreground tick color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_upload_success_icon_foreground}
 *
 * <li> <b>Thumbnail upload failure background circle color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_upload_failure_icon_background}
 *
 * <li> <b>Thumbnail upload failure foreground cross color:</b> via the color resource named {@code
 * gc_multi_page_thumbnail_upload_failure_icon_foreground}
 *
 * <li><b>Add page icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_multi_page_add_page_icon.png}
 *
 * <li> <b>Add page icon subtitle text:</b> via the string resource named {@code
 * gc_multi_page_review_add_pages_subtitle}
 *
 * <li> <b>Add page icon subtitle text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle}
 *
 * <li> <b>Add page icon subtitle font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Reorder pages tip text:</b> via the string resource named {@code
 * gc_multi_page_review_reorder_pages_tip}
 *
 * <li> <b>Reorder pages tip text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle}
 *
 * <li> <b>Reorder pages tip font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle} and setting an item named {@code
 * gcCustomFont} with the path to the font file in your {@code assets} folder
 *
 * <li><b>Rotate icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_rotate_icon.png}
 *
 * <li><b>Delete icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_delete_icon.png}
 *
 * <li> <b>Image error message text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.TextStyle} and setting an item named {@code android:textStyle} to
 * {@code normal}, {@code bold} or {@code italic}
 *
 * <li> <b>Image error message font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.TextStyle} and setting an item named {@code gcCustomFont} with the
 * path to the font file in your {@code assets} folder
 *
 * <li> <b>Image error message button text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.Button.TextStyle} and setting an item named {@code
 * android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *
 * <li> <b>Image error message button font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.Button.TextStyle} and setting an item named {@code gcCustomFont}
 * with the path to the font file in your {@code assets} folder
 *
 * <li> <b>Image error message background color:</b> via the color resource named {@code
 * gc_snackbar_error_background}
 *
 * <li> <b>Image analysis error message retry button text:</b> via the string resource named {@code
 * gc_document_analysis_error_retry}
 *
 * <li> <b>Imported image error message delete button text:</b> via the string resource named {@code
 * gc_multi_page_review_delete_invalid_document}
 *
 * <li> <b>Imported image delete last page dialog message:</b> via the string resource named {@code
 * gc_multi_page_review_file_import_delete_last_page_dialog_message}
 *
 * <li> <b>Imported image delete last page dialog positive button text:</b> via the string resource
 * named {@code gc_multi_page_review_file_import_delete_last_page_dialog_positive_button}
 *
 * <li> <b>Imported image delete last page dialog negative button text:</b> via the string resource
 * named {@code gc_multi_page_review_file_import_delete_last_page_dialog_negative_button}
 *
 * <li> <b>Imported image delete last page dialog button color:</b> via the color resource named
 * {@code gc_accent}
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
 * OnboardingActivity}, {@link ReviewActivity}, {@link MultiPageReviewActivity}, {@link
 * AnalysisActivity}).
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li><b>Background color:</b> via the color resource named {@code gc_action_bar} (highly
 * recommended for Android 5+: customize the status bar color via {@code gc_status_bar})
 *
 * <li><b>Title:</b> via the string resource named {@code gc_title_multi_page_review}
 *
 * <li><b>Title color:</b> via the color resource named {@code gc_action_bar_title}
 *
 * <li><b>Back button (only for {@link ReviewActivity}, {@link MultiPageReviewActivity} and {@link
 * AnalysisActivity}):</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code
 * gc_action_bar_back}
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

    private MultiPageReviewFragment mFragment;

    public static Intent createIntent(@NonNull final Context context) {
        return new Intent(context, MultiPageReviewActivity.class);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_multi_page_review);
        if (savedInstanceState == null) {
            initFragment();
        } else {
            retainFragment();
        }
        enableHomeAsUp(this);
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
        mFragment = MultiPageReviewFragment.createInstance();
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
    public void onBackPressed() {
        super.onBackPressed();
        trackReviewScreenEvent(ReviewScreenEvent.BACK);
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
    public void onReturnToCameraScreen() {
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
            if (resultCode == RESULT_NO_EXTRACTIONS) {
                finish();
            } else if (resultCode != Activity.RESULT_CANCELED) {
                setResult(resultCode, data);
                finish();
            }
        }
    }

}

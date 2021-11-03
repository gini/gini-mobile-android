package net.gini.android.capture.analysis;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureCoordinator;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.network.GiniCaptureNetworkApi;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.onboarding.OnboardingActivity;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.tracking.AnalysisScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

/**
 * <h3>Screen API</h3>
 *
 * <p> When you use the Screen API, the {@code AnalysisActivity} displays the captured or imported
 * document and an activity indicator while the document is being analyzed by the Gini API.
 *
 * <p> <b>Note:</b> The title from the ActionBar was removed. Use the activity indicator message
 * instead by overriding the string resource named {@code gc_analysis_activity_indicator_message}. The message is displayed for images
 * only.
 *
 * <p> For PDF documents the first page is shown (only on Android 5.0 Lollipop and newer) along with
 * the PDF's filename and number of pages above the page. On Android KitKat and older only the PDF's filename is shown with the preview area
 * left empty.
 *
 * <p> The preferred way of adding network calls to the Gini Capture SDK is by creating a {@link GiniCapture} instance with a {@link
 * GiniCaptureNetworkService} and a {@link GiniCaptureNetworkApi} implementation.
 *
 * <p> The {@code AnalysisActivity} is started by the {@link CameraActivity} after the user has
 * reviewed the document and either made no changes to the document and it hasn't been analyzed before tapping the Next button, or the user
 * has modified the document, e.g. by rotating it.
 *
 * <p> For imported documents that cannot be reviewed, like PDFs, the {@link CameraActivity} starts
 * the {@code AnalysisActivity} directly.
 *
 * <h3>Customizing the Analysis Screen</h3>
 * <p>
 * Customizing the look of the Analysis Screen is done via overriding of app resources.
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li> <b>Activity indicator color:</b> via the color resource named {@code
 * gc_analysis_activity_indicator}
 *
 * <li> <b>Activity indicator message:</b> via the string resource named {@code
 * gc_analysis_activity_indicator_message}
 *
 * <li> <b>Activity indicator message text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle}
 *
 * <li> <b>Activity indicator message font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in
 * your {@code assets} folder
 *
 * <li> <b>PDF info panel background:</b> via the color resource named {@code
 * gc_analysis_pdf_info_background}
 *
 * <li> <b>PDF filename text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfFilename.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or
 * {@code italic}
 *
 * <li> <b>PDF filename text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfFilename.TextStyle} and setting an item named {@code autoSizeMaxTextSize} and {@code autoSizeMinTextSize} to
 * the desired maximum and minimum {@code sp} sizes
 *
 * <li> <b>PDF filename text color:</b> via the color resource named {@code
 * gc_analysis_pdf_info_text}
 *
 * <li> <b>PDF filename font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfFilename.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your
 * {@code assets} folder
 *
 * <li> <b>PDF page count text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfPageCount.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or
 * {@code italic}
 *
 * <li> <b>PDF page count text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfPageCount.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *
 * <li> <b>PDF page count text color:</b> via the color resource named {@code
 * gc_analysis_pdf_info_text}
 *
 * <li> <b>PDF page count font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Analysis.PdfPageCount.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your
 * {@code assets} folder
 *
 * <li> <b>Background color:</b> via the color resource named {@code gc_background}. <b>Note:</b>
 * this color resource is global to all Activities ({@link CameraActivity}, {@link OnboardingActivity}, {@link ReviewActivity}, {@link
 * AnalysisActivity})
 *
 * <li> <b>Error message text color:</b> via the color resource named {@code
 * gc_snackbar_error_text}
 *
 * <li> <b>Error message font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your {@code
 * assets} folder
 *
 * <li> <b>Error message text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or {@code
 * italic}
 *
 * <li> <b>Error message text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *
 * <li> <b>Error message button text color:</b> via the color resource named {@code
 * gc_snackbar_error_button_title} and {@code gc_snackbar_error_button_title_pressed}
 *
 * <li> <b>Error message button font:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.Button.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your
 * {@code assets} folder
 *
 * <li> <b>Error message button text style:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.Button.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or
 * {@code italic}
 *
 * <li> <b>Error message button text size:</b> via overriding the style named {@code
 * GiniCaptureTheme.Snackbar.Error.Button.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *
 * <li> <b>Error message background color:</b> via the color resource named {@code
 * gc_snackbar_error_background}
 *
 * <li> <b>Document analysis error message retry button text:</b> via the string resource named
 * {@code gc_document_analysis_error_retry}
 *
 * </ul>
 *
 * <p> <b>Important:</b> All overriden styles must have their respective {@code Root.} prefixed
 * style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Snackbar.Error.TextStyle} must be {@code
 * Root.GiniCaptureTheme.Snackbar.Error.TextStyle}.
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * <p> Customizing the Action Bar is also done via overriding of app resources and each one - except
 * the title string resource - is global to all Activities ({@link CameraActivity}, {@link OnboardingActivity}, {@link ReviewActivity},
 * {@link net.gini.android.capture.review.multipage.MultiPageReviewActivity}, {@link AnalysisActivity}).
 *
 * <p> The following items are customizable:
 *
 * <ul>
 *
 * <li> <b>Background color:</b> via the color resource named {@code gc_action_bar} (highly
 * recommended for Android 5+: customize the status bar color via {@code gc_status_bar})
 *
 * <li> <b>Back button (only for {@link ReviewActivity} and {@link AnalysisActivity}):</b> via
 * images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named {@code gc_action_bar_back}
 *
 * </ul>
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

    private static final String ANALYSIS_FRAGMENT = "ANALYSIS_FRAGMENT";

    private String mAnalysisErrorMessage;
    private Document mDocument;
    private AnalysisFragmentCompat mFragment;

    @Override
    public void hideError() {
        mFragment.hideError();
    }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        trackAnalysisScreenEvent(AnalysisScreenEvent.CANCEL);
    }

    @Override
    public void showError(@NonNull final String message, @NonNull final String buttonTitle,
                          @NonNull final View.OnClickListener onClickListener) {
        mFragment.showError(message, buttonTitle, onClickListener);
    }

    @Override
    public void showError(@NonNull final String message, final int duration) {
        mFragment.showError(message, duration);
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
            startActivity(noResultsActivity);
            setResult(RESULT_NO_EXTRACTIONS);
        } else {
            final Intent result = new Intent();
            setResult(RESULT_OK, result);
        }
        finish();
    }

    @Override
    public void onDefaultPDFAppAlertDialogCancelled() {
        finish();
    }
}

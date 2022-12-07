package net.gini.android.capture.noresults;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.ImageRetakeOptionsListener;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.noresults.view.NoResultsNavigationBarBottomAdapter;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.view.InjectedViewContainer;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static net.gini.android.capture.camera.CameraActivity.RESULT_CAMERA_SCREEN;
import static net.gini.android.capture.camera.CameraActivity.RESULT_ENTER_MANUALLY;
import static net.gini.android.capture.internal.util.ActivityHelper.interceptOnBackPressed;

/**
 * <h3>Screen API</h3>
 *
 * <p>
 * When you use the Screen API, the {@code NoResultsFragmentCompat} displays hints that show how to
 * best take a picture of a document.
 * </p>
 *
 * <h3>Customizing the No Results Screen</h3>
 *
 * <p>
 *   Customizing the look of the No Results Screen is done via overriding of app resources.
 * </p>
 * <p>
 *     The following items are customizable:
 *     <ul>
 *         <li>
 *             <b>Header icon:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
 *             {@code gc_alert_icon.png}
 *         </li>
 *         <li>
 *             <b>Header text color:</b> via the color resource named {@code
 *             gc_noresults_header}
 *         </li>
 *         <li>
 *             <b>Header text font:</b> via overriding the style named {@code GiniCaptureTheme
 *             .NoResults.Header.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your {@code assets} folder
 *         </li>
 *         <li>
 *             <b>Header text style:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Header.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *         </li>
 *         <li>
 *             <b>Header text size:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Header.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *         </li>
 *         <li>
 *             <b>Headline text color:</b> via the color resource named {@code gc_noresults_headline}
 *         </li>
 *         <li>
 *             <b>Headline text font:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Headline.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your {@code assets} folder
 *         </li>
 *         <li>
 *             <b>Headline text style:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Headline.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *         </li>
 *         <li>
 *             <b>Headline text size:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Headline.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *         </li>
 *         <li>
 *             <b>Tip text color:</b> via the color resource named {@code gc_noresults_tip}
 *         </li>
 *         <li>
 *             <b>Tip text font:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Tip.TextStyle} and setting an item named {@code gcCustomFont} with the path to the font file in your {@code assets} folder
 *         </li>
 *         <li>
 *             <b>Tip text style:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Tip.TextStyle} and setting an item named {@code android:textStyle} to {@code normal}, {@code bold} or {@code italic}
 *         </li>
 *         <li>
 *             <b>Tip text size:</b> via overriding the style named {@code GiniCaptureTheme.NoResults.Tip.TextStyle} and setting an item named {@code android:textSize} to the desired {@code sp} size
 *         </li>
 *         <li>
 *             <b>Tip image - Good lighting:</b> via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
 *             named
 *             {@code gc_photo_tip_lighting.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Document should be flat:</b> via images for mdpi, hdpi, xhdpi, xxhdpi,
 *             xxxhdpi
 *             named {@code gc_photo_tip_flat.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Device should be parallel to document:</b> via images for mdpi, hdpi,
 *             xhdpi,xxhdpi, xxxhdpi named {@code gc_photo_tip_parallel.png}
 *         </li>
 *         <li>
 *             <b>Tip image - Document should be aligned with corner guides:</b> via
 *             images for mdpi, hdpi, xhdpi,xxhdpi, xxxhdpi named {@code gc_photo_tip_align.png}
 *         </li>
 *         <li>
 *             <b>Button color:</b> via the color resource named {@code gc_noresults_button}
 *         </li>
 *         <li>
 *             <b>Button text color:</b> via the color resource named {@code gc_noresults_button_text}
 *         </li>
 *         <li>
 *             <b>Background color:</b> via the color resource named {@code gc_noresults_background}.
 *         </li>
 *     </ul>
 * </p>
 *
 * <p>
 *     <b>Important:</b> All overriden styles must have their respective {@code Root.} prefixed style as their parent. Ex.: the parent of {@code GiniCaptureTheme.Onboarding.Message.TextStyle} must be {@code Root.GiniCaptureTheme.Onboarding.Message.TextStyle}.
 * </p>
 *
 * <h3>Customizing the Action Bar</h3>
 *
 * <p>
 * Customizing the Action Bar is done via overriding of app resources and each one - except the
 * title string resource - is global to all Activities ({@link CameraActivity}, {@link
 * NoResultsActivity}, {@link ReviewActivity}, {@link AnalysisActivity}).
 * </p>
 * <p>
 * The following items are customizable:
 * <ul>
 * <li>
 * <b>Background color:</b> via the color resource named {@code gc_action_bar} (highly recommended
 * for Android 5+: customize the status bar color via {@code gc_status_bar})
 * </li>
 * </ul>
 * </p>
 */
public class NoResultsActivity extends AppCompatActivity implements ImageRetakeOptionsListener {

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
    public static final String NO_RESULT_CANCEL_KEY = "GC_NO_RESULT_CANCEL";

    private Document mDocument;

    @Override
    public void onBackToCameraPressed() {
        setResult(RESULT_CAMERA_SCREEN);
        finish();
    }

    @Override
    public void onEnterManuallyPressed() {
        setResult(RESULT_ENTER_MANUALLY);
        finish();
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_noresults);
        setTitle("");
        readExtras();
        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
        }
        if (savedInstanceState == null) {
            initFragment();
        }
        setupNoResultsBottomNavigationBar();
        handleOnBackPressed();
    }

    private void handleOnBackPressed() {
        interceptOnBackPressed(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent noResultsIntent = new Intent();
                noResultsIntent.putExtra(NO_RESULT_CANCEL_KEY, true);
                setResult(Activity.RESULT_CANCELED, noResultsIntent);
                finish();
            }
        });
    }

    private void readExtras() {
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDocument = extras.getParcelable(EXTRA_IN_DOCUMENT);
        }
        checkRequiredExtras();
    }

    private void checkRequiredExtras() {
        if (mDocument == null) {
            throw new IllegalStateException(
                    "NoResultsActivity requires a Document. Set it as an extra using the EXTRA_IN_DOCUMENT key.");
        }
    }

    private void initFragment() {
        final NoResultsFragmentCompat noResultsFragment = NoResultsFragmentCompat.createInstance(
                mDocument);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_noresults, noResultsFragment)
                .commit();
    }

    private void setupNoResultsBottomNavigationBar() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            InjectedViewContainer<NoResultsNavigationBarBottomAdapter> injectedViewContainer =
                    findViewById(R.id.gc_injected_navigation_bar_container_bottom);
            NoResultsNavigationBarBottomAdapter adapter = GiniCapture.getInstance().getNoResultsNavigationBarBottomAdapter();
            injectedViewContainer.setInjectedViewAdapter(adapter);

            adapter.setOnBackButtonClickListener(v -> {
                onBackPressed();
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

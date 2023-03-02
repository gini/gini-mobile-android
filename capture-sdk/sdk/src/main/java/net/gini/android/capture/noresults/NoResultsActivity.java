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
import net.gini.android.capture.internal.ui.IntervalClickListener;
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
 * Internal use only.
 *
 * The {@code NoResultsActivity} displays hints that show how to best use the SDK.
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
            // TODO: will be removed
//            InjectedViewContainer<NoResultsNavigationBarBottomAdapter> injectedViewContainer =
//                    findViewById(R.id.gc_injected_navigation_bar_container_bottom);
//            NoResultsNavigationBarBottomAdapter adapter = GiniCapture.getInstance().getNoResultsNavigationBarBottomAdapter();
//            injectedViewContainer.setInjectedViewAdapter(adapter);
//
//            adapter.setOnBackButtonClickListener(new IntervalClickListener(v -> onBackPressed()));
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

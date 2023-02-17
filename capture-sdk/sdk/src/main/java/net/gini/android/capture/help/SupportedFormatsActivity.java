package net.gini.android.capture.help;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import android.os.Bundle;
import android.view.MenuItem;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.noresults.NoResultsActivity;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Internal use only.
 */
public class SupportedFormatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_supported_formats);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }
        setUpFormatsList();
        forcePortraitOrientationOnPhones(this);
        setupHomeButton();
        setupBottomBarNavigation();
        setupTopBarNavigation();
    }

    private void setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            enableHomeAsUp(this);
        }
    }

    private void setupTopBarNavigation() {
        InjectedViewContainer<NavigationBarTopAdapter> topBarInjectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_top);
        if (GiniCapture.hasInstance()) {

            topBarInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            NavigationBarTopAdapter topBarAdapter = topBarInjectedViewContainer.getInjectedViewAdapter();
            assert topBarAdapter != null;
            topBarAdapter.setNavButtonType(NavButtonType.BACK);
            topBarAdapter.setTitle(getString(R.string.gc_title_supported_formats));

            topBarAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> onBackPressed()));
        }
    }

    private void setupBottomBarNavigation() {
        InjectedViewContainer<HelpNavigationBarBottomAdapter> injectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {

            injectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getHelpNavigationBarBottomAdapter());

            HelpNavigationBarBottomAdapter helpNavigationBarBottomAdapter = injectedViewContainer.getInjectedViewAdapter();
            assert helpNavigationBarBottomAdapter != null;
            helpNavigationBarBottomAdapter.setOnBackClickListener(new IntervalClickListener(v -> {
                onBackPressed();
            }));
        }
    }

    private void setUpFormatsList() {
        final RecyclerView recyclerView = findViewById(R.id.gc_formats_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new SupportedFormatsAdapter());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

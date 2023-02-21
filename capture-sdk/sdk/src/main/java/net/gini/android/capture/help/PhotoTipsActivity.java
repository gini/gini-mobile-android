package net.gini.android.capture.help;

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

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

/**
 * Internal use only.
 */
public class PhotoTipsActivity extends AppCompatActivity {

    static final int RESULT_SHOW_CAMERA_SCREEN = RESULT_FIRST_USER + 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }

        setContentView(R.layout.gc_activity_photo_tips);

        forcePortraitOrientationOnPhones(this);
        setupHomeButton();
        setupTipList();
        setupBottomBarNavigation();
        setupTopBarNavigation();
    }

    private void setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            enableHomeAsUp(this);
        }
    }

    private void setupBottomBarNavigation() {
        InjectedViewContainer<HelpNavigationBarBottomAdapter> injectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {

            injectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getHelpNavigationBarBottomAdapter());

            HelpNavigationBarBottomAdapter helpNavigationBarBottomAdapter = GiniCapture.getInstance().getHelpNavigationBarBottomAdapter();
            helpNavigationBarBottomAdapter.setOnBackClickListener(new IntervalClickListener(v -> {
                onBackPressed();
            }));
        }
    }

    private void setupTopBarNavigation() {
        InjectedViewContainer<NavigationBarTopAdapter> topBarInjectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_top);
        if (GiniCapture.hasInstance()) {

            topBarInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            NavigationBarTopAdapter topBarAdapter = topBarInjectedViewContainer.getInjectedViewAdapter();
            assert topBarAdapter != null;
            topBarAdapter.setNavButtonType(NavButtonType.BACK);
            topBarAdapter.setTitle(getString(R.string.gc_title_photo_tips));

            topBarAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> onBackPressed()));
        }
    }

    private void setupTipList() {
        final RecyclerView recyclerView = findViewById(R.id.gc_tips_recycleview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PhotoTipsAdapter(this, false));
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

package net.gini.android.capture.help;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import android.os.Bundle;
import android.view.MenuItem;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
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
        setupBottomBarNavigation();
        setupTopBarNavigation();
    }


    private void setupTopBarNavigation() {
        InjectedViewContainer<NavigationBarTopAdapter> topBarInjectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_top);
        if (GiniCapture.hasInstance()) {
            topBarInjectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getNavigationBarTopAdapterInstance(),
                    injectedViewAdapter -> {
                        injectedViewAdapter.setNavButtonType(GiniCapture.getInstance().isBottomNavigationBarEnabled() ? NavButtonType.NONE : NavButtonType.BACK);
                        injectedViewAdapter.setTitle(getString(R.string.gc_title_supported_formats));

                        injectedViewAdapter.setOnNavButtonClickListener(new IntervalClickListener(v -> onBackPressed()));
                    }));
        }
    }

    private void setupBottomBarNavigation() {
        InjectedViewContainer<HelpNavigationBarBottomAdapter> injectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_bottom);
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().isBottomNavigationBarEnabled()) {
            injectedViewContainer.setInjectedViewAdapterHolder(new InjectedViewAdapterHolder<>(
                    GiniCapture.getInstance().internal().getHelpNavigationBarBottomAdapterInstance(),
                    injectedViewAdapter -> {
                        injectedViewAdapter.setOnBackClickListener(new IntervalClickListener(v -> {
                            onBackPressed();
                        }));
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

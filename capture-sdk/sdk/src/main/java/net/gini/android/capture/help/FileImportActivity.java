package net.gini.android.capture.help;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import com.google.android.material.snackbar.Snackbar;

/**
 * Internal use only.
 */
public class FileImportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_file_import);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }
        forcePortraitOrientationOnPhones(this);

        // Show illustration for the first section only if available (height > 0)
        final Drawable section1Illustration = ContextCompat.getDrawable(this,
                R.drawable.gc_file_import_section_1_illustration);
        if (section1Illustration.getMinimumHeight() > 0) {
            final ImageView section1ImageView = findViewById(R.id.gc_section_1_illustration);
            section1ImageView.setVisibility(View.VISIBLE);
            section1ImageView.setImageDrawable(section1Illustration);
        }

        waitForHalfSecondAndShowSnackBar();
        setupBottomBarNavigation();
        setupTopBarNavigation();
    }

    private void waitForHalfSecondAndShowSnackBar() {
        new Handler(Looper.getMainLooper()).postDelayed(this::showCustomSnackBar, 500);
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

    private void showCustomSnackBar() {
        ConstraintLayout constraintLayout = findViewById(R.id.gc_file_import_constraint_layout);

        Snackbar snackbar = Snackbar.make(constraintLayout, "", Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(Color.TRANSPARENT);

        Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        int bottomPadding = GiniCapture.getInstance().isBottomNavigationBarEnabled()
                ? (int) getResources().getDimension(R.dimen.gc_large_96) : 0;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarLayout.getLayoutParams();

        params.setMargins((int)getResources().getDimension(R.dimen.gc_medium), 0, (int)getResources().getDimension(R.dimen.gc_medium), bottomPadding);

        snackbar.getView().setLayoutParams(params);

        View view = getLayoutInflater().inflate(R.layout.gc_snackbar_info, null);

        TextView dismissTxt = view.findViewById(R.id.gc_snackbar_dismiss);
        dismissTxt.setOnClickListener(v -> {
            if (snackbar.isShown())
                snackbar.dismiss();
        });

        snackBarLayout.addView(view, 0);

        snackbar.show();

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

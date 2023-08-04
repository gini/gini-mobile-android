package net.gini.android.capture.help;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.R;
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter;
import net.gini.android.capture.internal.ui.IntervalClickListener;
import net.gini.android.capture.view.InjectedViewAdapterHolder;
import net.gini.android.capture.view.InjectedViewContainer;
import net.gini.android.capture.view.NavButtonType;
import net.gini.android.capture.view.NavigationBarTopAdapter;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

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
                        injectedViewAdapter.setTitle(getString(R.string.gc_title_file_import));

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

        Snackbar snackbar = Snackbar.make(constraintLayout, getString(R.string.gc_snackbar_illustrations), Snackbar.LENGTH_INDEFINITE);
        snackbar.setTextMaxLines(5);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true);
        @ColorInt int color = typedValue.data;

        snackbar.setAction(getString(R.string.gc_snackbar_dismiss), v -> snackbar.dismiss());
        snackbar.setActionTextColor(color); // snackbar action text color

        Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        int bottomPadding = GiniCapture.getInstance().isBottomNavigationBarEnabled()
                ? (int) getResources().getDimension(R.dimen.gc_large_96) : (int) getResources().getDimension(R.dimen.gc_large);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarLayout.getLayoutParams();
        params.setMargins((int)getResources().getDimension(R.dimen.gc_large), 0, (int)getResources().getDimension(R.dimen.gc_large), bottomPadding);
        snackbar.getView().setLayoutParams(params);
        snackbar.getView().setMinimumHeight((int)getResources().getDimension(R.dimen.gc_snackbar_text_height));
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

package net.gini.android.capture.help;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

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

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;
import static net.gini.android.capture.internal.util.ActivityHelper.forcePortraitOrientationOnPhones;

import com.google.android.material.card.MaterialCardView;
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

    private void setupTopBarNavigation() {
        InjectedViewContainer<NavigationBarTopAdapter> topBarInjectedViewContainer = findViewById(R.id.gc_injected_navigation_bar_container_top);
        if (GiniCapture.hasInstance()) {

            topBarInjectedViewContainer.setInjectedViewAdapter(GiniCapture.getInstance().getNavigationBarTopAdapter());

            NavigationBarTopAdapter topBarAdapter = topBarInjectedViewContainer.getInjectedViewAdapter();
            assert topBarAdapter != null;
            topBarAdapter.setNavButtonType(NavButtonType.BACK);
            topBarAdapter.setTitle(getString(R.string.gc_title_file_import));

            topBarAdapter.setOnNavButtonClickListener(
                    new IntervalClickListener(v ->
                    onBackPressed())
            );
        }
    }

    private void showCustomSnackBar() {
        ConstraintLayout constraintLayout = findViewById(R.id.gc_file_import_constraint_layout);

        Snackbar snackbar = Snackbar.make(constraintLayout, "", Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(Color.TRANSPARENT);

        Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        int bottomPadding = GiniCapture.getInstance().isBottomNavigationBarEnabled()
                ? (int) getResources().getDimension(R.dimen.xxxxxlarge) : 0;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarLayout.getLayoutParams();

        params.setMargins((int)getResources().getDimension(R.dimen.medium), 0, (int)getResources().getDimension(R.dimen.medium), bottomPadding);

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

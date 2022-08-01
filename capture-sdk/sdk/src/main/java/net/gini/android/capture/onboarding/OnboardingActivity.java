package net.gini.android.capture.onboarding;

import static net.gini.android.capture.internal.util.ActivityHelper.enableHomeAsUp;

import android.os.Bundle;
import android.view.MenuItem;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.R;
import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.camera.CameraActivity;
import net.gini.android.capture.review.ReviewActivity;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

/**
 * <p>The {@code OnboardingActivity} displays important advice for correctly photographing a document.
 *
 * <p>It is started by the {@link CameraActivity} when the latter is
 * launched for the first time. You may disable this behavior - we highly recommend keeping it - by
 * passing {@code false} to {@link GiniCapture.Builder#setShouldShowOnboardingAtFirstRun(boolean)}
 * when creating a {@link GiniCapture} instance.
 *
 * <p> You can change the number of displayed pages and their content (image and short text) by
 * setting an {@code ArrayList} containing {@link OnboardingPage} objects when building a {@link
 * GiniCapture} instance with {@link GiniCapture.Builder#setCustomOnboardingPages(ArrayList)}.
 */
public class OnboardingActivity extends AppCompatActivity implements OnboardingFragmentListener {

    private static final String ONBOARDING_FRAGMENT = "ONBOARDING_FRAGMENT";

    private OnboardingFragment mOnboardingFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gc_activity_onboarding);
        if (!GiniCapture.hasInstance()) {
            finish();
            return;
        }
        initFragment();
        setupHomeButton();
    }

    private void setupHomeButton() {
        if (GiniCapture.hasInstance() && GiniCapture.getInstance().areBackButtonsEnabled()) {
            enableHomeAsUp(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initFragment() {
        if (!isFragmentShown()) {
            createFragment();
            showFragment();
        }
    }

    private boolean isFragmentShown() {
        return getSupportFragmentManager().findFragmentByTag(ONBOARDING_FRAGMENT) != null;
    }

    private void createFragment() {
        if (GiniCapture.hasInstance()) {
            final ArrayList<OnboardingPage> onboardingPages =
                    GiniCapture.getInstance().getCustomOnboardingPages();
            if (onboardingPages != null) {
                mOnboardingFragment = OnboardingFragment.createInstance(
                        onboardingPages);
                return;
            }
        }
        mOnboardingFragment = new OnboardingFragment();
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.gc_fragment_onboarding, mOnboardingFragment, ONBOARDING_FRAGMENT)
                .commit();
    }

    @Override
    public void onCloseOnboarding() {
        finish();
    }

    @Override
    public void onError(@NonNull final GiniCaptureError giniCaptureError) {

    }

    @VisibleForTesting
    void showFragment(@NonNull final OnboardingFragment onboardingFragment) {
        if (mOnboardingFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mOnboardingFragment)
                    .commit();
        }
        mOnboardingFragment = onboardingFragment;
        showFragment();
    }
}

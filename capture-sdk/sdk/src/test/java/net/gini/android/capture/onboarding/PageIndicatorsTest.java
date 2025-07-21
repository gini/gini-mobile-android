package net.gini.android.capture.onboarding;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static net.gini.android.capture.onboarding.PageIndicatorsHelper.isPageActive;
import static net.gini.android.capture.onboarding.PageIndicatorsHelper.isPageInactive;

@RunWith(AndroidJUnit4.class)
//TODO: remove after upgrading to robolectric to 4.16
@Config(maxSdk = 35)
public class PageIndicatorsTest {

    @Test
    public void should_createPageIndicatorImageViews() {
        final OnboardingFragment.PageIndicators pageIndicators = createPageIndicatorsInstance(2);

        assertThat(pageIndicators.getPageIndicatorImageViews().size()).isEqualTo(2);
    }

    @Test
    public void should_setActiveRequiredPageIndicator() {
        final OnboardingFragment.PageIndicators pageIndicators = createPageIndicatorsInstance(2);
        pageIndicators.setActive(0);

        isPageActive(pageIndicators, 0);
        isPageInactive(pageIndicators, 1);

        pageIndicators.setActive(1);

        isPageInactive(pageIndicators, 0);
        isPageActive(pageIndicators, 1);
    }

    @NonNull
    private OnboardingFragment.PageIndicators createPageIndicatorsInstance(final int nrOfPages) {
        final LinearLayout linearLayout = new LinearLayout(ApplicationProvider.getApplicationContext());
        final OnboardingFragment.PageIndicators pageIndicators =
                new OnboardingFragment.PageIndicators(
                        ApplicationProvider.getApplicationContext(), nrOfPages, linearLayout);
        pageIndicators.create();
        return pageIndicators;
    }


}

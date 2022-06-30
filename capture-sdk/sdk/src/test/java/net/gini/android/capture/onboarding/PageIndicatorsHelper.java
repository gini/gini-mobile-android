package net.gini.android.capture.onboarding;

import static com.google.common.truth.Truth.assertAbout;

import static net.gini.android.capture.onboarding.PageIndicatorImageViewSubject.pageIndicatorImageView;

import com.google.common.truth.Truth;

import net.gini.android.capture.R;

public final class PageIndicatorsHelper {

    public static void isPageActive(final OnboardingFragment.PageIndicators pageIndicators,
            final int pageNr) {
        Truth.assertThat(pageIndicators.getPageIndicatorImageViews().get(pageNr).getImageAlpha())
                .isEqualTo(255);
    }

    public static void isPageInactive(final OnboardingFragment.PageIndicators pageIndicators,
            final int pageNr) {
        Truth.assertThat(pageIndicators.getPageIndicatorImageViews().get(pageNr).getImageAlpha())
                .isLessThan(255);
    }

    private PageIndicatorsHelper() {
    }
}

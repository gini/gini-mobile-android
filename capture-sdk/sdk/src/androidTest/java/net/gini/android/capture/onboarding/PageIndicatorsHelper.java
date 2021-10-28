package net.gini.android.capture.onboarding;

import static com.google.common.truth.Truth.assertAbout;

import static net.gini.android.capture.onboarding.PageIndicatorImageViewSubject.pageIndicatorImageView;

import net.gini.android.capture.R;

public final class PageIndicatorsHelper {

    public static void isPageActive(final OnboardingFragmentImpl.PageIndicators pageIndicators,
            final int pageNr) {
        assertAbout(pageIndicatorImageView()).that(
                pageIndicators.getPageIndicatorImageViews().get(pageNr)).showsDrawable(
                R.drawable.gc_onboarding_indicator_active);
    }

    public static void isPageInactive(final OnboardingFragmentImpl.PageIndicators pageIndicators,
            final int pageNr) {
        assertAbout(pageIndicatorImageView()).that(
                pageIndicators.getPageIndicatorImageViews().get(pageNr)).showsDrawable(
                R.drawable.gc_onboarding_indicator_inactive);
    }

    private PageIndicatorsHelper() {
    }
}

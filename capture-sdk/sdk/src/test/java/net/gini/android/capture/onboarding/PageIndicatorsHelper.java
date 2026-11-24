package net.gini.android.capture.onboarding;



import com.google.common.truth.Truth;


public final class PageIndicatorsHelper {

    public static void assertPageActive(final OnboardingFragment.PageIndicators pageIndicators,
            final int pageNr) {
        Truth.assertThat(pageIndicators.getPageIndicatorImageViews().get(pageNr).getImageAlpha())
                .isEqualTo(255);
    }

    public static void assertPageInactive(final OnboardingFragment.PageIndicators pageIndicators,
            final int pageNr) {
        Truth.assertThat(pageIndicators.getPageIndicatorImageViews().get(pageNr).getImageAlpha())
                .isLessThan(255);
    }

    private PageIndicatorsHelper() {
    }
}

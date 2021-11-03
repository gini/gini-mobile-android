package net.gini.android.capture.onboarding;

import net.gini.android.capture.R;
import net.gini.android.capture.internal.util.FeatureConfiguration;

import java.util.ArrayList;

import androidx.annotation.VisibleForTesting;

/**
 * Internal use only.
 *
 * @suppress
 */
public enum DefaultPagesPhone {
    FLAT(new OnboardingPage(R.string.gc_onboarding_flat, R.drawable.gc_onboarding_flat)),
    PARALLEL(
            new OnboardingPage(R.string.gc_onboarding_parallel, R.drawable.gc_onboarding_parallel)),
    ALIGN(new OnboardingPage(R.string.gc_onboarding_align, R.drawable.gc_onboarding_align));

    private final OnboardingPage mOnboardingPage;

    DefaultPagesPhone(final OnboardingPage onboardingPage) {
        mOnboardingPage = onboardingPage;
    }

    @VisibleForTesting
    OnboardingPage getPage() {
        return mOnboardingPage;
    }

    public static ArrayList<OnboardingPage> asArrayList() { // NOPMD - ArrayList required (Bundle)
        final ArrayList<OnboardingPage> arrayList = new ArrayList<>(values().length);
        for (final DefaultPagesPhone pages : values()) {
            arrayList.add(pages.getPage());
        }
        if (FeatureConfiguration.isMultiPageEnabled()) {
            arrayList.add(ConditionalPages.MULTI_PAGE.getPage());
        }
        return arrayList;
    }
}

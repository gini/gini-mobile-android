package net.gini.android.capture.onboarding;

import net.gini.android.capture.internal.ui.FragmentImplCallback;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

/**
 * Internal use only.
 *
 * @suppress
 */
public interface OnboardingFragmentImplCallback extends FragmentImplCallback {

    PagerAdapter getViewPagerAdapter(@NonNull List<OnboardingPage> pages);
}

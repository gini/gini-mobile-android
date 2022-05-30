package net.gini.android.capture.onboarding;

import android.annotation.SuppressLint;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

class ViewPagerAdapterCompat extends FragmentPagerAdapter {

    private final List<OnboardingPage> mPages;

    @SuppressLint("WrongConstant")
    public ViewPagerAdapterCompat(@NonNull final FragmentManager fm,
                                  @NonNull final List<OnboardingPage> pages) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mPages = pages;
    }

    @NonNull
    protected List<OnboardingPage> getPages() {
        return mPages;
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public Fragment getItem(final int position) {
        final boolean isLastPage = position == getCount() - 1;
        return OnboardingPageFragmentCompat.createInstance(getPages().get(position), isLastPage);
    }
}

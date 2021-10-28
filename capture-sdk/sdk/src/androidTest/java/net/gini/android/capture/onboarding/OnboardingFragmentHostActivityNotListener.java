package net.gini.android.capture.onboarding;

import net.gini.android.capture.test.FragmentHostActivity;
import net.gini.android.capture.test.R;

import java.util.ArrayList;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class OnboardingFragmentHostActivityNotListener extends
        FragmentHostActivity<OnboardingFragmentCompat> {

    static OnboardingFragmentListener sListener;

    @Override
    protected void setListener() {
        if (sListener != null) {
            getFragment().setListener(sListener);
        }
    }

    @Override
    protected OnboardingFragmentCompat createFragment() {
        final ArrayList<OnboardingPage> pages = new ArrayList<>();
        pages.add(new OnboardingPage(R.string.gc_onboarding_flat,
                R.drawable.gc_onboarding_flat));
        return OnboardingFragmentCompat.createInstanceWithoutEmptyLastPage(pages);
    }

}

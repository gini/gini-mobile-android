package net.gini.android.capture.onboarding;

import net.gini.android.capture.GiniCaptureError;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class OnboardingFragmentHostActivity extends
        OnboardingFragmentHostActivityNotListener implements OnboardingFragmentListener {

    private boolean isClosed;

    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void onCloseOnboarding() {
        isClosed = true;
    }

    @Override
    public void onError(@NonNull final GiniCaptureError error) {

    }
}

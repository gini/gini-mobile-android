package net.gini.android.capture.onboarding;

import android.os.Bundle;

import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;

final class OnboardingPageFragmentHelper {

    private static final String ARGS_PAGE = "GC_PAGE";

    static Bundle createArguments(@NonNull final OnboardingPage page) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_PAGE, page);
        return arguments;
    }

    static OnboardingPageFragmentImpl createFragmentImpl(
            @NonNull final FragmentImplCallback fragment, @NonNull final Bundle arguments) {
        final OnboardingPage page = arguments.getParcelable(ARGS_PAGE);
        if (page == null) {
            throw new IllegalStateException("Missing OnboardingPage.");
        }
        return new OnboardingPageFragmentImpl(fragment, page);
    }

    private OnboardingPageFragmentHelper() {
    }
}

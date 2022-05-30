package net.gini.android.capture.onboarding;

import android.os.Bundle;

import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;

final class OnboardingPageFragmentHelper {

    private static final String ARGS_PAGE = "GC_PAGE";
    private static final String ARGS_IS_LAST_PAGE = "GC_IS_LAST_PAGE";

    static Bundle createArguments(@NonNull final OnboardingPage page, final boolean isLastPage) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_PAGE, page);
        arguments.putBoolean(ARGS_IS_LAST_PAGE, isLastPage);
        return arguments;
    }

    static OnboardingPageFragmentImpl createFragmentImpl(
            @NonNull final FragmentImplCallback fragment, @NonNull final Bundle arguments) {
        final OnboardingPage page = arguments.getParcelable(ARGS_PAGE);
        final boolean isLastPage = arguments.getBoolean(ARGS_IS_LAST_PAGE);
        if (page == null) {
            throw new IllegalStateException("Missing OnboardingPage.");
        }
        return new OnboardingPageFragmentImpl(fragment, page, isLastPage);
    }

    private OnboardingPageFragmentHelper() {
    }
}

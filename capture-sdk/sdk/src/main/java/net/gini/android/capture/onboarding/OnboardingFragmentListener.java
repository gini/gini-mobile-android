package net.gini.android.capture.onboarding;

import net.gini.android.capture.GiniCaptureError;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * <p>
 * Interface used by {@link OnboardingFragment} to dispatch events to the hosting Activity.
 * </p>
 *
 * @suppress
 */
interface OnboardingFragmentListener {
    /**
     * <p>
     *     Called when the user has left the last page - either by swiping or tapping on the Next button - and you should
     *     close the Onboarding Fragment.
     * </p>
     */
    void onCloseOnboarding();

    /**
     * <p>
     * Called when an error occurred.
     * </p>
     * @param error details about what went wrong
     */
    void onError(@NonNull GiniCaptureError error);
}
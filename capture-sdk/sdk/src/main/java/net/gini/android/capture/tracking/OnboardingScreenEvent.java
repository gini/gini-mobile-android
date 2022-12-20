package net.gini.android.capture.tracking;

/**
 * Created by Alpar Szotyori on 03.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * Events triggered on the onboarding screen.
 *
 * <p> All events trigger on Screen APIs.
 */
public enum OnboardingScreenEvent {
    /**
     * Triggers when the first onboarding page is shown.(<b>Screen API</b>)
     */
    START,
    /**
     * Triggers when the user presses the next button on the last page.(<b>Screen API</b>)
     */
    FINISH
}

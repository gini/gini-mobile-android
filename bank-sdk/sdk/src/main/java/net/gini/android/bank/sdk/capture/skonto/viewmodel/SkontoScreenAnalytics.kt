package net.gini.android.bank.sdk.capture.skonto.viewmodel

import net.gini.android.bank.sdk.capture.skonto.mapper.toAnalyticsModel
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

internal class SkontoScreenAnalytics(
    private val analyticsTracker: UserAnalyticsEventTracker,
) {

    internal fun logScreenShownEvent(
        isSkontoSwitchActive: Boolean,
        skontoEdgeCase: SkontoEdgeCase?,
    ) {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOfNotNull(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto),
                UserAnalyticsEventProperty.SwitchActive(isSkontoSwitchActive),
                skontoEdgeCase?.let { UserAnalyticsEventProperty.EdgeCaseType(it.toAnalyticsModel()) }
            )
        )
    }

    internal  fun logHelpClicked() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.HELP_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto))
        )
    }

    internal  fun logBackClicked() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.CLOSE_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto))
        )
    }

    internal  fun logSkontoAmountTapped() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.FINAL_AMOUNT_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto))
        )
    }

    internal  fun logFullAmountTapped() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.FULL_AMOUNT_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto))
        )
    }

    internal  fun logDueDateTapped() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.DUE_DATE_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto))
        )
    }
}

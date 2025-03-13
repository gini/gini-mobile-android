package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

internal class SkontoActiveChangeIntent(
    private val analyticsTracker: UserAnalyticsEventTracker,
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
) {

    fun SkontoScreenContainerHost.run(newValue: Boolean) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        logAnalytics(newValue)

        val totalAmount = if (newValue) state.skontoAmount else state.fullAmount
        val discount = getSkontoDiscountPercentageUseCase.execute(
            state.skontoAmount.value,
            state.fullAmount.value
        )

        reduce {
            state.copy(
                isSkontoSectionActive = newValue,
                totalAmount = totalAmount,
                skontoPercentage = discount
            )
        }
    }

    private fun logAnalytics(newValue: Boolean) {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.SKONTO_SWITCH_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto),
                UserAnalyticsEventProperty.SwitchActive(newValue)
            )
        )
    }
}

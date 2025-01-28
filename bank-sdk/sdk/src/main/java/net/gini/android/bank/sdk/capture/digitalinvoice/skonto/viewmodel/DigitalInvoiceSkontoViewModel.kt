package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel

import androidx.lifecycle.ViewModel
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoSideEffect
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.BackClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.KeyboardStateChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.mapper.toAnalyticsModel
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.math.BigDecimal
import java.time.LocalDate

internal typealias SkontoContainerHost =
        ContainerHost<SkontoScreenState, SkontoSideEffect>

internal class DigitalInvoiceSkontoViewModel(
    args: DigitalInvoiceSkontoArgs,
    skontoScreenInitialStateFactory: SkontoScreenInitialStateFactory,
    private val analyticsTracker: UserAnalyticsEventTracker,
    private val invoiceClickIntent: InvoiceClickIntent,
    private val backClickIntent: BackClickIntent,
    private val infoBannerInteractionIntent: InfoBannerInteractionIntent,
    private val keyboardStateChangeIntent: KeyboardStateChangeIntent,
    private val skontoDueDateChangeIntent: SkontoDueDateChangeIntent,
    private val skontoAmountFieldChangeIntent: SkontoAmountFieldChangeIntent,
) : ViewModel(), SkontoContainerHost {

    override val container: Container<SkontoScreenState, SkontoSideEffect> =
        container(skontoScreenInitialStateFactory.create(args.data, args.isSkontoSectionActive)) {
            logScreenShownEvent()
        }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) =
        with(skontoAmountFieldChangeIntent) { run(newValue) }

    fun onSkontoDueDateChanged(newDate: LocalDate) =
        with(skontoDueDateChangeIntent) { run(newDate) }

    fun onKeyboardStateChanged(isVisible: Boolean) =
        with(keyboardStateChangeIntent) { run(isVisible) }

    fun onInfoBannerClicked() =
        with(infoBannerInteractionIntent) { runClick() }

    fun onInfoDialogDismissed() =
        with(infoBannerInteractionIntent) { runDismiss() }

    fun onInvoiceClicked() =
        with(invoiceClickIntent) { run() }

    fun onBackClicked() =
        with(backClickIntent) {
            logBackClicked()
            run()
        }

    fun onSkontoAmountFieldFocused(){
        logSkontoAmountTapped()
    }

    fun onDueDateFieldFocused(){
        logDueDateTapped()
    }


    fun onHelpClicked() =
        intent {
            logHelpClicked()
            postSideEffect(SkontoSideEffect.OpenHelpScreen)
        }


    private fun logScreenShownEvent() = intent {
        val state = state as SkontoScreenState.Ready? ?: return@intent
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.SCREEN_SHOWN,
            setOfNotNull(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant),
                UserAnalyticsEventProperty.SwitchActive(state.isSkontoSectionActive),
                state.edgeCase?.let { UserAnalyticsEventProperty.EdgeCaseType(it.toAnalyticsModel()) }
            )
        )
    }

    private fun logHelpClicked() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.HELP_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant))
        )
    }

    private fun logBackClicked() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.BACK_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant))
        )
    }

    private fun logSkontoAmountTapped() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.FINAL_AMOUNT_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant))
        )
    }

    private fun logDueDateTapped() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.DUE_DATE_TAPPED,
            setOf(UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant))
        )
    }
}

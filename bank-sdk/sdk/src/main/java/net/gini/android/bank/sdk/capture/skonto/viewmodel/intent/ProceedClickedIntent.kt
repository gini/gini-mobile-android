package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.mapper.toAnalyticsModel
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent.OpenExtractionsScreenSubIntent
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocsFeatureEnabledUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

internal class ProceedClickedIntent(
    private val analyticsTracker: UserAnalyticsEventTracker,
    private val openExtractionsScreenSubIntent: OpenExtractionsScreenSubIntent,
    private val getTransactionDocShouldBeAutoAttachedUseCase: GetTransactionDocShouldBeAutoAttachedUseCase,
    private val getTransactionDocsFeatureEnabledUseCase: GetTransactionDocsFeatureEnabledUseCase,
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase,
) {

    fun SkontoScreenContainerHost.run(skontoFragmentListener: SkontoFragmentListener?) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        if (!getTransactionDocsFeatureEnabledUseCase()) {
            with(openExtractionsScreenSubIntent) {
                run(skontoFragmentListener)
            }
            return@intent
        }
        if (getTransactionDocShouldBeAutoAttachedUseCase()) {
            transactionDocDialogConfirmAttachUseCase(true)
            with(openExtractionsScreenSubIntent) {
                run(skontoFragmentListener)
            }
        } else {
            reduce { state.copy(transactionDialogVisible = true) }
        }

        logProceedClickEvent(state)
    }

    private fun logProceedClickEvent(state: SkontoScreenState.Ready) {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.PROCEED_TAPPED,
            setOfNotNull(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.Skonto),
                state.edgeCase?.toAnalyticsModel()
                    ?.let { UserAnalyticsEventProperty.EdgeCaseType(it) }
            ),
        )
    }
}

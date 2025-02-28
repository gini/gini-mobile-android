package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoSideEffect
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost
import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsScreen
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsEventProperty

internal class InvoiceClickIntent(
    private val analyticsTracker: UserAnalyticsEventTracker,
    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider,
    private val skontoInvoicePreviewTextLinesFactory: SkontoInvoicePreviewTextLinesFactory
) {

    fun SkontoContainerHost.run() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        logAnalyticsEvent()

        val documentId = lastAnalyzedDocumentProvider.provide()?.giniApiDocumentId ?: return@intent

        val skontoData = SkontoData(
            skontoAmountToPay = state.skontoAmount,
            skontoDueDate = state.discountDueDate,
            skontoPercentageDiscounted = state.skontoPercentage,
            skontoRemainingDays = state.paymentInDays,
            fullAmountToPay = state.fullAmount,
            skontoPaymentMethod = state.paymentMethod,
        )
        val infoTextLines = skontoInvoicePreviewTextLinesFactory.create(
            skontoData
        )

        postSideEffect(SkontoSideEffect.OpenInvoiceScreen(documentId, infoTextLines))
    }

    private fun logAnalyticsEvent() {
        analyticsTracker.trackEvent(
            UserAnalyticsEvent.INVOICE_PREVIEW_TAPPED,
            setOf(
                UserAnalyticsEventProperty.Screen(UserAnalyticsScreen.SkontoReturnAssistant)
            )
        )
    }
}

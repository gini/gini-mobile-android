package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenSideEffect
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider

internal class InvoiceClickIntent(
    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider,
    private val skontoInvoicePreviewTextLinesFactory: SkontoInvoicePreviewTextLinesFactory
) {

    fun SkontoScreenContainerHost.run() = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent

        val skontoData = SkontoData(
            skontoAmountToPay = state.skontoAmount,
            skontoDueDate = state.discountDueDate,
            skontoPercentageDiscounted = state.skontoPercentage,
            skontoRemainingDays = state.paymentInDays,
            fullAmountToPay = state.fullAmount,
            skontoPaymentMethod = state.paymentMethod,
        )
        val documentId = lastAnalyzedDocumentProvider.provide()?.giniApiDocumentId ?: return@intent

        postSideEffect(
            SkontoScreenSideEffect.OpenInvoiceScreen(
                documentId,
                skontoInvoicePreviewTextLinesFactory.create(skontoData)
            )
        )
    }
}

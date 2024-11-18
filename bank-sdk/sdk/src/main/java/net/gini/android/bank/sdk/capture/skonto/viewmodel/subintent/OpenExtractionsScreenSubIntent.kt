@file:OptIn(OrbitExperimental::class)

package net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent

import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoExtractionsHandler
import net.gini.android.bank.sdk.capture.skonto.SkontoFragmentListener
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import net.gini.android.capture.provider.LastExtractionsProvider
import org.orbitmvi.orbit.annotation.OrbitExperimental

internal class OpenExtractionsScreenSubIntent(
    private val skontoExtractionsHandler: SkontoExtractionsHandler,
    private val lastExtractionsProvider: LastExtractionsProvider,
) {

    suspend fun SkontoScreenContainerHost.run(listener: SkontoFragmentListener?) = subIntent {
        val state = state as? SkontoScreenState.Ready ?: return@subIntent
        skontoExtractionsHandler.updateExtractions(
            totalAmount = state.totalAmount,
            skontoPercentage = state.skontoPercentage,
            skontoAmount = state.skontoAmount,
            paymentInDays = state.paymentInDays,
            discountDueDate = state.discountDueDate.toString(),
        )
        lastExtractionsProvider.update(skontoExtractionsHandler.getExtractions().toMutableMap())
        listener?.onPayInvoiceWithSkonto(
            skontoExtractionsHandler.getExtractions(),
            skontoExtractionsHandler.getCompoundExtractions()
        )
    }
}

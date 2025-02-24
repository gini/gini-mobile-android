package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase

internal class SkontoScreenInitialStateFactory(
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
) {

    fun create(data: SkontoData, isSkontoSectionActive: Boolean): SkontoScreenState.Ready {
        val discount = data.skontoPercentageDiscounted

        val paymentMethod = data.skontoPaymentMethod ?: SkontoData.SkontoPaymentMethod.Unspecified
        val edgeCase = getSkontoEdgeCaseUseCase.execute(data.skontoDueDate, paymentMethod)

        return SkontoScreenState.Ready(
            isSkontoSectionActive = isSkontoSectionActive,
            paymentInDays = data.skontoRemainingDays,
            skontoPercentage = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            paymentMethod = paymentMethod,
            edgeCase = edgeCase,
            edgeCaseInfoDialogVisible = edgeCase != null,
            skontoAmountValidationError = null,
        )
    }
}

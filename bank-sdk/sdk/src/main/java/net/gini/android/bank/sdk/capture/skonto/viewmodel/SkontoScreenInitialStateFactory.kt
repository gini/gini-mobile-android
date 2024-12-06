package net.gini.android.bank.sdk.capture.skonto.viewmodel

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.capture.Amount

internal class SkontoScreenInitialStateFactory(
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
    private val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase,
) {

    fun create(data: SkontoData): SkontoScreenState.Ready {

        val discount = data.skontoPercentageDiscounted

        val paymentMethod = data.skontoPaymentMethod ?: SkontoData.SkontoPaymentMethod.Unspecified
        val edgeCase = getSkontoEdgeCaseUseCase.execute(data.skontoDueDate, paymentMethod)

        val isSkontoSectionActive = getSkontoDefaultSelectionStateUseCase.execute(edgeCase)

        val totalAmount =
            if (isSkontoSectionActive) data.skontoAmountToPay else data.fullAmountToPay

        val savedAmountValue = getSkontoSavedAmountUseCase.execute(
            data.skontoAmountToPay.value,
            data.fullAmountToPay.value
        )
        val savedAmount = Amount(savedAmountValue, data.fullAmountToPay.currency)

        return SkontoScreenState.Ready(
            isSkontoSectionActive = isSkontoSectionActive,
            paymentInDays = data.skontoRemainingDays,
            skontoPercentage = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            edgeCase = edgeCase,
            edgeCaseInfoDialogVisible = edgeCase != null,
            savedAmount = savedAmount,
            transactionDialogVisible = false,
            skontoAmountValidationError = null,
            fullAmountValidationError = null,
        )
    }
}

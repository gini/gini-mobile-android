package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoArgs
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args.DigitalInvoiceSkontoResultArgs
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import java.math.BigDecimal
import java.time.LocalDate

internal class DigitalInvoiceSkontoViewModel(
    args: DigitalInvoiceSkontoArgs,
    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider,
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
    private val getSkontoRemainingDaysUseCase: GetSkontoRemainingDaysUseCase,
) : ViewModel() {

    val stateFlow: MutableStateFlow<DigitalInvoiceSkontoScreenState> =
        MutableStateFlow(createInitalState(args.data, args.isSkontoSectionActive))

    val sideEffectFlow: MutableSharedFlow<DigitalInvoiceSkontoSideEffect> = MutableSharedFlow()

    internal fun provideFragmentResult(): DigitalInvoiceSkontoResultArgs {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready
                ?: error("Can't extract result. State is not ready")

        return DigitalInvoiceSkontoResultArgs(
            skontoData = SkontoData(
                skontoAmountToPay = currentState.skontoAmount,
                skontoDueDate = currentState.discountDueDate,
                skontoPercentageDiscounted = currentState.skontoPercentage,
                skontoRemainingDays = currentState.paymentInDays,
                fullAmountToPay = currentState.fullAmount,
                skontoPaymentMethod = currentState.paymentMethod,
            ),
            isSkontoEnabled = currentState.isSkontoSectionActive,
        )
    }

    private fun createInitalState(
        data: SkontoData,
        isSkontoSectionActive: Boolean,
    ): DigitalInvoiceSkontoScreenState.Ready {


        val discount = data.skontoPercentageDiscounted

        val paymentMethod =
            data.skontoPaymentMethod ?: SkontoData.SkontoPaymentMethod.Unspecified
        val edgeCase = getSkontoEdgeCaseUseCase.execute(data.skontoDueDate, paymentMethod)

        return DigitalInvoiceSkontoScreenState.Ready(
            isSkontoSectionActive = isSkontoSectionActive,
            paymentInDays = data.skontoRemainingDays,
            skontoPercentage = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            paymentMethod = paymentMethod,
            edgeCase = edgeCase,
            edgeCaseInfoDialogVisible = edgeCase != null,
        )
    }

    fun onSkontoActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch
        val discount =
            getSkontoDiscountPercentageUseCase.execute(
                currentState.skontoAmount.value,
                currentState.fullAmount.value
            )

        stateFlow.emit(
            currentState.copy(
                isSkontoSectionActive = newValue,
                skontoPercentage = discount
            )
        )
    }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch

        if (newValue > currentState.fullAmount.value) {
            stateFlow.emit(
                currentState.copy(skontoAmount = currentState.skontoAmount)
            )
            return@launch
        }

        val discount = getSkontoDiscountPercentageUseCase.execute(
            newValue,
            currentState.fullAmount.value
        )

        val newSkontoAmount = currentState.skontoAmount.copy(value = newValue)

        stateFlow.emit(
            currentState.copy(
                skontoAmount = newSkontoAmount,
                skontoPercentage = discount,
            )
        )
    }

    fun onSkontoDueDateChanged(newDate: LocalDate) = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch
        val newPayInDays = getSkontoRemainingDaysUseCase.execute(newDate)
        stateFlow.emit(
            currentState.copy(
                discountDueDate = newDate,
                paymentInDays = newPayInDays,
                edgeCase = getSkontoEdgeCaseUseCase.execute(
                    dueDate = newDate,
                    paymentMethod = currentState.paymentMethod
                )
            )
        )
    }

    fun onInfoBannerClicked() = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch
        stateFlow.emit(
            currentState.copy(
                edgeCaseInfoDialogVisible = true,
            )
        )
    }

    fun onInfoDialogDismissed() = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch
        stateFlow.emit(
            currentState.copy(
                edgeCaseInfoDialogVisible = false,
            )
        )
    }

    fun onInvoiceClicked() = viewModelScope.launch {
        val currentState =
            stateFlow.value as? DigitalInvoiceSkontoScreenState.Ready ?: return@launch
        val documentId = lastAnalyzedDocumentProvider.provide()?.first ?: return@launch
        sideEffectFlow.emit(DigitalInvoiceSkontoSideEffect.OpenInvoiceScreen(
            documentId,
            SkontoData(
                skontoAmountToPay = currentState.skontoAmount,
                skontoDueDate = currentState.discountDueDate,
                skontoPercentageDiscounted = currentState.skontoPercentage,
                skontoRemainingDays = currentState.paymentInDays,
                fullAmountToPay = currentState.fullAmount,
                skontoPaymentMethod = currentState.paymentMethod,
            )
        ))
    }

    fun onHelpClicked() = viewModelScope.launch {
        sideEffectFlow.emit(DigitalInvoiceSkontoSideEffect.OpenHelpScreen)
    }
}

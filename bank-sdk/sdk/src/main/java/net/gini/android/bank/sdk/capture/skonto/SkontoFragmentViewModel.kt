package net.gini.android.bank.sdk.capture.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoExtractionsHandler
import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.GetTransactionDocShouldBeAutoAttachedUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogCancelAttachUseCase
import net.gini.android.bank.sdk.transactiondocs.internal.usecase.TransactionDocDialogConfirmAttachUseCase
import net.gini.android.capture.Amount
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import net.gini.android.capture.provider.LastExtractionsProvider
import java.math.BigDecimal
import java.time.LocalDate

internal class SkontoFragmentViewModel(
    private val data: SkontoData,
    private val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase,
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
    private val getSkontoAmountUseCase: GetSkontoAmountUseCase,
    private val getSkontoRemainingDaysUseCase: GetSkontoRemainingDaysUseCase,
    private val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase,
    private val skontoExtractionsHandler: SkontoExtractionsHandler,
    private val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider,
    private val skontoInvoicePreviewTextLinesFactory: SkontoInvoicePreviewTextLinesFactory,
    private val lastExtractionsProvider: LastExtractionsProvider,
    private val transactionDocDialogConfirmAttachUseCase: TransactionDocDialogConfirmAttachUseCase,
    private val transactionDocDialogCancelAttachUseCase: TransactionDocDialogCancelAttachUseCase,
    private val getTransactionDocShouldBeAutoAttachedUseCase: GetTransactionDocShouldBeAutoAttachedUseCase,
) : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoFragmentContract.State> =
        MutableStateFlow(createInitalState(data))

    val sideEffectFlow: MutableSharedFlow<SkontoFragmentContract.SideEffect> = MutableSharedFlow()

    private var listener: SkontoFragmentListener? = null

    fun setListener(listener: SkontoFragmentListener?) {
        this.listener = listener
    }

    fun onProceedClicked() = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        if (getTransactionDocShouldBeAutoAttachedUseCase()) {
            onConfirmAttachTransactionDocClicked(true)
        } else {
            stateFlow.emit(currentState.copy(transactionDialogVisible = true))
        }
    }

    fun onConfirmAttachTransactionDocClicked(alwaysAttach: Boolean) = viewModelScope.launch {
        transactionDocDialogConfirmAttachUseCase(alwaysAttach)
        openExtractionsScreen()
    }

    fun onCancelAttachTransactionDocClicked() = viewModelScope.launch {
        transactionDocDialogCancelAttachUseCase()
        openExtractionsScreen()
    }

    private fun openExtractionsScreen() {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return
        skontoExtractionsHandler.updateExtractions(
            totalAmount = currentState.totalAmount,
            skontoPercentage = currentState.skontoPercentage,
            skontoAmount = currentState.skontoAmount,
            paymentInDays = currentState.paymentInDays,
            discountDueDate = currentState.discountDueDate.toString(),
        )
        lastExtractionsProvider.update(skontoExtractionsHandler.getExtractions().toMutableMap())
        listener?.onPayInvoiceWithSkonto(
            skontoExtractionsHandler.getExtractions(),
            skontoExtractionsHandler.getCompoundExtractions()
        )
    }

    private fun createInitalState(
        data: SkontoData,
    ): SkontoFragmentContract.State.Ready {

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

        return SkontoFragmentContract.State.Ready(
            isSkontoSectionActive = isSkontoSectionActive,
            paymentInDays = data.skontoRemainingDays,
            skontoPercentage = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            skontoEdgeCase = edgeCase,
            edgeCaseInfoDialogVisible = edgeCase != null,
            savedAmount = savedAmount,
            transactionDialogVisible = false,
        )
    }

    fun onSkontoActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount = if (newValue) currentState.skontoAmount else currentState.fullAmount
        val discount = getSkontoDiscountPercentageUseCase.execute(
            currentState.skontoAmount.value,
            currentState.fullAmount.value
        )

        stateFlow.emit(
            currentState.copy(
                isSkontoSectionActive = newValue,
                totalAmount = totalAmount,
                skontoPercentage = discount
            )
        )
    }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch

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

        val totalAmount = if (currentState.isSkontoSectionActive)
            newValue
        else currentState.fullAmount.value

        val newSkontoAmount = currentState.skontoAmount.copy(value = newValue)
        val newTotalAmount = currentState.totalAmount.copy(value = totalAmount)

        val savedAmountValue = getSkontoSavedAmountUseCase.execute(
            newSkontoAmount.value,
            currentState.fullAmount.value
        )

        val savedAmount = Amount(savedAmountValue, currentState.fullAmount.currency)

        stateFlow.emit(
            currentState.copy(
                skontoAmount = newSkontoAmount,
                skontoPercentage = discount,
                totalAmount = newTotalAmount,
                savedAmount = savedAmount,
            )
        )
    }

    fun onSkontoDueDateChanged(newDate: LocalDate) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val newPayInDays = getSkontoRemainingDaysUseCase.execute(newDate)
        stateFlow.emit(
            currentState.copy(
                discountDueDate = newDate,
                paymentInDays = newPayInDays,
                skontoEdgeCase = getSkontoEdgeCaseUseCase.execute(
                    dueDate = newDate,
                    paymentMethod = currentState.paymentMethod
                )
            )
        )
    }

    fun onFullAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount =
            if (currentState.isSkontoSectionActive) currentState.skontoAmount.value else newValue

        val discount = currentState.skontoPercentage

        val skontoAmount = getSkontoAmountUseCase.execute(newValue, discount)

        val savedAmountValue = getSkontoSavedAmountUseCase.execute(
            skontoAmount,
            newValue
        )

        val savedAmount = Amount(savedAmountValue, currentState.fullAmount.currency)

        stateFlow.emit(
            currentState.copy(
                skontoAmount = currentState.skontoAmount.copy(value = skontoAmount),
                fullAmount = currentState.fullAmount.copy(value = newValue),
                totalAmount = currentState.totalAmount.copy(value = totalAmount),
                savedAmount = savedAmount,
            )
        )
    }

    fun onInfoBannerClicked() = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(
            currentState.copy(
                edgeCaseInfoDialogVisible = true,
            )
        )
    }

    fun onInfoDialogDismissed() = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(
            currentState.copy(
                edgeCaseInfoDialogVisible = false,
            )
        )
    }

    fun onInvoiceClicked() = viewModelScope.launch {
        val currentState =
            stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val skontoData = SkontoData(
            skontoAmountToPay = currentState.skontoAmount,
            skontoDueDate = currentState.discountDueDate,
            skontoPercentageDiscounted = currentState.skontoPercentage,
            skontoRemainingDays = currentState.paymentInDays,
            fullAmountToPay = currentState.fullAmount,
            skontoPaymentMethod = currentState.paymentMethod,
        )
        val documentId = lastAnalyzedDocumentProvider.provide()?.giniApiDocumentId ?: return@launch
        sideEffectFlow.emit(
            SkontoFragmentContract.SideEffect.OpenInvoiceScreen(
                documentId,
                skontoInvoicePreviewTextLinesFactory.create(skontoData)
            )
        )
    }
}

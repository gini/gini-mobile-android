package net.gini.android.bank.sdk.capture.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

internal class SkontoFragmentViewModel(
    private val data: SkontoData,
) : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoFragmentContract.State> =
        MutableStateFlow(createInitalState(data))

    private fun createInitalState(
        data: SkontoData,
    ): SkontoFragmentContract.State.Ready {


        val discount = data.skontoPercentageDiscounted

        val paymentMethod = data.skontoPaymentMethod ?: SkontoData.SkontoPaymentMethod.Unspecified
        val edgeCase = extractSkontoEdgeCase(data.skontoDueDate, paymentMethod)

        val isSkontoSectionActive = edgeCase != SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly
                && edgeCase != SkontoFragmentContract.SkontoEdgeCase.SkontoExpired

        val totalAmount =
            if (isSkontoSectionActive) data.skontoAmountToPay else data.fullAmountToPay

        val savedAmountValue = calculateSavedAmount(data.skontoAmountToPay.amount, data.fullAmountToPay.amount)
        val savedAmount = SkontoData.Amount(savedAmountValue, data.fullAmountToPay.currencyCode)

        return SkontoFragmentContract.State.Ready(
            isSkontoSectionActive = true,
            paymentInDays = data.skontoRemainingDays,
            discountAmount = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            skontoEdgeCase = edgeCase,
            edgeCaseInfoDialogVisible = edgeCase != null,
            skontoAmountValidation = validateSkontoAmount(
                skontoAmount = data.skontoAmountToPay,
                fullAmount = data.fullAmountToPay
            ),
            savedAmount = savedAmount
        )
    }

    fun onSkontoActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount = if (newValue) currentState.skontoAmount else currentState.fullAmount
        val discount =
            calculateDiscount(currentState.skontoAmount.amount, currentState.fullAmount.amount)

        stateFlow.emit(
            currentState.copy(
                isSkontoSectionActive = newValue,
                totalAmount = totalAmount,
                discountAmount = discount
            )
        )
    }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val discount = calculateDiscount(newValue, currentState.fullAmount.amount)
        val totalAmount = if (currentState.isSkontoSectionActive)
            newValue
        else
            currentState.fullAmount.amount

        val newSkontoAmount = currentState.skontoAmount.copy(amount = newValue)
        val newTotalAmount = currentState.totalAmount.copy(amount = totalAmount)

        val savedAmountValue = calculateSavedAmount(newSkontoAmount.amount, currentState.fullAmount.amount)
        val savedAmount = SkontoData.Amount(savedAmountValue, currentState.fullAmount.currencyCode)

        stateFlow.emit(
            currentState.copy(
                skontoAmount = newSkontoAmount,
                discountAmount = discount,
                totalAmount = newTotalAmount,
                skontoAmountValidation = validateSkontoAmount(newSkontoAmount, currentState.fullAmount),
                savedAmount = savedAmount,
            )
        )
    }

    fun onSkontoDueDateChanged(newDate: LocalDate) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val newPayInDays = ChronoUnit.DAYS.between(newDate, LocalDate.now()).absoluteValue.toInt()
        stateFlow.emit(
            currentState.copy(
                discountDueDate = newDate,
                paymentInDays = newPayInDays,
                skontoEdgeCase = extractSkontoEdgeCase(
                    dueDate = newDate,
                    paymentMethod = currentState.paymentMethod
                )
            )
        )
    }

    fun onFullAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount =
            if (currentState.isSkontoSectionActive) currentState.skontoAmount.amount else newValue

        val discount = currentState.discountAmount

        val skontoAmount = newValue.minus(
            newValue.multiply( // full_amount - (full_amount * (discount / 100))
                discount.divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            )
        )

        val savedAmountValue = calculateSavedAmount(skontoAmount, newValue)
        val savedAmount = SkontoData.Amount(savedAmountValue, currentState.fullAmount.currencyCode)

        stateFlow.emit(
            currentState.copy(
                skontoAmount = currentState.skontoAmount.copy(amount = skontoAmount),
                fullAmount = currentState.fullAmount.copy(amount = newValue),
                totalAmount = currentState.totalAmount.copy(amount = totalAmount),
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

    private fun calculateDiscount(skontoAmount: BigDecimal, fullAmount: BigDecimal): BigDecimal {
        if (fullAmount == BigDecimal.ZERO) return BigDecimal("100")
        return BigDecimal.ONE
            .minus(skontoAmount.divide(fullAmount, 4, RoundingMode.HALF_UP))
            .multiply(BigDecimal("100"))
            .coerceAtLeast(BigDecimal.ZERO)
    }

    private fun calculateSavedAmount(skontoAmount: BigDecimal, fullAmount: BigDecimal) =
        fullAmount.minus(skontoAmount).abs()

    private fun validateSkontoAmount(
        skontoAmount: SkontoData.Amount,
        fullAmount: SkontoData.Amount
    ): SkontoFragmentContract.State.Ready.SkontoAmountValidation {
        return when {
            skontoAmount.amount <= fullAmount.amount ->
                SkontoFragmentContract.State.Ready.SkontoAmountValidation.Valid

            else ->
                SkontoFragmentContract.State.Ready.SkontoAmountValidation.Invalid.SkontoAmountGreaterOfFullAmount
        }
    }

    private fun extractSkontoEdgeCase(
        dueDate: LocalDate,
        paymentMethod: SkontoData.SkontoPaymentMethod,
    ): SkontoFragmentContract.SkontoEdgeCase? {
        val today = LocalDate.now()
        return when {
            dueDate.isBefore(today) ->
                SkontoFragmentContract.SkontoEdgeCase.SkontoExpired

            dueDate == today ->
                SkontoFragmentContract.SkontoEdgeCase.SkontoLastDay

            paymentMethod == SkontoData.SkontoPaymentMethod.Cash ->
                SkontoFragmentContract.SkontoEdgeCase.PayByCashOnly

            else -> null
        }
    }
}
package net.gini.android.bank.sdk.capture.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

internal class SkontoFragmentViewModel(
    private val data: SkontoData,
) : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoFragmentContract.State> =
        MutableStateFlow(createInitalState(data))

    private fun createInitalState(
        data: SkontoData,
    ): SkontoFragmentContract.State.Ready {

        val isSkontoSectionActive = true

        val totalAmount =
            if (isSkontoSectionActive) data.skontoAmountToPay else data.fullAmountToPay
        val discount = data.skontoPercentageDiscounted

        return SkontoFragmentContract.State.Ready(
            isSkontoSectionActive = true,
            paymentInDays = data.skontoRemainingDays,
            discountAmount = discount,
            skontoAmount = data.skontoAmountToPay,
            discountDueDate = data.skontoDueDate,
            fullAmount = data.fullAmountToPay,
            totalAmount = totalAmount,
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

        stateFlow.emit(
            currentState.copy(
                skontoAmount = currentState.skontoAmount.copy(amount = newValue),
                discountAmount = discount,
                totalAmount = currentState.totalAmount.copy(amount = totalAmount),
            )
        )
    }

    fun onSkontoDueDateChanged(newDate: LocalDate) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(currentState.copy(discountDueDate = newDate))
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

        stateFlow.emit(
            currentState.copy(
                skontoAmount = currentState.skontoAmount.copy(amount = skontoAmount),
                fullAmount = currentState.fullAmount.copy(amount = newValue),
                totalAmount = currentState.totalAmount.copy(amount = totalAmount)
            )
        )
    }

    private fun calculateDiscount(skontoAmount: BigDecimal, fullAmount: BigDecimal): BigDecimal {
        if (fullAmount == BigDecimal.ZERO) return BigDecimal("100")
        return BigDecimal.ONE
            .minus(skontoAmount.divide(fullAmount, 4, RoundingMode.HALF_UP))
            .multiply(BigDecimal("100"))
    }
}
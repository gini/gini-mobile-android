package net.gini.android.bank.sdk.capture.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class SkontoFragmentViewModel : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoFragmentContract.State> =
        MutableStateFlow(
            createInitalState(
                skontoAmount = BigDecimal("97"),
                fullAmount = BigDecimal("100"),
                paymentInDays = 14,
                isSkontoSectionActive = true,
                currency = "EUR",
                discountDueDate = LocalDate.now()
            )
        )

    private fun createInitalState(
        skontoAmount: BigDecimal,
        fullAmount: BigDecimal,
        paymentInDays: Int,
        isSkontoSectionActive: Boolean = true,
        currency: String = "EUR",
        discountDueDate: LocalDate,
    ): SkontoFragmentContract.State.Ready {

        val totalAmount = if (isSkontoSectionActive) skontoAmount else fullAmount
        val discount = calculateDiscount(skontoAmount, fullAmount)

        return SkontoFragmentContract.State.Ready(
            isSkontoSectionActive = true,
            paymentInDays = paymentInDays,
            discountValue = discount,
            skontoAmount = skontoAmount,
            discountDueDate = discountDueDate,
            fullAmount = fullAmount,
            totalAmount = totalAmount,
            currency = currency,
        )
    }

    fun onSkontoActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount = if (newValue) currentState.skontoAmount else currentState.fullAmount
        val discount = calculateDiscount(currentState.skontoAmount, currentState.fullAmount)

        stateFlow.emit(
            currentState.copy(
                isSkontoSectionActive = newValue,
                totalAmount = totalAmount,
                discountValue = discount
            )
        )
    }

    fun onSkontoAmountFieldChanged(newValue: BigDecimal) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val discount = calculateDiscount(newValue, currentState.fullAmount)
        val totalAmount =
            if (currentState.isSkontoSectionActive) newValue else currentState.fullAmount

        stateFlow.emit(
            currentState.copy(
                skontoAmount = newValue,
                discountValue = discount,
                totalAmount = totalAmount,
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
            if (currentState.isSkontoSectionActive) currentState.skontoAmount else newValue
        val discount = currentState.discountValue
        val skontoAmount = newValue.minus(
            newValue.multiply( // full_amount - (full_amount * (discount / 100))
                discount.divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            )
        )

        stateFlow.emit(
            currentState.copy(
                skontoAmount = skontoAmount,
                fullAmount = newValue,
                totalAmount = totalAmount
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
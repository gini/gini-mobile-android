package net.gini.android.bank.sdk.capture.skonto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SkontoFragmentViewModel : ViewModel() {

    val stateFlow: MutableStateFlow<SkontoFragmentContract.State> =
        MutableStateFlow(
            SkontoFragmentContract.State.Ready(
                isSkontoSectionActive = true,
                paymentInDays = 14,
                discountValue = 3.0f,
                skontoAmount = 97.0f,
                discountDueDate = LocalDate.now(),
                fullAmount = "100",
                totalAmount = 97.0f,
                totalDiscount = 3.0f,
                currency = "EUR",
            )
        )

    fun onSkontoActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        val totalAmount = if (newValue) currentState.skontoAmount else currentState.fullAmount
        stateFlow.emit(
            currentState.copy(
                isSkontoSectionActive = newValue,
            )
        )
    }

    fun onSkontoAmountFieldChanged(newValue: String) = viewModelScope.launch {
        val floatValue = newValue.replace(",", ".").toFloatOrNull() ?: 0f
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(
            currentState.copy(
                skontoAmount = floatValue,
            )
        )
    }

    fun onSkontoDueDateChanged(newDate: LocalDate) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(currentState.copy(discountDueDate = newDate))
    }

    fun onFullAmountFieldChanged(newValue: String) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(currentState.copy(fullAmount = newValue))
    }

    private fun getCurrentTotalAmount(state: SkontoFragmentContract.State.Ready) =
        if (state.isSkontoSectionActive) state.skontoAmount else state.fullAmount
}
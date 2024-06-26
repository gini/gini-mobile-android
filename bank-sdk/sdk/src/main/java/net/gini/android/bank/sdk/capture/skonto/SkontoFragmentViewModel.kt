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
                isDiscountSectionActive = true,
                paymentInDays = 14,
                discountValue = 3.0f,
                amountWithDiscount = 97.0f,
                discountDueDate = LocalDate.now(),
                withoutDiscountAmount = 100.0f,
                totalAmount = 97.0f,
                totalDiscount = 3.0f
            )
        )

    fun onDiscountSectionActiveChanged(newValue: Boolean) = viewModelScope.launch {
        val currentState = stateFlow.value as? SkontoFragmentContract.State.Ready ?: return@launch
        stateFlow.emit(currentState.copy(isDiscountSectionActive = newValue))
    }
}
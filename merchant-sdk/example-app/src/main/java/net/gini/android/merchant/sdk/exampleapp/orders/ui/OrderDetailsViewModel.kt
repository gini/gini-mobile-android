package net.gini.android.merchant.sdk.exampleapp.orders.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import net.gini.android.merchant.sdk.exampleapp.orders.data.model.Order
import kotlin.time.Duration.Companion.milliseconds

class OrderDetailsViewModel : ViewModel() {

    private val _orderFlow = MutableStateFlow(Order("", "", "", ""))

    @OptIn(FlowPreview::class)
    val orderFlow = _orderFlow.asStateFlow().debounce(300.milliseconds)

    fun setOrder(order: Order) {
        _orderFlow.value = order
    }

    fun getOrder(): Order {
        return _orderFlow.value
    }

    fun updateRecipient(recipient: String) {
        _orderFlow.value = _orderFlow.value.copy(recipient = recipient)
    }

    fun updateIBAN(iban: String) {
        _orderFlow.value = _orderFlow.value.copy(iban = iban)
    }

    fun updateAmount(amount: String) {
        _orderFlow.value = _orderFlow.value.copy(amount = amount)
    }

    fun updatePurpose(purpose: String) {
        _orderFlow.value = _orderFlow.value.copy(purpose = purpose)
    }
}
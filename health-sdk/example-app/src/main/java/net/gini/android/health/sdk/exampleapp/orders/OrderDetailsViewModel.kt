package net.gini.android.health.sdk.exampleapp.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class OrderDetailsViewModel(
    private val giniHealth: GiniHealth
) : ViewModel() {

    private val _orderFlow = MutableStateFlow(Order(UUID.randomUUID().toString(),"", "", "", ""))

    @OptIn(FlowPreview::class)
    val orderFlow = _orderFlow.asStateFlow().debounce(300.milliseconds)

    fun getOrder(): Order {
        return _orderFlow.value
    }

    fun setOrder(order: Order) {
        _orderFlow.value = order.copy(
            amount = if (order.amount.indexOf(":") != -1) {
                order.amount.substring(0, order.amount.indexOf(":"))
            } else {
                order.amount
            }
        )
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

    fun createPaymentRequest() = viewModelScope.launch {
        when (val paymentProvidersAppsState = giniHealth.giniInternalPaymentModule.paymentComponent.paymentProviderAppsFlow.value) {
            is PaymentProviderAppsState.Success -> {
                val paymentProviders = paymentProvidersAppsState.paymentProviderApps
                val paymentProviderForRequest = paymentProviders.first { it.paymentProvider.id == PAYMENT_PROVIDER_ID_FOR_PAYMENT_REQUEST }.runCatching {
                    val paymentRequest = giniHealth.giniInternalPaymentModule.getPaymentRequest(this, paymentDetails = PaymentDetails(
                        recipient = _orderFlow.value.recipient,
                        amount = _orderFlow.value.amount,
                        purpose = _orderFlow.value.purpose,
                        iban = _orderFlow.value.iban
                    ))
                }.onFailure {
                    // error
                }
            }
            else -> {
                // log error
            }
        }
    }

    companion object {
        const val PAYMENT_PROVIDER_ID_FOR_PAYMENT_REQUEST = "b09ef70a-490f-11eb-952e-9bc6f4646c57"
    }
}
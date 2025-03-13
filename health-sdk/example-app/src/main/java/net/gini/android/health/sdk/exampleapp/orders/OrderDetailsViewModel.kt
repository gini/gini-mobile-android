package net.gini.android.health.sdk.exampleapp.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.orders.data.OrdersRepository
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.utils.isValidIban
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class OrderDetailsViewModel(
    private val giniHealth: GiniHealth,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _orderFlow = MutableStateFlow(Order(UUID.randomUUID().toString(),"", "", "", ""))

    @OptIn(FlowPreview::class)
    val orderFlow = _orderFlow.asStateFlow().debounce(300.milliseconds)

    private val _errorFlow = MutableStateFlow<Error?>(null)
    val errorFlow = _errorFlow

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
        val paymentDetails = PaymentDetails(
            recipient = _orderFlow.value.recipient,
            amount = _orderFlow.value.amount,
            purpose = _orderFlow.value.purpose,
            iban = _orderFlow.value.iban
        )

        if (paymentDetails.iban.isEmpty() || paymentDetails.amount.isEmpty() || paymentDetails.purpose.isEmpty() || paymentDetails.recipient.isEmpty()) {
            _errorFlow.value = Error.PaymentDetailsIncomplete
            return@launch
        }
        if (!isValidIban(paymentDetails.iban)) {
            _errorFlow.value = Error.InvalidIban
            return@launch
        }

        when (val paymentProvidersAppsState = giniHealth.giniInternalPaymentModule.paymentComponent.paymentProviderAppsFlow.value) {
            is PaymentProviderAppsState.Success -> {
                val paymentProviders = paymentProvidersAppsState.paymentProviderApps
                paymentProviders.first { it.paymentProvider.id == PAYMENT_PROVIDER_ID_FOR_PAYMENT_REQUEST }.runCatching {
                    giniHealth.giniInternalPaymentModule.getPaymentRequest(this, paymentDetails = paymentDetails).also {
                        val newOrder = _orderFlow.value.copy(expiryDate = it.expirationDate, requestId = it.id)
                        ordersRepository.convertToPaymentRequest(newOrder, it.id)
                        _orderFlow.value = newOrder.copy(id = it.id)
                    }
                }.onFailure {
                    _errorFlow.value = Error.ErrorMessage(it.message ?: "")
                }
            }
            else -> {
                _errorFlow.value = Error.GenericError
            }
        }
    }

    companion object {
        const val PAYMENT_PROVIDER_ID_FOR_PAYMENT_REQUEST = "b09ef70a-490f-11eb-952e-9bc6f4646c57"
    }

    sealed class Error {
        data object InvalidIban: Error()
        data object PaymentDetailsIncomplete: Error()
        data object GenericError: Error()
        data class ErrorMessage(val error: String): Error()
    }
}
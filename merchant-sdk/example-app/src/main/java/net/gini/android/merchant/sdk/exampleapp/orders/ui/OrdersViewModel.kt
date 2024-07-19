package net.gini.android.merchant.sdk.exampleapp.orders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.exampleapp.orders.data.OrdersRepository
import net.gini.android.merchant.sdk.exampleapp.orders.ui.model.OrderItem
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.merchant.sdk.integratedFlow.PaymentFragment
import org.slf4j.LoggerFactory

class OrdersViewModel(
    private val ordersRepository: OrdersRepository,
    val giniMerchant: GiniMerchant
) : ViewModel() {

    val ordersFlow = ordersRepository.ordersFlow.map { orders ->
        orders.map { order -> OrderItem.fromOrder(order) }
    }

    private val _selectedOrderItem: MutableStateFlow<OrderItem?> = MutableStateFlow(null)
    val selectedOrderItem: StateFlow<OrderItem?> = _selectedOrderItem

    private val _startIntegratedPaymentFlow = MutableSharedFlow<PaymentFragment>(
        extraBufferCapacity = 1
    )
    val startIntegratedPaymentFlow = _startIntegratedPaymentFlow

    private var paymentFlowConfiguration: PaymentFlowConfiguration? = null

    private var _finishPaymentFlow = MutableStateFlow<Boolean?>(null)
    val finishPaymentFlow: StateFlow<Boolean?> = _finishPaymentFlow

    fun startObservingPaymentFlow() = viewModelScope.launch {
        giniMerchant.eventsFlow.collect { event ->
            when (event) {
                is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated,
                is GiniMerchant.MerchantSDKEvents.OnFinishedWithCancellation -> {
                    _finishPaymentFlow.tryEmit(true)
                }
                else -> {}
            }
        }
    }

    fun loadPaymentProviderApps() {
        viewModelScope.launch {
            giniMerchant.loadPaymentProviderApps()
        }
    }

    fun setSelectedOrderItem(orderItem: OrderItem?) = viewModelScope.launch {
        _selectedOrderItem.emit(orderItem)
    }

    fun startPaymentFlow() {
        _startIntegratedPaymentFlow.tryEmit(giniMerchant.getFragment(
            recipient = _selectedOrderItem.value?.recipient ?: "",
            iban = _selectedOrderItem.value?.order?.iban ?: "",
            purpose = _selectedOrderItem.value?.purpose ?: "",
            amount = _selectedOrderItem.value?.amount ?: ""
            ,
            flowConfiguration = paymentFlowConfiguration))
    }

    fun setIntegratedFlowConfiguration(flowConfiguration: PaymentFlowConfiguration) {
        this.paymentFlowConfiguration = flowConfiguration
    }

    fun resetFinishPaymentFlow() {
        _finishPaymentFlow.tryEmit(null)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OrdersViewModel::class.java)

    }
}
package net.gini.android.health.sdk.exampleapp.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.exampleapp.orders.data.OrdersRepository
import net.gini.android.health.sdk.exampleapp.orders.data.model.Order
import net.gini.android.health.sdk.exampleapp.orders.data.model.OrderItem
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.integratedFlow.PaymentFragment
import net.gini.android.health.sdk.review.model.PaymentDetails
import org.slf4j.LoggerFactory

class OrdersViewModel(
    private val ordersRepository: OrdersRepository,
    val giniHealth: GiniHealth
) : ViewModel() {

    val ordersFlow = ordersRepository.ordersFlow.map { orders ->
        orders.map { order -> OrderItem.fromOrder(order) }
    }

    private val _selectedOrderItem: MutableStateFlow<OrderItem?> = MutableStateFlow(null)
    val selectedOrderItem: StateFlow<OrderItem?> = _selectedOrderItem

    private val _errorsFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorsFlow: SharedFlow<String> = _errorsFlow

    private var paymentFlowConfiguration: PaymentFlowConfiguration? = null

    private val _startIntegratedPaymentFlow = MutableSharedFlow<PaymentDetails>(
        extraBufferCapacity = 1
    )
    val startIntegratedPaymentFlow = _startIntegratedPaymentFlow

    private var _finishPaymentFlow = MutableStateFlow<Boolean?>(null)
    val finishPaymentFlow: StateFlow<Boolean?> = _finishPaymentFlow

    val openBankState = giniHealth.openBankState
    val displayedScreen = giniHealth.displayedScreen

    fun startObservingPaymentFlow() = viewModelScope.launch {
        giniHealth.openBankState.collect { event ->
            when (event) {
                is GiniHealth.PaymentState.Success,
                is GiniHealth.PaymentState.Cancel -> {
                    _finishPaymentFlow.tryEmit(true)
                }
                else -> {}
            }
        }
    }

    fun setSelectedOrderItem(orderItem: OrderItem?) = viewModelScope.launch {
        _selectedOrderItem.emit(orderItem)
    }

    fun startPaymentFlowWithoutDocument(paymentDetails: PaymentDetails) {
        _startIntegratedPaymentFlow.tryEmit(paymentDetails)
    }

    fun getPaymentFragmentForPaymentDetails(paymentDetails: PaymentDetails, paymentFlowConfiguration: PaymentFlowConfiguration?): Result<PaymentFragment> {
        try {
            val paymentFragment = giniHealth.getPaymentFragmentWithoutDocument(paymentDetails, PaymentFlowConfiguration(shouldShowReviewBottomDialog = paymentFlowConfiguration?.shouldShowReviewBottomDialog ?: false, shouldHandleErrorsInternally = true))
            return Result.success(paymentFragment)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun startPaymentFlow(order: Order) {
//        try {
//            _startIntegratedPaymentFlow.tryEmit(giniHealth.getPaymentFragmentWithoutDocument(
//                paymentDetails = PaymentDetails(
//                    recipient = order.recipient,
//                    iban = order.iban,
//                    amount = order.amount,
//                    purpose = order.purpose,
//                    extractions = null
//                ),
//                configuration = paymentFlowConfiguration)
//            )
//        } catch (e: IllegalStateException) {
//            LOG.error(e.message)
//            _errorsFlow.tryEmit(e.message ?: "")
//        }
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
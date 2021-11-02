package net.gini.pay.appscreenapi.pay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.ResolvePaymentInput
import net.gini.android.core.api.models.ResolvedPayment
import net.gini.pay.appscreenapi.util.ResultWrapper
import net.gini.pay.appscreenapi.util.wrapToResult
import net.gini.android.bank.sdk.GiniBank

class PayViewModel(
    private val giniBank: GiniBank
) : ViewModel() {

    private val _paymentRequest = MutableStateFlow<ResultWrapper<PaymentRequest>>(ResultWrapper.Loading())
    val paymentRequest: StateFlow<ResultWrapper<PaymentRequest>> = _paymentRequest

    private val _paymentState = MutableStateFlow<ResultWrapper<ResolvedPayment>>(ResultWrapper.Loading())
    val paymentState: StateFlow<ResultWrapper<ResolvedPayment>> = _paymentState

    private var requestId: String? = null

    fun fetchPaymentRequest(requestId: String) {
        this.requestId = requestId
        _paymentRequest.value = ResultWrapper.Loading()
        viewModelScope.launch {
            _paymentRequest.value = wrapToResult { giniBank.getPaymentRequest(requestId) }
        }
    }

    fun onPay(paymentDetails: ResolvePaymentInput) {
        _paymentState.value = ResultWrapper.Loading()
        requestId?.let { id ->
            viewModelScope.launch {
                _paymentState.value = wrapToResult { giniBank.resolvePaymentRequest(id, paymentDetails) }
            }
        }
    }

    fun returnToPaymentInitiatorApp(context: Context) {
        val payment = paymentState.value
        if (payment is ResultWrapper.Success)
        giniBank.returnToPaymentInitiatorApp(context, payment.value)
    }
}
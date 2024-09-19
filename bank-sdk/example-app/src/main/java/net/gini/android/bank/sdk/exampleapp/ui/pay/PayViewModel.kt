package net.gini.android.bank.sdk.exampleapp.ui.pay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.core.DefaultNetworkServicesProvider
import net.gini.android.bank.sdk.exampleapp.core.ResultWrapper
import net.gini.android.bank.sdk.exampleapp.core.wrapToResult
import net.gini.android.core.api.models.PaymentRequest
import javax.inject.Inject

@HiltViewModel
class PayViewModel @Inject constructor(
    defaultNetworkServicesProvider: DefaultNetworkServicesProvider,
) : ViewModel(), PayViewModelInterface {

    private val _paymentRequest = MutableStateFlow<ResultWrapper<PaymentRequest>>(ResultWrapper.Loading())
    override val paymentRequest: StateFlow<ResultWrapper<PaymentRequest>> = _paymentRequest

    private val _paymentState = MutableStateFlow<ResultWrapper<ResolvedPayment>>(ResultWrapper.Loading())
    override val paymentState: StateFlow<ResultWrapper<ResolvedPayment>> = _paymentState

    private var requestId: String? = null

    init {
        GiniBank.setGiniApi(defaultNetworkServicesProvider.giniBankAPI)
    }

    override fun fetchPaymentRequest(requestId: String) {
        this.requestId = requestId
        _paymentRequest.value = ResultWrapper.Loading()
        viewModelScope.launch {
            _paymentRequest.value = wrapToResult { GiniBank.getPaymentRequest(requestId) }
        }
    }

    override fun onPay(paymentDetails: ResolvePaymentInput) {
        _paymentState.value = ResultWrapper.Loading()
        requestId?.let { id ->
            viewModelScope.launch {
                _paymentState.value = wrapToResult { GiniBank.resolvePaymentRequest(id, paymentDetails) }
            }
        }
    }

    override fun returnToPaymentInitiatorApp(context: Context) {
        val payment = _paymentState.value
        if (payment is ResultWrapper.Success)
            GiniBank.returnToPaymentInitiatorApp(context, payment.value)
    }
}
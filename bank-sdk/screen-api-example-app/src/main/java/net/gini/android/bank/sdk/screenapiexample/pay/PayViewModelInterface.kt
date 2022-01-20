package net.gini.android.bank.sdk.screenapiexample.pay

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.bank.sdk.screenapiexample.util.ResultWrapper
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.ResolvePaymentInput
import net.gini.android.core.api.models.ResolvedPayment

/**
 * Created by Alpár Szotyori on 19.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
interface PayViewModelInterface {

    val paymentRequest: StateFlow<ResultWrapper<PaymentRequest>>
    val paymentState: StateFlow<ResultWrapper<ResolvedPayment>>

    fun fetchPaymentRequest(requestId: String)
    fun onPay(paymentDetails: ResolvePaymentInput)
    fun returnToPaymentInitiatorApp(context: Context)
}

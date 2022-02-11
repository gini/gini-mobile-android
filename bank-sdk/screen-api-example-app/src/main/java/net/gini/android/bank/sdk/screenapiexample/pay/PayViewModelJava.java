package net.gini.android.bank.sdk.screenapiexample.pay;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import net.gini.android.bank.sdk.GiniBank;
import net.gini.android.bank.sdk.screenapiexample.util.ResultWrapper;
import net.gini.android.bank.sdk.util.CoroutineContinuationHelper;
import net.gini.android.core.api.models.PaymentRequest;
import net.gini.android.bank.api.models.ResolvePaymentInput;
import net.gini.android.bank.api.models.ResolvedPayment;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/**
 * Created by Alpár Szotyori on 19.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Example showing how to use the GiniBank suspending functions from Java.
 */
public class PayViewModelJava extends ViewModel implements PayViewModelInterface {

    private final GiniBank giniBank;

    private final MutableStateFlow<ResultWrapper<PaymentRequest>> _paymentRequest = StateFlowKt.MutableStateFlow(new ResultWrapper.Loading<>());
    private final MutableStateFlow<ResultWrapper<ResolvedPayment>> _paymentState = StateFlowKt.MutableStateFlow(new ResultWrapper.Loading<>());

    private String requestId = null;

    public PayViewModelJava(@NonNull final GiniBank giniBank) {
        this.giniBank = giniBank;
    }

    @NonNull
    @Override
    public StateFlow<ResultWrapper<PaymentRequest>> getPaymentRequest() {
        return _paymentRequest;
    }

    @NonNull
    @Override
    public StateFlow<ResultWrapper<ResolvedPayment>> getPaymentState() {
        return _paymentState;
    }

    public void fetchPaymentRequest(@NonNull final String requestId) {
        this.requestId = requestId;
        giniBank.getPaymentRequest(requestId, CoroutineContinuationHelper.callbackContinuation(new CoroutineContinuationHelper.ContinuationCallback<PaymentRequest>() {
            @Override
            public void onFinished(PaymentRequest result) {
                _paymentRequest.setValue(new ResultWrapper.Success<>(result));
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                _paymentRequest.setValue(new ResultWrapper.Error<>(error));
            }

            @Override
            public void onCancelled() {

            }
        }));
    }

    public void onPay(@NonNull final ResolvePaymentInput paymentDetails) {
        if (requestId != null) {
            giniBank.resolvePaymentRequest(requestId, paymentDetails, CoroutineContinuationHelper.callbackContinuation(new CoroutineContinuationHelper.ContinuationCallback<ResolvedPayment>() {
                @Override
                public void onFinished(ResolvedPayment result) {
                    _paymentState.setValue(new ResultWrapper.Success<>(result));
                }

                @Override
                public void onFailed(@NonNull Throwable error) {
                    _paymentState.setValue(new ResultWrapper.Error<>(error));
                }

                @Override
                public void onCancelled() {

                }
            }));
        }
    }

    public void returnToPaymentInitiatorApp(@NonNull final Context context) {
        if (_paymentState.getValue() instanceof ResultWrapper.Success) {
            final ResultWrapper.Success<ResolvedPayment> paymentStateValue = (ResultWrapper.Success<ResolvedPayment>) _paymentState.getValue();
            final ResolvedPayment resolvedPayment = paymentStateValue.getValue();
            giniBank.returnToPaymentInitiatorApp(context, resolvedPayment);
        }
    }
}

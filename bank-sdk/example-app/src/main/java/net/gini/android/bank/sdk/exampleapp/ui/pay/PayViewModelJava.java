package net.gini.android.bank.sdk.exampleapp.ui.pay;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import net.gini.android.bank.api.models.ResolvePaymentInput;
import net.gini.android.bank.api.models.ResolvedPayment;
import net.gini.android.bank.sdk.GiniBank;
import net.gini.android.bank.sdk.exampleapp.core.ResultWrapper;
import net.gini.android.bank.sdk.util.CoroutineContinuationHelper;
import net.gini.android.core.api.models.PaymentRequest;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/**
 * Example showing how to use the GiniBank suspending functions from Java.
 *
 * Created by Alpár Szotyori on 19.01.22.
 * Copyright (c) 2022 Gini GmbH.
 */
public class PayViewModelJava extends ViewModel implements PayViewModelInterface {

    private final GiniBank giniBank;

    private final MutableStateFlow<ResultWrapper<PaymentRequest>> paymentRequestFlow = StateFlowKt.MutableStateFlow(new ResultWrapper.Loading<>());
    private final MutableStateFlow<ResultWrapper<ResolvedPayment>> paymentStateFlow = StateFlowKt.MutableStateFlow(new ResultWrapper.Loading<>());

    private String requestId = null;

    public PayViewModelJava(@NonNull final GiniBank giniBank) {
        this.giniBank = giniBank;
    }

    @NonNull
    @Override
    public StateFlow<ResultWrapper<PaymentRequest>> getPaymentRequest() {
        return paymentRequestFlow;
    }

    @NonNull
    @Override
    public StateFlow<ResultWrapper<ResolvedPayment>> getPaymentState() {
        return paymentStateFlow;
    }

    public void fetchPaymentRequest(@NonNull final String requestId) {
        this.requestId = requestId;
        giniBank.getPaymentRequest(requestId, CoroutineContinuationHelper.callbackContinuation(new CoroutineContinuationHelper.ContinuationCallback<PaymentRequest>() {
            @Override
            public void onFinished(PaymentRequest result) {
                paymentRequestFlow.setValue(new ResultWrapper.Success<>(result));
            }

            @Override
            public void onFailed(@NonNull Throwable error) {
                paymentRequestFlow.setValue(new ResultWrapper.Error<>(error));
            }

            @Override
            public void onCancelled() {
                // no-op: cancellation is not handled
            }
        }));
    }

    public void onPay(@NonNull final ResolvePaymentInput paymentDetails) {
        if (requestId != null) {
            try {
                giniBank.resolvePaymentRequest(requestId, paymentDetails, CoroutineContinuationHelper.callbackContinuation(new CoroutineContinuationHelper.ContinuationCallback<ResolvedPayment>() {
                    @Override
                    public void onFinished(ResolvedPayment result) {
                        paymentStateFlow.setValue(new ResultWrapper.Success<>(result));
                    }

                    @Override
                    public void onFailed(@NonNull Throwable error) {
                        paymentStateFlow.setValue(new ResultWrapper.Error<>(error));
                    }

                    @Override
                    public void onCancelled() {
                        // no-op: cancellation is not handled
                    }
                }));
            } catch (final Exception exception) {
                paymentStateFlow.setValue(new ResultWrapper.Error<>(exception));
            }
        }
    }

    public void returnToPaymentInitiatorApp(@NonNull final Context context) {
        if (paymentStateFlow.getValue() instanceof ResultWrapper.Success) {
            final ResultWrapper.Success<ResolvedPayment> paymentStateValue = (ResultWrapper.Success<ResolvedPayment>) paymentStateFlow.getValue();
            final ResolvedPayment resolvedPayment = paymentStateValue.getValue();
            giniBank.returnToPaymentInitiatorApp(context, resolvedPayment);
        }
    }
}

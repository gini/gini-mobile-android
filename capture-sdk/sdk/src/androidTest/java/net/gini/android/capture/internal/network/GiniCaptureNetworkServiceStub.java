package net.gini.android.capture.internal.network;

import net.gini.android.capture.Document;
import net.gini.android.capture.network.AnalysisResult;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.GiniCaptureNetworkCallback;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.Result;
import net.gini.android.capture.network.model.GiniCaptureBox;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.util.CancellationToken;

import java.util.Collections;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 16.04.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class GiniCaptureNetworkServiceStub implements GiniCaptureNetworkService {

    public static final String DEFAULT_DOCUMENT_ID = "ABCD-EFGH";

    @Override
    public CancellationToken upload(@NonNull final Document document,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
        callback.success(new Result(DEFAULT_DOCUMENT_ID));
        return new CallbackCancellationToken(callback);
    }

    @Override
    public CancellationToken delete(@NonNull final String giniApiDocumentId,
            @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
        callback.success(new Result(DEFAULT_DOCUMENT_ID));
        return new CallbackCancellationToken(callback);
    }

    @Override
    public CancellationToken analyze(@NonNull final LinkedHashMap<String, Integer> giniApiDocumentIdRotationMap,
            @NonNull final GiniCaptureNetworkCallback<AnalysisResult, Error> callback) {
        callback.success(createAnalysisResult());
        return new CallbackCancellationToken(callback);
    }

    @Override
    public void cleanup() {

    }

    @NonNull
    protected AnalysisResult createAnalysisResult() {
        return new AnalysisResult(DEFAULT_DOCUMENT_ID,
                Collections.singletonMap("amountToPay",
                        new GiniCaptureSpecificExtraction("amountToPay",
                                "1:00EUR", "amountToPay",
                                new GiniCaptureBox(1, 0,0,0,0),
                                Collections.<GiniCaptureExtraction>emptyList())),
                Collections.<String, GiniCaptureCompoundExtraction>emptyMap());
    }

    public static class CallbackCancellationToken implements CancellationToken {

        private final GiniCaptureNetworkCallback mCallback;

        public CallbackCancellationToken(
                final GiniCaptureNetworkCallback callback) {
            mCallback = callback;
        }

        @Override
        public void cancel() {
            mCallback.cancelled();
        }
    }
}

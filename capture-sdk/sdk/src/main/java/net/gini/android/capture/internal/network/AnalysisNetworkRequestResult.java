package net.gini.android.capture.internal.network;

import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.network.AnalysisResult;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 16.04.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class AnalysisNetworkRequestResult<T extends GiniCaptureDocument>
        extends NetworkRequestResult<T> {

    private final AnalysisResult mAnalysisResult;

    public AnalysisNetworkRequestResult(@NonNull final T giniCaptureDocument,
            @NonNull final String apiDocumentId,
            @NonNull final AnalysisResult analysisResult) {
        super(giniCaptureDocument, apiDocumentId);
        mAnalysisResult = analysisResult;
    }

    @NonNull
    public AnalysisResult getAnalysisResult() {
        return mAnalysisResult;
    }
}

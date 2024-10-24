package net.gini.android.capture.internal.network;

import net.gini.android.capture.document.GiniCaptureDocument;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 16.04.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class NetworkRequestResult<T extends GiniCaptureDocument> {

    private final T mGiniCaptureDocument;
    private final String mApiDocumentId;
    private final String mApiDocumentFilename;

    public NetworkRequestResult(@NonNull final T giniCaptureDocument,
                                @NonNull final String apiDocumentId,
                                @NonNull final String apiDocumentFilename) {
        mGiniCaptureDocument = giniCaptureDocument;
        mApiDocumentId = apiDocumentId;
        mApiDocumentFilename = apiDocumentFilename;
    }

    @NonNull
    public T getGiniCaptureDocument() {
        return mGiniCaptureDocument;
    }

    @NonNull
    public String getApiDocumentId() {
        return mApiDocumentId;
    }

    @NonNull
    public String getApiDocumentFilename() {
        return mApiDocumentFilename;
    }
}

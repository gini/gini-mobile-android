package net.gini.android.core.api;

import static net.gini.android.core.api.MediaTypes.*;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 14.01.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * The current supported APIs.
 */
public enum GiniApiType {
    HEALTH("https://pay-api.gini.net/",
            GINI_JSON_V1,
            GINI_PARTIAL_V1,
            GINI_DOCUMENT_JSON_V1),
    BANK("https://pay-api.gini.net/",
            GINI_JSON_V1,
            GINI_PARTIAL_V1,
            GINI_DOCUMENT_JSON_V1),
    ACCOUNTING("https://accounting-api.gini.net/",
            GINI_JSON_V1,
            "",""),
    /**
     * @deprecated Use industry specific API types instead.
     */
    DEFAULT("https://pay-api.gini.net/",
            GINI_JSON_V1,
            GINI_PARTIAL_V1,
            GINI_DOCUMENT_JSON_V1);

    private final String mBaseUrl;
    private final String mGiniJsonMediaType;
    private final String mGiniPartialMediaType;
    private final String mGiniCompositeJsonMediaType;

    GiniApiType(@NonNull final String baseUrl,
            @NonNull final String giniJsonMediaType,
            @NonNull final String giniPartialMediaType,
            @NonNull final String giniCompositeJsonMediaType) {
        mBaseUrl = baseUrl;
        mGiniJsonMediaType = giniJsonMediaType;
        mGiniPartialMediaType = giniPartialMediaType;
        mGiniCompositeJsonMediaType = giniCompositeJsonMediaType;
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public String getGiniJsonMediaType() {
        return mGiniJsonMediaType;
    }

    public String getGiniPartialMediaType() {
        return mGiniPartialMediaType;
    }

    public String getGiniCompositeJsonMediaType() {
        return mGiniCompositeJsonMediaType;
    }
}
